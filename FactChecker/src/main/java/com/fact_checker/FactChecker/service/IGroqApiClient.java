package org.groq4j;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

import javax.json.JsonObject;

public interface IGroqApiClient {
    Single<JsonObject> createChatCompletionAsync(JsonObject request);

    StringBuilder createChatCompletionAsyncWithResult(JsonObject request);

    Observable<JsonObject> createChatCompletionStreamAsync(JsonObject request);
}
