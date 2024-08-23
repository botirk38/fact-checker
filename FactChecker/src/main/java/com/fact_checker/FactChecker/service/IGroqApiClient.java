package com.fact_checker.FactChecker.service;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

import javax.json.JsonObject;

/**
 * Interface for interacting with the Groq API client.
 * This interface defines methods for creating chat completions
 * using both asynchronous and streaming approaches.
 */
public interface IGroqApiClient {

    /**
     * Creates a chat completion asynchronously.
     *
     * @param request The JSON object containing the chat completion request.
     * @return A Single emitting a JsonObject with the chat completion response.
     */
    Single<JsonObject> createChatCompletionAsync(JsonObject request);

    /**
     * Creates a chat completion asynchronously and returns the result as a StringBuilder.
     *
     * @param request The JSON object containing the chat completion request.
     * @return A StringBuilder containing the chat completion result.
     */
    StringBuilder createChatCompletionAsyncWithResult(JsonObject request);

    /**
     * Creates a streaming chat completion asynchronously.
     *
     * @param request The JSON object containing the chat completion request.
     * @return An Observable emitting JsonObjects with the streaming chat completion responses.
     */
    Observable<JsonObject> createChatCompletionStreamAsync(JsonObject request);
}
