package com.fact_checker.FactChecker.service;

import com.fact_checker.FactChecker.config.OpenAIConfig;
import com.fact_checker.FactChecker.exceptions.EmbeddingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knuddels.jtokkit.api.Encoding;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class VectorizationService {
    private static final Logger logger = LoggerFactory.getLogger(VectorizationService.class);
    private static final String MODEL = "text-embedding-3-small";
    private static final int DIMENSIONS = 768;
    private static final int TOKEN_LIMIT = 8191;
    private static final String EMBEDDINGS_ENDPOINT = "/embeddings";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String JSON_MEDIA_TYPE = "application/json; charset=utf-8";

    private final OpenAIConfig openAiConfig;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final Encoding encoding;


    public double[] getEmbedding(String text, int dim) throws EmbeddingException {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("Input text cannot be null or empty");
        }

        var tokens = encoding.encode(text);

        if (tokens.size() > TOKEN_LIMIT) {
            throw new IllegalArgumentException(
                    String.format("Input exceeds the token limit of %d. Current token count: %d", TOKEN_LIMIT, tokens.size()));
        }

        String decodedTokens = encoding.decode(tokens);

        try {
            String jsonPayload = createJsonPayload(decodedTokens);
            Request request = createRequest(jsonPayload);
            return executeRequest(request, dim);
        } catch (IOException e) {
            logger.error("Error during embedding process", e);
            throw new EmbeddingException("Failed to get embedding", e);
        }
    }

    private String createJsonPayload(String decodedTokens) throws IOException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("input", decodedTokens);
        payload.put("model", MODEL);
        payload.put("dimensions", DIMENSIONS);
        return objectMapper.writeValueAsString(payload);
    }

    private Request createRequest(String jsonPayload) {
        RequestBody body = RequestBody.create(jsonPayload, MediaType.get(JSON_MEDIA_TYPE));
        return new Request.Builder()
                .url(openAiConfig.getApiUrl() + EMBEDDINGS_ENDPOINT)
                .post(body)
                .addHeader(AUTHORIZATION_HEADER, BEARER_PREFIX + openAiConfig.getApiKey())
                .addHeader(CONTENT_TYPE_HEADER, JSON_MEDIA_TYPE)
                .build();
    }

    private double[] executeRequest(Request request, int dim) throws IOException, EmbeddingException {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new EmbeddingException("API request failed with code " + response.code());
            }

            try (ResponseBody responseBody = response.body()) {
                if (responseBody == null) {
                    throw new EmbeddingException("Response body is null");
                }
                String responseBodyString = responseBody.string();
                JsonNode jsonResponse = objectMapper.readTree(responseBodyString);
                JsonNode embeddingNode = jsonResponse.path("data").path(0).path("embedding");

                if (embeddingNode.isMissingNode()) {
                    throw new EmbeddingException("Embedding data not found in the response");
                }

                return IntStream.range(0, dim)
                        .mapToDouble(i -> embeddingNode.get(i).asDouble())
                        .toArray();
            }
        }
    }
}
