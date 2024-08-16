package com.fact_checker.FactChecker.service;

import io.reactivex.rxjava3.core.Single;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Service
public class TextAnalysisService {

    private final IGroqApiClient apiClient;

    @Value("${groq.api.key}")
    private String apiKey;

    public TextAnalysisService(IGroqApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public int analyzeText(StringBuilder text) {
        StringBuilder statements = generateClaimsSeparatedByAsterisks(apiClient, text);
        StringBuilder statScore = rateClaimsByFacts(apiClient, statements);
        List<Double> scores = extractScores(statScore.toString());
        return sumList(scores) / scores.size();
    }

    public StringBuilder generateClaimsSeparatedByAsterisks(IGroqApiClient apiClient, StringBuilder sb) {
        JsonObject request = Json.createObjectBuilder()
                .add("model", "Llama-3.1-8b-Instant")
                .add("messages", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("role", "user")
                                .add("content", "Please divide the following text into distinct factual claims, with each claim separated by an asterisk (*). The text is:"
                                        + sb.toString()
                                        + " Make sure that each claim is clearly separated and represents a distinct idea or statement from the text.")))
                .build();

        JsonObject result = fetchSingleResponse(apiClient, request);
        JsonArray choices = result.getJsonArray("choices");
        JsonObject firstChoice = choices.getJsonObject(0);
        JsonObject message = firstChoice.getJsonObject("message");
        return new StringBuilder(message.getString("content"));
    }

    public StringBuilder rateClaimsByFacts(IGroqApiClient apiClient, StringBuilder sb) {
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
                                        - Each key should be labeled as "statement" followed by its sequence number (e.g., "statement1").
                                        - The value for each key should be a float or integer representing the factual score.
                                        - No additional text or explanations should be included in the output; only the JSON object.
                                        Text to analyse:                                      
                                        """ + sb.toString())))
                .build();

        JsonObject result = fetchSingleResponse(apiClient, request);
        JsonArray choices = result.getJsonArray("choices");
        JsonObject firstChoice = choices.getJsonObject(0);
        JsonObject message = firstChoice.getJsonObject("message");
        return new StringBuilder(message.getString("content"));
    }

    public ArrayList<Double> extractScores(String input) {
        ArrayList<Double> numbersList = new ArrayList<>();
        String[] numbers = input.split(" ");
        for (String number : numbers) {
            numbersList.add(Double.parseDouble(number));
        }
        return numbersList;
    }

    public int sumList(List<Double> list) {
        int sum = 0;
        for (Double value : list) {
            sum += value.intValue();
        }
        return sum;
    }

    private JsonObject fetchSingleResponse(IGroqApiClient apiClient, JsonObject request) {
        final JsonObject[] result = new JsonObject[1];
        CountDownLatch latch = new CountDownLatch(1);

        Single<JsonObject> responseSingle = apiClient.createChatCompletionAsync(request);
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
