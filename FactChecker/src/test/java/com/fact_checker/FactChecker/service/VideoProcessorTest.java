package com.fact_checker.FactChecker.service;

import com.fact_checker.FactChecker.config.OpenAIConfig;
import com.fact_checker.FactChecker.exceptions.OpenAiException;
import com.fact_checker.FactChecker.exceptions.VideoProcessingException;
import com.fact_checker.FactChecker.model.Video;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoProcessorTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private OpenAIConfig openAIConfig;

    private VideoProcessor videoProcessor;

    @BeforeEach
    void setUp() {
        videoProcessor = new VideoProcessor(restTemplate, openAIConfig, "test-upload-path");
    }

    @Test
    void extractTextFromSpeech_Success() throws Exception {
        // Arrange
        byte[] dummyAudioData = "dummy audio data".getBytes();
        InputStream inputStream = new ByteArrayInputStream(dummyAudioData);
        String filename = "test.mp4";

        VideoProcessor spyVideoProcessor = spy(videoProcessor);
        doReturn(dummyAudioData).when(spyVideoProcessor).extractAudioFromVideo(any(InputStream.class));
        doReturn("Transcribed text").when(spyVideoProcessor).performSpeechRecognition(any(byte[].class));
        doReturn("thumbnail.png").when(spyVideoProcessor).extractThumbnail(any(InputStream.class));

        // Act
        CompletableFuture<Video> future = spyVideoProcessor.extractTextFromSpeech(inputStream, filename);
        Video result = future.get();

        // Assert
        assertNotNull(result);
        assertEquals(filename, result.getFileName());
        assertEquals("Transcribed text", result.getTranscriptionText());
        assertEquals("thumbnail.png", result.getThumbnailPath());
        assertNotNull(result.getProcessedAt());
        verify(spyVideoProcessor).extractAudioFromVideo(any(InputStream.class));
        verify(spyVideoProcessor).performSpeechRecognition(dummyAudioData);
        verify(spyVideoProcessor).extractThumbnail(any(InputStream.class));
    }

    @Test
    void extractTextFromSpeech_Failure() throws IOException {
        // Arrange
        InputStream inputStream = new ByteArrayInputStream("dummy data".getBytes());
        String filename = "test.mp4";

        VideoProcessor spyVideoProcessor = spy(videoProcessor);
        doThrow(new IOException("Test exception")).when(spyVideoProcessor).extractAudioFromVideo(any(InputStream.class));

        // Act & Assert
        CompletableFuture<Video> future = spyVideoProcessor.extractTextFromSpeech(inputStream, filename);
        ExecutionException exception = assertThrows(ExecutionException.class, future::get);
        assertInstanceOf(VideoProcessingException.class, exception.getCause());
        assertEquals("Error processing video", exception.getCause().getMessage());
    }

    @Test
    void performSpeechRecognition_Success() {
        // Arrange
        byte[] dummyAudioData = "dummy audio data".getBytes();
        VideoProcessor.TranscriptionResponse transcriptionResponse = new VideoProcessor.TranscriptionResponse();
        transcriptionResponse.setText("Transcribed text");

        ResponseEntity<VideoProcessor.TranscriptionResponse> mockResponse = new ResponseEntity<>(transcriptionResponse, HttpStatus.OK);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(VideoProcessor.TranscriptionResponse.class)
        )).thenReturn(mockResponse);

        when(openAIConfig.getApiUrl()).thenReturn("https://api.openai.com/v1/audio/transcriptions");
        when(openAIConfig.getApiKey()).thenReturn("test-api-key");

        // Act
        String result = videoProcessor.performSpeechRecognition(dummyAudioData);

        // Assert
        assertEquals("Transcribed text", result);
        verify(restTemplate).exchange(
                eq("https://api.openai.com/v1/audio/transcriptions"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(VideoProcessor.TranscriptionResponse.class)
        );
    }

    @Test
    void performSpeechRecognition_Failure() {
        // Arrange
        byte[] dummyAudioData = "dummy audio data".getBytes();
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(VideoProcessor.TranscriptionResponse.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        when(openAIConfig.getApiUrl()).thenReturn("https://api.openai.com/v1/audio/transcriptions");
        when(openAIConfig.getApiKey()).thenReturn("test-api-key");

        // Act & Assert
        OpenAiException exception = assertThrows(OpenAiException.class, () -> videoProcessor.performSpeechRecognition(dummyAudioData));
        assertTrue(exception.getMessage().contains("Error performing speech recognition"));
    }
}
