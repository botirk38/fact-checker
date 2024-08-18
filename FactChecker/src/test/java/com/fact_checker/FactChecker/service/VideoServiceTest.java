package com.fact_checker.FactChecker.service;

import com.fact_checker.FactChecker.exceptions.FileProcessingException;
import com.fact_checker.FactChecker.exceptions.InvalidFileException;
import com.fact_checker.FactChecker.model.Video;
import com.fact_checker.FactChecker.repository.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoServiceTest {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private VideoProcessor videoProcessor;

    private VideoService videoService;

    private static final String UPLOAD_PATH = "test-upload-path";

    @BeforeEach
    void setUp() {
        videoService = new VideoService(videoRepository, videoProcessor, UPLOAD_PATH);
    }

    @Test
    void processAndSaveVideo_validFile_success() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "test.mp4", "video/mp4", "test data".getBytes());
        Video processedVideo = new Video();
        processedVideo.setId(1L);

        when(videoProcessor.extractTextFromSpeech(any(InputStream.class), anyString()))
                .thenReturn(CompletableFuture.completedFuture(processedVideo));
        when(videoRepository.save(any(Video.class))).thenReturn(processedVideo);

        Files.createDirectories(Paths.get(UPLOAD_PATH));

        Video result = videoService.processAndSaveVideo(file).get();

        assertThat(result).isEqualTo(processedVideo);
        verify(videoRepository).save(any(Video.class));
        verify(videoProcessor).extractTextFromSpeech(any(InputStream.class), anyString());
    }

    @Test
    void processAndSaveVideo_emptyFile_throwsInvalidFileException() {
        MultipartFile emptyFile = new MockMultipartFile("file", "empty.mp4", "video/mp4", new byte[0]);

        assertThatThrownBy(() -> videoService.processAndSaveVideo(emptyFile).get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(InvalidFileException.class)
                .hasRootCauseMessage("File is empty");
    }

    @Test
    void processAndSaveVideo_unsupportedFileExtension_throwsInvalidFileException() {
        MultipartFile unsupportedFile = new MockMultipartFile("file", "test.txt", "text/plain", "test data".getBytes());

        assertThatThrownBy(() -> videoService.processAndSaveVideo(unsupportedFile).get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(InvalidFileException.class)
                .hasRootCauseMessage("File extension is not supported");
    }

    @Test
    void processAndSaveVideo_ioExceptionDuringSave_throwsFileProcessingException() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "test.mp4", "video/mp4", "test data".getBytes());

        when(videoProcessor.extractTextFromSpeech(any(InputStream.class), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IOException("Simulated IO error")));

        Files.createDirectories(Paths.get(UPLOAD_PATH));

        assertThatThrownBy(() -> videoService.processAndSaveVideo(file).get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(FileProcessingException.class)
                .hasRootCauseMessage("Simulated IO error");
    }

    @Test
    void processAndSaveVideo_exceptionDuringProcessing_throwsFileProcessingException() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "test.mp4", "video/mp4", "test data".getBytes());

        when(videoProcessor.extractTextFromSpeech(any(InputStream.class), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Processing error")));

        Files.createDirectories(Paths.get(UPLOAD_PATH));

        assertThatThrownBy(() -> videoService.processAndSaveVideo(file).get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(FileProcessingException.class)
                .hasRootCauseMessage("Processing error");
    }

    @Test
    void processAndSaveVideo_nullFilename_throwsInvalidFileException() {
        MultipartFile fileWithNullName = new MockMultipartFile("file", null, "video/mp4", "test data".getBytes());

        assertThatThrownBy(() -> videoService.processAndSaveVideo(fileWithNullName).get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(InvalidFileException.class)
                .hasRootCauseMessage("File extension is not supported");
    }
}
