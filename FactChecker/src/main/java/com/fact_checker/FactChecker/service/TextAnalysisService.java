package org.groq4j;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;

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

public class TextAnalysisService {

    public static void main(String[] args) {
        // Replace with your actual API key
        String apiKey ="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";
        StringBuilder sb = new StringBuilder();
        String filePath = "C:\\Users\\i7 11Th\\Desktop\\FactMedia\\The Earth is a fascinating planet t.txt";
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append(System.lineSeparator()); // Append each line with a newline
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(sb.toString());
        IGroqApiClient apiClient = new GroqApiClientImpl(apiKey);
        //call the next method to get statments seperated by *.
        StringBuilder statements;
        statements=generateClaimsSeparatedByAsterisks(apiClient,sb);
        System.out.println("STATEMENTS :"+statements);
        StringBuilder stat_score;
        stat_score=rateClaimsByFacts(apiClient,statements);
        System.out.println("STAEMENTS & SCORE: "+stat_score);
        List<Double> scores;
        scores=extractScores(stat_score.toString());
        System.out.println("Scores: "+scores);
        int fact_rate;
        fact_rate=sumList(scores)/(scores.size());
        System.out.println("Fact rate: "+fact_rate);
        //Testing::
        int calc=fact_rate;
        for(int i=1;i<10;i++){

            statements=generateClaimsSeparatedByAsterisks(apiClient,sb);
            stat_score=rateClaimsByFacts(apiClient,statements);
            scores=extractScores(stat_score.toString());
            fact_rate=sumList(scores)/(scores.size());
            calc+=fact_rate;
        }
        System.out.println("10examples: "+ (calc/10));
    }

    public static int sumList(List<Double> list) {
        int sum = 0;
        for (int i = 0; i < list.size() ; i++) {
            sum+=(list.get(i));
        }
        return sum;
    }
    public static ArrayList<Double> extractScores(String input) {
        ArrayList<Double> numbersList = new ArrayList<>();

        // Split the input string by spaces
        String[] numbers = input.split(" ");

        // Convert each element to a Double and add it to the ArrayList
        for (String number : numbers) {
            numbersList.add(Double.parseDouble(number));
        }
        return numbersList;
    }


    public static StringBuilder rateClaimsByFacts(IGroqApiClient apiClient, StringBuilder sb){
        // Prepare a request JSON object
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

        // Call the fetchSingleResponse method
        JsonObject result = fetchSingleResponse(apiClient, request);
        // Extract the choices array
        JsonArray choices = result.getJsonArray("choices");
        // Access the first object in the choices array
        JsonObject firstChoice = choices.getJsonObject(0);
        // Access the message object within the first choice
        JsonObject message = firstChoice.getJsonObject("message");
        System.out.println("HHHHH"+message);
        // Extract the content field from the message object
        StringBuilder content = new StringBuilder(message.getString("content"));

        // Return the content
        return content;
    }
    public static StringBuilder generateClaimsSeparatedByAsterisks(IGroqApiClient apiClient, StringBuilder sb) {
        // Prepare a request JSON object
        JsonObject request = Json.createObjectBuilder()
                .add("model", "Llama-3.1-8b-Instant")
                .add("messages", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("role", "user")
                                .add("content", "Please divide the following text into distinct factual claims, with each claim separated by an asterisk (*). The text is:"
                                        + sb.toString()
                                        + " Make sure that each claim is clearly separated and represents a distinct idea or statement from the text.")))
                .build();

        // Call the fetchSingleResponse method
        JsonObject result = fetchSingleResponse(apiClient, request);

        // Extract the choices array
        JsonArray choices = result.getJsonArray("choices");

        // Access the first object in the choices array
        JsonObject firstChoice = choices.getJsonObject(0);

        // Access the message object within the first choice
        JsonObject message = firstChoice.getJsonObject("message");

        // Extract the content field from the message object
        StringBuilder content = new StringBuilder(message.getString("content"));

        // Return the content
        return content;
    }


    private static JsonObject fetchSingleResponse(IGroqApiClient apiClient, JsonObject request) {
        final JsonObject[] result = new JsonObject[1];
        CountDownLatch latch = new CountDownLatch(1);

        Single<JsonObject> responseSingle = apiClient.createChatCompletionAsync(request);
        responseSingle.subscribe(
                response -> {
                    //System.out.println("Single Response: " + response);
                    result[0] = response;
                    latch.countDown(); // Signal that the operation is complete
                },
                throwable -> {
                    System.err.println("Error: " + throwable.getMessage());
                    latch.countDown(); // Signal that the operation is complete even if there's an error
                }
        );

        try {
            latch.await(); // Wait for the asynchronous operation to complete
        } catch (InterruptedException e) {
            System.err.println("Interrupted while waiting for response: " + e.getMessage());
        }

        return result[0];
    }




}
