package com.fact_checker.FactChecker.service;
import com.fact_checker.FactChecker.model.Video;
import io.reactivex.rxjava3.core.Single;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TextAnalysisService {

    private final IGroqApiClient apiClient;
    private final String apiKey;

    // Inject IGroqApiClient and API key via constructor
    public TextAnalysisService(IGroqApiClient apiClient, @Value("${groq.api.key}") String apiKey ) {
        this.apiClient = apiClient;
        this.apiKey = apiKey;
    }
    
    public double analyzeText(Video video) {
        StringBuilder sb = new StringBuilder(video.getTranscriptionText());
        System.out.println("sb: " + sb);
        StringBuilder statements = generateClaimsSeparatedByAsterisks(sb);
        System.out.println("statements: " + statements);
        StringBuilder statScore = rateClaimsByFacts(statements);
        System.out.println("statScore: " + statScore);
        HashMap<String, Double> scoresMap = extractScores(statScore.toString());
        System.out.println("scoresMap: " + scoresMap);

        return scoresMap.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    public int sumList(List<Double> list) {
        int sum = 0;
        for (Double value : list) {
            sum += value;
        }
        return sum;
    }
    public HashMap<String, Double> extractScores(String input) {
        HashMap<String, Double> scoresMap = new HashMap<>();
        Pattern pattern = Pattern.compile("\"(.*?)\":\\s*(\\d+(?:\\.\\d+)?)");
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            String statement = matcher.group(1);
            Double score = Double.parseDouble(matcher.group(2));
            scoresMap.put(statement, score);
        }

        return scoresMap;
    }


    public StringBuilder rateClaimsByFacts(StringBuilder sb) {
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
                                          "statement1": 11.01,
                                          "statement2": 98,
                                          "statement3": 54.4
                                        }
                                        Important notes:
                                        - Each key should be labeled by the actual statement followed by its sequence number (e.g., "the sky is blue 1").
                                        - The value for each key should be a float or integer representing the factual score.(ex" 11.01)
                                        - No additional text or explanations should be included in the output; only the JSON object.
                                        Text to analyse:                                      
                                        """ + sb.toString()
                                )))
                .build();

        JsonObject result = fetchSingleResponse(request);
        JsonArray choices = result.getJsonArray("choices");
        JsonObject firstChoice = choices.getJsonObject(0);
        JsonObject message = firstChoice.getJsonObject("message");
        return new StringBuilder(message.getString("content"));

    }
    public StringBuilder generateClaimsSeparatedByAsterisks(StringBuilder sb) {
        JsonObject request = Json.createObjectBuilder()
                .add("model", "Llama-3.1-8b-Instant")
                .add("messages", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("role", "user")
                                .add("content", "Please divide the following text into distinct factual claims, with each claim separated by an asterisk (*). The text is:"
                                        + sb.toString()
                                        + " Make sure that each claim is clearly separated and represents a distinct idea or statement from the text.")))
                .build();

        JsonObject result = fetchSingleResponse(request);
        JsonArray choices = result.getJsonArray("choices");
        JsonObject firstChoice = choices.getJsonObject(0);
        JsonObject message = firstChoice.getJsonObject("message");
        return new StringBuilder(message.getString("content"));
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
