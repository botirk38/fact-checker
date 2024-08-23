package com.fact_checker.FactChecker.service;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.processing.SupportedSourceVersion;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TextAnalysisService {

    private final IGroqApiClient apiClient;
    private final String apiKey;

    // Inject IGroqApiClient and API key via constructor
    public TextAnalysisService(IGroqApiClient apiClient, @Value("${groq.api.key}") String apiKey) {
        this.apiClient = apiClient;
        this.apiKey = apiKey;
    }
    
    public int analyzeText(String text) {
        StringBuilder sb = new StringBuilder(text);
        StringBuilder statements = generateClaimsSeparatedByAsterisks(sb);
        StringBuilder statScore = rateClaimsByFacts(statements);
        List<Double> scores = extractScores(statScore.toString());
        return sumList(scores) / scores.size();
    }

    public int sumList(List<Double> list) {
        int sum = 0;
        for (Double value : list) {
            sum += value;
        }
        return sum;
    }
    public ArrayList<Double> extractScores(String input) {
        ArrayList<Double> numbersList = new ArrayList<>();
        Pattern pattern = Pattern.compile(":\\s*(\\d+(?:\\.\\d+)?)");
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            String numberStr = matcher.group(1);
            numbersList.add(Double.parseDouble(numberStr));
        }
        return numbersList;
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
                                        - Each key should be labeled as "statement" followed by its sequence number (e.g., "statement1").
                                        - The value for each key should be a float or integer representing the factual score.
                                        - No additional text or explanations should be included in the output; only the JSON object.
                                        Text to analyse:                                      
                                        """ + sb.toString()
                                )))
                .build();

        JsonObject result = fetchSingleResponse(request);
        System.out.println("Result : "+ result);
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
