package com.fact_checker.FactChecker.service;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import javax.json.JsonObject;

public interface IGroqApiClient {

    Single<JsonObject> createChatCompletionAsync(JsonObject request);

    StringBuilder createChatCompletionAsyncWithResult(JsonObject request);

    Observable<JsonObject> createChatCompletionStreamAsync(JsonObject request);
}
