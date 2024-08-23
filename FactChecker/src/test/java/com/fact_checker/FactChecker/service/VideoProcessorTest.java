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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.io.InputStream;

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
    Path tempFile = Files.createTempFile("test", ".mp4");
    String filename = "test.mp4";
    VideoProcessor spyVideoProcessor = spy(videoProcessor);

    doReturn("dummy audio data".getBytes()).when(spyVideoProcessor).extractAudioFromVideo(any());
    doReturn("Transcribed text").when(spyVideoProcessor).performSpeechRecognition(any(byte[].class));
    doReturn("thumbnail.png").when(spyVideoProcessor).extractThumbnail(any(InputStream.class), anyString());

    // Act
    CompletableFuture<Video> future = spyVideoProcessor.extractTextFromSpeech(tempFile, filename);
    Video result = future.get();

    // Assert
    assertNotNull(result);
    assertEquals(filename, result.getFileName());
    assertEquals("Transcribed text", result.getTranscriptionText());
    assertEquals("thumbnail.png", result.getThumbnailPath());
    assertNotNull(result.getProcessedAt());

    verify(spyVideoProcessor).extractAudioFromVideo(any());
    verify(spyVideoProcessor).performSpeechRecognition(any(byte[].class));
    verify(spyVideoProcessor).extractThumbnail(any(InputStream.class), anyString());

    // Clean up
    Files.deleteIfExists(tempFile);
  }

  @Test
  void extractTextFromSpeech_Failure() throws IOException {
    // Arrange
    Path tempFile = Files.createTempFile("test", ".mp4");
    String filename = "test.mp4";
    VideoProcessor spyVideoProcessor = spy(videoProcessor);

    doThrow(new IOException("Test exception")).when(spyVideoProcessor).extractAudioFromVideo(any());

    // Act & Assert
    CompletableFuture<Video> future = spyVideoProcessor.extractTextFromSpeech(tempFile, filename);
    ExecutionException exception = assertThrows(ExecutionException.class, future::get);
    assertInstanceOf(VideoProcessingException.class, exception.getCause());
    assertEquals("Error processing video: test.mp4", exception.getCause().getMessage());

  }

  @Test
  void performSpeechRecognition_Success() {
    // Arrange
    byte[] dummyAudioData = "dummy audio data".getBytes();
    VideoProcessor.TranscriptionResponse transcriptionResponse = new VideoProcessor.TranscriptionResponse();
    transcriptionResponse.setText("Transcribed text");
    ResponseEntity<VideoProcessor.TranscriptionResponse> mockResponse = new ResponseEntity<>(transcriptionResponse,
        HttpStatus.OK);

    when(restTemplate.exchange(
        anyString(),
        eq(HttpMethod.POST),
        any(HttpEntity.class),
        eq(VideoProcessor.TranscriptionResponse.class))).thenReturn(mockResponse);
    when(openAIConfig.getApiUrl()).thenReturn("https://api.openai.com/v1/");
    when(openAIConfig.getApiKey()).thenReturn("test-api-key");

    // Act
    String result = videoProcessor.performSpeechRecognition(dummyAudioData);

    // Assert
    assertEquals("Transcribed text", result);
    verify(restTemplate).exchange(
        eq("https://api.openai.com/v1/audio/transcriptions"),
        eq(HttpMethod.POST),
        any(HttpEntity.class),
        eq(VideoProcessor.TranscriptionResponse.class));
  }

  @Test
  void performSpeechRecognition_Failure() {
    // Arrange
    byte[] dummyAudioData = "dummy audio data".getBytes();
    when(restTemplate.exchange(
        anyString(),
        eq(HttpMethod.POST),
        any(HttpEntity.class),
        eq(VideoProcessor.TranscriptionResponse.class)))
        .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));
    when(openAIConfig.getApiUrl()).thenReturn("https://api.openai.com/v1/audio/transcriptions");
    when(openAIConfig.getApiKey()).thenReturn("test-api-key");

    // Act & Assert
    OpenAiException exception = assertThrows(OpenAiException.class,
        () -> videoProcessor.performSpeechRecognition(dummyAudioData));
    assertTrue(exception.getMessage().contains("Error performing speech recognition"));
  }
}
