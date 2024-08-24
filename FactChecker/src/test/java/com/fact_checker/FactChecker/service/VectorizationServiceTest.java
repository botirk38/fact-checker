package com.fact_checker.FactChecker.service;

import com.fact_checker.FactChecker.config.OpenAIConfig;
import com.fact_checker.FactChecker.exceptions.EmbeddingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.IntArrayList;
import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VectorizationServiceTest {

    @Mock
    private OpenAIConfig openAiConfig;

    @Mock
    private OkHttpClient client;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Encoding encoding;

    @Mock
    private Call call;

    @Mock
    private Response response;

    @Mock
    private ResponseBody responseBody;

    private VectorizationService vectorizationService;

    @BeforeEach
    void setUp() {
        vectorizationService = new VectorizationService(openAiConfig, client, objectMapper, encoding);
    }

    @Test
    void getEmbedding_SuccessfulRequest_ReturnsEmbedding() throws Exception {
        // Arrange
        String inputText = "Test text";
        int dim = 3;
        IntArrayList tokenList = new IntArrayList(3);
        tokenList.add(1);
        tokenList.add(2);
        tokenList.add(3);
        String decodedTokens = "Decoded tokens";
        String jsonPayload = "{\"input\":\"Decoded tokens\",\"model\":\"text-embedding-3-small\",\"dimensions\":768}";
        String responseJson = "{\"data\":[{\"embedding\":[0.1,0.2,0.3]}]}";

        when(encoding.encode(inputText)).thenReturn(tokenList);
        when(encoding.decode(tokenList)).thenReturn(decodedTokens);
        when(objectMapper.writeValueAsString(any())).thenReturn(jsonPayload);
        when(openAiConfig.getApiUrl()).thenReturn("https://test.com");
        when(openAiConfig.getApiKey()).thenReturn("test-key");
        when(client.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);
        when(responseBody.string()).thenReturn(responseJson);

        JsonNode jsonNode = mock(JsonNode.class);
        JsonNode dataNode = mock(JsonNode.class);
        JsonNode embeddingNode = mock(JsonNode.class);
        when(objectMapper.readTree(responseJson)).thenReturn(jsonNode);
        when(jsonNode.path("data")).thenReturn(dataNode);
        when(dataNode.path(0)).thenReturn(dataNode);
        when(dataNode.path("embedding")).thenReturn(embeddingNode);
        when(embeddingNode.isMissingNode()).thenReturn(false);
        when(embeddingNode.get(anyInt())).thenReturn(mock(JsonNode.class));
        when(embeddingNode.get(anyInt()).asDouble()).thenReturn(0.1, 0.2, 0.3);

        // Act
        double[] result = vectorizationService.getEmbedding(inputText, dim);

        // Assert
        assertArrayEquals(new double[]{0.1, 0.2, 0.3}, result, 0.001);
    }

    @Test
    void getEmbedding_NullInput_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> vectorizationService.getEmbedding(null, 3));
    }

    @Test
    void getEmbedding_EmptyInput_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> vectorizationService.getEmbedding("", 3));
    }

    @Test
    void getEmbedding_TokenLimitExceeded_ThrowsIllegalArgumentException() {
        // Arrange
        String inputText = "Test text";
        IntArrayList tokenList = mock(IntArrayList.class);
        when(tokenList.size()).thenReturn(10000); // Exceeds TOKEN_LIMIT
        when(encoding.encode(inputText)).thenReturn(tokenList);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> vectorizationService.getEmbedding(inputText, 3));
    }
    @Test
    void getEmbedding_ApiRequestFailed_ThrowsEmbeddingException() throws Exception {
        // Arrange
        String inputText = "Test text";
        IntArrayList tokenList = new IntArrayList(3);
        tokenList.add(1);
        tokenList.add(2);
        tokenList.add(3);
        String decodedTokens = "Decoded tokens";
        String jsonPayload = "{\"input\":\"Decoded tokens\",\"model\":\"text-embedding-3-small\",\"dimensions\":768}";

        when(encoding.encode(inputText)).thenReturn(tokenList);
        when(encoding.decode(tokenList)).thenReturn(decodedTokens);
        when(objectMapper.writeValueAsString(any())).thenReturn(jsonPayload);
        when(openAiConfig.getApiUrl()).thenReturn("https://test.com");
        when(openAiConfig.getApiKey()).thenReturn("test-key");
        when(client.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(false);
        when(response.code()).thenReturn(400);

        // Act & Assert
        assertThrows(EmbeddingException.class, () -> vectorizationService.getEmbedding(inputText, 3));
    }

    @Test
    void getEmbedding_NullResponseBody_ThrowsEmbeddingException() throws Exception {
        // Arrange
        String inputText = "Test text";
        IntArrayList tokenList = new IntArrayList(3);
        tokenList.add(1);
        tokenList.add(2);
        tokenList.add(3);

        String decodedTokens = "Decoded tokens";
        String jsonPayload = "{\"input\":\"Decoded tokens\",\"model\":\"text-embedding-3-small\",\"dimensions\":768}";

        when(encoding.encode(inputText)).thenReturn(tokenList);
        when(encoding.decode(tokenList)).thenReturn(decodedTokens);
        when(objectMapper.writeValueAsString(any())).thenReturn(jsonPayload);
        when(openAiConfig.getApiUrl()).thenReturn("https://test.com");
        when(openAiConfig.getApiKey()).thenReturn("test-key");
        when(client.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(null);

        // Act & Assert
        assertThrows(EmbeddingException.class, () -> vectorizationService.getEmbedding(inputText, 3));
    }

    @Test
    void getEmbedding_MissingEmbeddingData_ThrowsEmbeddingException() throws Exception {
        // Arrange
        String inputText = "Test text";
        IntArrayList tokenList = new IntArrayList(3);
        tokenList.add(1);
        tokenList.add(2);
        tokenList.add(3);

        String decodedTokens = "Decoded tokens";
        String jsonPayload = "{\"input\":\"Decoded tokens\",\"model\":\"text-embedding-3-small\",\"dimensions\":768}";
        String responseJson = "{\"data\":[{}]}";

        when(encoding.encode(inputText)).thenReturn(tokenList);
        when(encoding.decode(tokenList)).thenReturn(decodedTokens);
        when(objectMapper.writeValueAsString(any())).thenReturn(jsonPayload);
        when(openAiConfig.getApiUrl()).thenReturn("https://test.com");
        when(openAiConfig.getApiKey()).thenReturn("test-key");
        when(client.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);
        when(responseBody.string()).thenReturn(responseJson);

        JsonNode jsonNode = mock(JsonNode.class);
        JsonNode dataNode = mock(JsonNode.class);
        JsonNode embeddingNode = mock(JsonNode.class);
        when(objectMapper.readTree(responseJson)).thenReturn(jsonNode);
        when(jsonNode.path("data")).thenReturn(dataNode);
        when(dataNode.path(0)).thenReturn(dataNode);
        when(dataNode.path("embedding")).thenReturn(embeddingNode);
        when(embeddingNode.isMissingNode()).thenReturn(true);

        // Act & Assert
        assertThrows(EmbeddingException.class, () -> vectorizationService.getEmbedding(inputText, 3));
    }
}
