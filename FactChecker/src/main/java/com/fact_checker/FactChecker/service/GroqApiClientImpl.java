package com.fact_checker.FactChecker.service;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import org.springframework.stereotype.Service;
import java.io.StringReader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import javax.json.Json;
import javax.json.JsonObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Value;

/**
 * Implementation of the IGroqApiClient interface for interacting with the Groq API.
 * This class handles the creation of chat completions using both asynchronous and streaming approaches.
 */
@Service
public class GroqApiClientImpl implements IGroqApiClient {

    private final String apiKey;
    private final HttpClient client;

    /**
     * Constructor for GroqApiClientImpl.
     * @param apiKey The API key for authenticating with the Groq API.
     */
    public GroqApiClientImpl(@Value("${groq.api.key}") String apiKey) {
        this.apiKey = apiKey;
        ExecutorService executor = Executors.newCachedThreadPool();
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .executor(executor)
                .build();
    }

    /**
     * Creates a chat completion asynchronously.
     * @param request The JSON object containing the chat completion request.
     * @return A Single emitting a JsonObject with the chat completion response.
     */
    @Override
    public Single<JsonObject> createChatCompletionAsync(JsonObject request) {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(request.toString(), StandardCharsets.UTF_8))
                .build();

        return Single.<HttpResponse<String>>create(emitter -> {
                    client.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                            .thenAccept(emitter::onSuccess)
                            .exceptionally(throwable -> {
                                emitter.onError(throwable);
                                return null;
                            });
                }).map(HttpResponse::body)
                .map(body -> Json.createReader(new StringReader(body)).readObject());
    }

    /**
     * Creates a chat completion asynchronously and returns the result as a StringBuilder.
     * @param request The JSON object containing the chat completion request.
     * @return A StringBuilder containing the chat completion result.
     */
    @Override
    public StringBuilder createChatCompletionAsyncWithResult(JsonObject request) {
        return null;
    }

    /**
     * Creates a streaming chat completion asynchronously.
     * @param request The JSON object containing the chat completion request.
     * @return An Observable emitting JsonObjects with the streaming chat completion responses.
     */
    @Override
    public Observable<JsonObject> createChatCompletionStreamAsync(JsonObject request) {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(request.toString(), StandardCharsets.UTF_8))
                .build();

        return Observable.<String>create(emitter -> {
                    client.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString()).thenAccept(httpResponse -> {
                        try {
                            String[] lines = httpResponse.body().split("\n");
                            for (String line : lines) {
                                if (emitter.isDisposed()) {
                                    break;
                                }
                                emitter.onNext(line);
                            }
                            emitter.onComplete();
                        } catch (Exception e) {
                            emitter.onError(e);
                        }
                    }).exceptionally(throwable -> {
                        emitter.onError(throwable);
                        return null;
                    });
                }).filter(line -> line.startsWith("data: "))
                .map(line -> line.substring(6))
                .filter(jsonData -> !jsonData.equals("[DONE]"))
                .map(jsonData -> Json.createReader(new StringReader(jsonData)).readObject());
    }
}
