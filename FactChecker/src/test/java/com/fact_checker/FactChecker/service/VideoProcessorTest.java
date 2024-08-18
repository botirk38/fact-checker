package com.fact_checker.FactChecker.service;

import com.fact_checker.FactChecker.model.Video;
import com.fact_checker.FactChecker.repository.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoProcessorTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private VideoRepository transcriptionRepository;

    private VideoProcessor videoProcessor;

    @BeforeEach
    void setUp() {
        videoProcessor = new VideoProcessor(restTemplate);
        ReflectionTestUtils.setField(videoProcessor, "openaiApiKey", "test-api-key");
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

        // Act
        CompletableFuture<Video> future = spyVideoProcessor.extractTextFromSpeech(inputStream, filename);
        Video result = future.get();

        // Assert
        assertNotNull(result);
        assertEquals(filename, result.getFileName());
        assertEquals("Transcribed text", result.getTranscriptionText());
        assertNotNull(result.getProcessedAt());
        verify(spyVideoProcessor).extractAudioFromVideo(inputStream);
        verify(spyVideoProcessor).performSpeechRecognition(dummyAudioData);
    }
    @Test
    void extractAudioFromVideo_Success() throws Exception {
        // This test remains the same as it's mocked in other tests
        VideoProcessor spyVideoProcessor = spy(videoProcessor);
        doReturn(new byte[]{1, 2, 3}).when(spyVideoProcessor).extractAudioFromVideo(any(InputStream.class));

        byte[] result = spyVideoProcessor.extractAudioFromVideo(new ByteArrayInputStream(new byte[0]));
        assertNotNull(result);
        assertTrue(result.length > 0);
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
                any(),
                eq(VideoProcessor.TranscriptionResponse.class)
        )).thenReturn(mockResponse);

        // Act
        String result = videoProcessor.performSpeechRecognition(dummyAudioData);

        // Assert
        assertEquals("Transcribed text", result);
        verify(restTemplate).exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(),
                eq(VideoProcessor.TranscriptionResponse.class)
        );
    }

    @Test
    void performSpeechRecognition_Failure() {
        // Arrange
        byte[] dummyAudioData = "dummy audio data".getBytes();
        ResponseEntity<VideoProcessor.TranscriptionResponse> mockResponse = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(),
                eq(VideoProcessor.TranscriptionResponse.class)
        )).thenReturn(mockResponse);

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            videoProcessor.performSpeechRecognition(dummyAudioData);
        });
        assertTrue(exception.getMessage().contains("Failed to transcribe audio"));
    }
}
