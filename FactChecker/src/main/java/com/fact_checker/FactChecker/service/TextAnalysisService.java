package com.fact_checker.FactChecker.service;
import com.fact_checker.FactChecker.model.Video;
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

@Service
public class TextAnalysisService {

    private final IGroqApiClient apiClient;
    private final String apiKey;
    private static final Logger logger = LoggerFactory.getLogger(TextAnalysisService.class);
    private static final double LOW_PERCENTAGE_THRESHOLD = 0.5;

    // Inject IGroqApiClient and API key via constructor
    public TextAnalysisService(IGroqApiClient apiClient, @Value("${groq.api.key}") String apiKey ) {
        this.apiClient = apiClient;
        this.apiKey = apiKey;
    }
    
    public double analyzeText(Video video) {
        String transcriptionText = video.getTranscriptionText();
        List<String> claims = generateClaimsSeparatedByAsterisks(transcriptionText);
        Map<String, Double> scoredClaims = rateClaimsByFacts(claims);

        if (scoredClaims == null) {
            video.setFactPercentage(0);
            video.setFalseStatements(new ArrayList<>());
            return 0;
        }

        double averageScore = getAverageScore(scoredClaims);

        video.setFactPercentage(averageScore);

        List<String> falseClaims = getFalseClaims(scoredClaims);

        video.setFalseStatements(falseClaims);

        return averageScore;

    }

    List<String> getFalseClaims(Map<String, Double> scoredClaims) {

        return scoredClaims.entrySet()
                .stream()
                .filter(entry -> entry.getValue() < LOW_PERCENTAGE_THRESHOLD)
                .map(Map.Entry::getKey)
                .toList();

    }


    double getAverageScore(Map<String, Double> scoredClaims){

        return scoredClaims.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

    }


    private Map<String, Double> rateClaimsByFacts(List<String> claims) {

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
                                            - Each key should be labeled as "statement" followed by its sequence number (e.g., "statement1").
                                            - The value for each key should be a float or integer representing the factual score.
                                            - No additional text or explanations should be included in the output; only the JSON object.
                                            Text to analyse:                                      
                                            """
                                    )))
                    .build();

            JsonObject result = fetchSingleResponse(request);
            JsonArray choices = result.getJsonArray("choices");
            JsonObject firstChoice = choices.getJsonObject(0);
            JsonObject message = firstChoice.getJsonObject("message");
            String content = message.getString("content");

            return convertJsonToMap(content);

        } catch (Exception e) {
            logger.error("Error while fetching response from Groq API", e);
            return null;
        }

    }

    Map<String, Double> convertJsonToMap(String content){
        try (JsonReader jsonReader = Json.createReader(new StringReader(content))) {
            JsonObject jsonObject = jsonReader.readObject();
            Map<String, Double> resultMap = new HashMap<>();
            for (Map.Entry<String, JsonValue> entry : jsonObject.entrySet()) {
                resultMap.put(entry.getKey(), ((JsonNumber) entry.getValue()).doubleValue());
            }
            return resultMap;
        }
    }
    private List<String> generateClaimsSeparatedByAsterisks(String text) {
        try {
            JsonObject request = Json.createObjectBuilder()
                    .add("model", "Llama-3.1-8b-Instant")
                    .add("messages", Json.createArrayBuilder()
                            .add(Json.createObjectBuilder()
                                    .add("role", "user")
                                    .add("content", "Please divide the following text into distinct factual claims, with each claim separated by an asterisk (*). The text is: " + text)))
                    .build();

            JsonObject result = fetchSingleResponse(request);
            String content = result.getJsonArray("choices").getJsonObject(0).getJsonObject("message").getString("content");
            return Arrays.asList(content.split("\\*"));
        } catch (Exception e) {
            logger.error("Error while generating claims: {} ", e.getMessage());
            return Collections.emptyList();
        }
    }

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
