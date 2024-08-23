package com.fact_checker.FactChecker.service;
import com.fact_checker.FactChecker.model.Video;
import com.fact_checker.FactChecker.repository.VideoRepository;
import io.reactivex.rxjava3.core.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.json.*;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TextAnalysisService
 *
 * This service is responsible for analyzing text content from videos, evaluating factual claims,
 * and providing a factual accuracy score. It utilizes the Groq API for natural language processing
 * and fact-checking capabilities.
 *
 * Key Features:
 * - Analyzes transcription text from videos
 * - Generates distinct factual claims from the text
 * - Evaluates the factual accuracy of each claim
 * - Calculates an overall fact percentage for the video
 * - Identifies false statements within the video content
 *
 * Dependencies:
 * - IGroqApiClient: Interface for interacting with the Groq API
 * - VideoRepository: Repository for persisting Video entities
 * - Groq API key: Configured via application properties
 *
 * Usage:
 * Autowire this service in your Spring components to perform text analysis on video content.
 */
@Service
public class TextAnalysisService {

    private final IGroqApiClient apiClient;
    private final String apiKey;
    private static final Logger logger = LoggerFactory.getLogger(TextAnalysisService.class);
    private static final double LOW_PERCENTAGE_THRESHOLD = 0.5;
    private final VideoRepository videoRepository;

    /**
     * Constructor for TextAnalysisService.
     *
     * @param apiClient The Groq API client
     * @param apiKey The API key for Groq, injected from application properties
     * @param videoRepository The repository for Video entities
     */
    public TextAnalysisService(IGroqApiClient apiClient, @Value("${groq.api.key}") String apiKey, VideoRepository videoRepository) {
        this.apiClient = apiClient;
        this.apiKey = apiKey;
        this.videoRepository = videoRepository;
    }

    /**
     * Analyzes the transcription text of a video for factual accuracy.
     *
     * This method performs the following steps:
     * 1. Extracts claims from the transcription text
     * 2. Rates each claim for factual accuracy
     * 3. Calculates an average fact score
     * 4. Identifies false statements
     * 5. Updates the video entity with the results
     * 6. Persists the updated video entity
     *
     * @param video The Video entity to analyze
     * @return The average fact score as a percentage (0-100)
     */
    public double analyzeText(Video video) {
        String transcriptionText = video.getTranscriptionText();
        List<String> claims = generateClaimsSeparatedByAsterisks(transcriptionText);
        Map<String, Double> scoredClaims = rateClaimsByFacts(claims);

        if (scoredClaims == null) {
            return 0;
        }

        double averageScore = getAverageScore(scoredClaims);

        video.setFactPercentage(averageScore);

        List<String> falseClaims = getFalseClaims(scoredClaims);

        video.setFalseStatements(falseClaims);

        videoRepository.save(video);

        return averageScore;
    }

    /**
     * Identifies claims that are considered false based on their fact score.
     *
     * @param scoredClaims A map of claims and their corresponding fact scores
     * @return A list of claims that fall below the LOW_PERCENTAGE_THRESHOLD
     */
    List<String> getFalseClaims(Map<String, Double> scoredClaims) {
        return scoredClaims.entrySet()
                .stream()
                .filter(entry -> entry.getValue() < LOW_PERCENTAGE_THRESHOLD * 100)
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Calculates the average score from a map of scored claims.
     *
     * @param scoredClaims A map of claims and their corresponding fact scores
     * @return The average score as a percentage (0-100)
     */
    double getAverageScore(Map<String, Double> scoredClaims) {
        return scoredClaims.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000; // 1 second

    /**
     * Rates the factual accuracy of a list of claims using the Groq API.
     *
     * This method includes retry logic to handle potential API failures.
     *
     * @param claims A list of factual claims to be evaluated
     * @return A map of claims and their corresponding fact scores, or null if all attempts fail
     */
    private Map<String, Double> rateClaimsByFacts(List<String> claims) {
        int retries = 0;
        while (retries < MAX_RETRIES) {
            try {
                System.out.println("api key" + apiKey);
                JsonObject request = Json.createObjectBuilder()
                        .add("model", "Llama-3.1-8b-Instant")
                        .add("messages", Json.createArrayBuilder()
                                .add(Json.createObjectBuilder()
                                        .add("role", "user")
                                        .add("content", """
                                            Analyze the following text and evaluate the factual accuracy of each statement, assigning a score between 1 and 100. Return the results in a JSON object where each statement is represented as a key (e.g., "statement1", "statement2", etc.), and its corresponding factual score as the value.
                                            The output should strictly follow this format:
                                            {
                                              "<statement>": 11.01,
                                              "<statement>": 98,
                                              "<statement>": 54.4
                                            }
                                            Example Response:
                                            {
                                                "The moon is made of cheese": 80.05,
                                                "The cheese is made of flour": 100,
                                                "The flour is made of water": 100,
                                            }
                                            Important notes:
                                            - Each key should be labeled as its own statement.
                                            - The value for each key should be a float or integer representing the factual score.
                                            - No additional text or explanations should be included in the output; only the JSON object.
                                            Text to analyse:                                      
                                            """
                                        ))
                                .add(Json.createObjectBuilder()
                                        .add("role", "user")
                                        .add("content", claims.toString())))
                        .build();

                JsonObject result = fetchSingleResponse(request);
                if (result == null) {
                    throw new RuntimeException("Null response from Groq API");
                }

                JsonArray choices = result.getJsonArray("choices");
                if (choices == null || choices.isEmpty()) {
                    throw new RuntimeException("No choices in Groq API response");
                }

                JsonObject firstChoice = choices.getJsonObject(0);
                JsonObject message = firstChoice.getJsonObject("message");
                String content = message.getString("content");

                logger.info("Response from Groq API: {}", content);

                Map<String, Double> scoredClaims = convertJsonToMap(content);
                if (scoredClaims == null || scoredClaims.isEmpty()) {
                    throw new RuntimeException("Failed to parse claims from API response");
                }

                return scoredClaims;

            } catch (Exception e) {
                logger.error("Error while fetching response from Groq API (Attempt {} of {})", retries + 1, MAX_RETRIES, e);
                retries++;
                if (retries < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.error("Retry delay interrupted", ie);
                    }
                }
            }
        }

        logger.error("Failed to fetch response from Groq API after {} attempts", MAX_RETRIES);
        return null;
    }

    /**
     * Converts a JSON string to a Map<String, Double>.
     *
     * @param content The JSON string to convert
     * @return A map representation of the JSON content, or null if conversion fails
     */
    Map<String, Double> convertJsonToMap(String content) {
        try (JsonReader jsonReader = Json.createReader(new StringReader(content))) {
            JsonObject jsonObject = jsonReader.readObject();
            Map<String, Double> resultMap = new HashMap<>();
            for (Map.Entry<String, JsonValue> entry : jsonObject.entrySet()) {
                resultMap.put(entry.getKey(), ((JsonNumber) entry.getValue()).doubleValue());
            }
            return resultMap;
        } catch (Exception e) {
            logger.error("Error while converting json to map", e);
            return null;
        }
    }

    /**
     * Generates a list of distinct factual claims from the given text.
     *
     * This method uses the Groq API to divide the text into separate claims.
     * This includes retry logic in case the API is not available.
     *
     * @param text The input text to analyze
     * @return A list of distinct factual claims, or an empty list if generation fails
     */

    private List<String> generateClaimsSeparatedByAsterisks(String text) {
        int retries = 0;
        while (retries < MAX_RETRIES) {
            try {
                JsonObject request = Json.createObjectBuilder()
                        .add("model", "Llama-3.1-8b-Instant")
                        .add("messages", Json.createArrayBuilder()
                                .add(Json.createObjectBuilder()
                                        .add("role", "user")
                                        .add("content", """
                Divide the following text into distinct factual claims.
                Guidelines:
                1. Each claim should be a single, standalone fact.
                2. Separate each claim with an asterisk (*).
                3. Preserve the original wording as much as possible.
                4. Do not add any additional commentary or explanations.
                5. Ensure all information from the original text is included in the claims.

                Examples:
                Input: "The Eiffel Tower, built in 1889, is 324 meters tall and located in Paris, France."
                Output: The Eiffel Tower was built in 1889.*The Eiffel Tower is 324 meters tall.*The Eiffel Tower is located in Paris, France.

                Input: "COVID-19, first identified in Wuhan, China in December 2019, quickly spread globally, causing a pandemic."
                Output: COVID-19 was first identified in Wuhan, China.*COVID-19 was first identified in December 2019.*COVID-19 spread globally.*COVID-19 caused a pandemic.

                Additional notes:
                - If a sentence contains multiple facts, split it into separate claims.
                - Maintain the original context of the claims.
                - Avoid redundancy in the claims.
                - For complex sentences, it's acceptable to simplify while preserving the core facts.

                Text to analyze:
                %s
                """
                                                .formatted(text))))
                        .build();

                JsonObject result = fetchSingleResponse(request);
                String content = result.getJsonArray("choices").getJsonObject(0).getJsonObject("message").getString("content");
                logger.info("Claims: {}", content);
                return Arrays.asList(content.split("\\*"));
            } catch (Exception e) {
                logger.error("Error while generating claims (Attempt {} of {}): {}", retries + 1, MAX_RETRIES, e.getMessage());
                retries++;
                if (retries < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.error("Retry delay interrupted", ie);
                    }
                }
            }
        }
        logger.error("Failed to generate claims after {} attempts", MAX_RETRIES);
        return Collections.emptyList();
    }



    /**
     * Fetches a single response from the Groq API.
     *
     * This method uses RxJava to handle the asynchronous API call.
     *
     * @param request The JSON request object to send to the API
     * @return The JSON response from the API, or null if the call fails
     */
    private JsonObject fetchSingleResponse(JsonObject request) {
        final JsonObject[] result = new JsonObject[1];
        CountDownLatch latch = new CountDownLatch(1);

        Single<JsonObject> responseSingle = this.apiClient.createChatCompletionAsync(request);
        responseSingle.subscribe(
                response -> {
                    result[0] = response;
                    latch.countDown();
                },
                throwable -> {
                    System.err.println("Error: " + throwable.getMessage());
                    latch.countDown();
                }
        );

        try {
            latch.await();
        } catch (InterruptedException e) {
            System.err.println("Interrupted while waiting for response: " + e.getMessage());
        }

        return result[0];
    }
}
