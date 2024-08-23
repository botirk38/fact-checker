package com.fact_checker.FactChecker.service;

import com.fact_checker.FactChecker.config.OpenAIConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
public class TextVectorization{
private final OpenAIConfig openAiConfig;
private final OkHttpClient client;
private final ObjectMapper objectMapper;
private final String model;
private final int dim;

private static final String API_URL = "https://api.openai.com/v1/embeddings";

public TextVectorization(OpenAIConfig openAiConfig, OkHttpClient client, ObjectMapper objectMapper, String model, int dim){
        this.openAiConfig=openAiConfig;
        this.client=client;
        this.objectMapper=objectMapper;
        this.dim=dim;
        this.model=model;
        }

public double[] getEmbedding(String text,int dim) throws IOException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("input", text);
        payload.put("model", "text-embedding-3-small");
        payload.put("dimensions", dim);


    String jsonPayload = objectMapper.writeValueAsString(payload);

        RequestBody body = RequestBody.create(jsonPayload, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
        .url(API_URL)
        .post(body)
        .addHeader("Authorization", "Bearer " + openAiConfig.getApiKey())
        .addHeader("Content-Type", "application/json")
        .build();

        try (Response response = client.newCall(request).execute()) {
        if (!response.isSuccessful()) {
        throw new IOException("API request failed with code " + response.code() + ": " + response.body().string());
        }

        String responseBody = response.body().string();
        JsonNode jsonResponse = objectMapper.readTree(responseBody);
        JsonNode embeddingNode = jsonResponse.path("data").path(0).path("embedding");

        if (embeddingNode.isMissingNode()) {
        throw new IOException("Embedding data not found in the response");
        }

        double[] embedding = new double[dim];
        for (int i = 0; i < dim; i++) {
        embedding[i] = embeddingNode.get(i).asDouble();
        }

        return embedding;
        }
        }

    public static void main(String[] args) {
        try {
            // Instantiate necessary dependencies
            OpenAIConfig openAiConfig = new OpenAIConfig();
            OkHttpClient client = new OkHttpClient();
            ObjectMapper objectMapper = new ObjectMapper();
            String model = "text-embedding-3-small";
            int dimensions = 512;

            // Create an instance of the class
            TextVectorization vectorizer = new TextVectorization(openAiConfig, client, objectMapper, model, dimensions);

            // Call the non-static method using the instance
            double[] vector = vectorizer.getEmbedding("Convert this text into a vector.",512);
            System.out.println("Embedding Vector: " + Arrays.toString(vector));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
        }