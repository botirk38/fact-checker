package com.fact_checker.FactChecker.service;

import com.fact_checker.FactChecker.model.Video;
import com.fact_checker.FactChecker.repository.VideoRepository;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.json.*;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TextAnalysisServiceTest {

    @Mock
    private IGroqApiClient apiClient;

    private TextAnalysisService textAnalysisService;

    @Mock
    private VideoRepository videoRepository;

    @BeforeEach
    void setUp() {
        textAnalysisService = new TextAnalysisService(apiClient, "test-api-key", videoRepository);
    }

    @Test
    void analyzeText_shouldReturnCorrectAverageScore() {
        // Arrange
        Video video = new Video();
        video.setTranscriptionText("Test transcription");

        mockApiResponses("Claim 1*Claim 2",
                "{\"Claim 1\": 80, \"Claim 2\": 60}");

        // Act
        double result = textAnalysisService.analyzeText(video);

        // Assert
        assertEquals(70.0, result, 0.01);
        assertEquals(70.0, video.getFactPercentage(), 0.01);
        assertEquals(0, video.getFalseStatements().size());
    }

    @Test
    void analyzeText_shouldIdentifyFalseClaims() {
        // Arrange
        Video video = new Video();
        video.setTranscriptionText("Test transcription");

        mockApiResponses("Claim 1*Claim 2*Claim 3",
                "{\"Claim 1\": 80, \"Claim 2\": 40, \"Claim 3\": 30}");

        // Act
        double result = textAnalysisService.analyzeText(video);

        // Assert
        assertEquals(50.0, result, 0.01);
        assertEquals(50.0, video.getFactPercentage(), 0.01);
        assertEquals(2, video.getFalseStatements().size());
        assertTrue(video.getFalseStatements().contains("Claim 2"));
        assertTrue(video.getFalseStatements().contains("Claim 3"));
    }

    @Test
    void analyzeText_shouldHandleEmptyTranscription() {
        // Arrange
        Video video = new Video();
        video.setTranscriptionText("");

        mockApiResponses("", "{}");

        // Act
        double result = textAnalysisService.analyzeText(video);

        // Assert
        assertEquals(0.0, result, 0.01);
        assertEquals(0.0, video.getFactPercentage(), 0.01);
        assertTrue(video.getFalseStatements().isEmpty());
    }

    @Test
    void analyzeText_shouldHandleApiErrors() {
        // Arrange
        Video video = new Video();
        video.setTranscriptionText("Test transcription");

        // Mock the API client to simulate an error
        when(apiClient.createChatCompletionAsync(any(JsonObject.class)))
                .thenReturn(Single.error(new RuntimeException("API Error")));

        // Act
        double result = textAnalysisService.analyzeText(video);

        // Assert
        assertEquals(0.0, result, 0.001);
        assertEquals(0.0, video.getFactPercentage(), 0.001);
        assertTrue(video.getFalseStatements().isEmpty());

        // Verify that the API was called
        verify(apiClient, times(4)).createChatCompletionAsync(any(JsonObject.class));
    }
    @Test
    void getFalseClaims_shouldReturnCorrectClaims() {
        // Arrange
        Map<String, Double> scoredClaims = Map.of(
                "Claim 1", 80.0,
                "Claim 2", 40.0,
                "Claim 3", 60.0,
                "Claim 4", 30.0
        );

        // Act
        List<String> falseClaims = textAnalysisService.getFalseClaims(scoredClaims);

        // Assert
        assertEquals(2, falseClaims.size());
        assertTrue(falseClaims.contains("Claim 2"));
        assertTrue(falseClaims.contains("Claim 4"));
    }

    @Test
    void getAverageScore_shouldCalculateCorrectly() {
        // Arrange
        Map<String, Double> scoredClaims = Map.of(
                "Claim 1", 0.8,
                "Claim 2", 0.4,
                "Claim 3", 0.6
        );

        // Act
        double averageScore = textAnalysisService.getAverageScore(scoredClaims);

        // Assert
        assertEquals(0.6, averageScore, 0.01);
    }

    @Test
    void convertJsonToMap_shouldParseCorrectly() {
        // Arrange
        String jsonString = "{\"Claim 1\": 80.5, \"Claim 2\": 60.0}";

        // Act
        Map<String, Double> result = textAnalysisService.convertJsonToMap(jsonString);

        // Assert
        assertEquals(2, result.size());
        assertEquals(80.5, result.get("Claim 1"), 0.01);
        assertEquals(60.0, result.get("Claim 2"), 0.01);
    }

    private void mockApiResponses(String claimsResponse, String scoresResponse) {
        JsonObject claimsResult = createMockJsonResponse(claimsResponse);
        JsonObject scoresResult = createMockJsonResponse(scoresResponse);

        when(apiClient.createChatCompletionAsync(any()))
                .thenReturn(Single.just(claimsResult))
                .thenReturn(Single.just(scoresResult));
    }

    private JsonObject createMockJsonResponse(String content) {
        return Json.createObjectBuilder()
                .add("choices", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("message", Json.createObjectBuilder()
                                        .add("content", content))))
                .build();
    }
}
