package com.fact_checker.FactChecker.service;

import com.fact_checker.FactChecker.exceptions.OpenAiException;
import com.fact_checker.FactChecker.exceptions.VideoProcessingException;
import com.fact_checker.FactChecker.model.Video;
import com.fact_checker.FactChecker.config.OpenAIConfig;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.Setter;
import org.bytedeco.javacv.*;
import org.bytedeco.ffmpeg.global.avcodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Service class for processing video files.
 * This class handles video processing tasks such as extracting audio, performing speech recognition,
 * and generating thumbnails.
 */
@Service
public class VideoProcessor {

  private static final int BIT_RATE = 19200;
  private static final int AUDIO_QUALITY = 0;
  private static final Logger logger = LoggerFactory.getLogger(VideoProcessor.class);

  private final ExecutorService executorService;
  private final RestTemplate restTemplate;
  private final OpenAIConfig openAiConfig;
  private final String thumbnailUploadPath;

  /**
   * Constructor for VideoProcessor.
   *
   * @param restTemplate RestTemplate for making HTTP requests
   * @param openAiConfig Configuration for OpenAI API
   * @param thumbnailUploadPath Path for uploading thumbnails
   */
  public VideoProcessor(RestTemplate restTemplate, OpenAIConfig openAiConfig,
                        @Value("${thumbnail.upload.path}") String thumbnailUploadPath) {
    this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    this.restTemplate = restTemplate;
    this.openAiConfig = openAiConfig;
    this.thumbnailUploadPath = thumbnailUploadPath;
    initializeThumbnailDirectory();
  }

  /**
   * Initializes the thumbnail directory.
   * Creates the directory if it doesn't exist and checks if it's writable.
   */
  private void initializeThumbnailDirectory() {
    Path thumbnailUploadPathDir = Paths.get(thumbnailUploadPath);
    if (!Files.exists(thumbnailUploadPathDir)) {
      try {
        Files.createDirectories(thumbnailUploadPathDir);
        logger.info("Thumbnail upload directory created: {}", thumbnailUploadPath);
      } catch (IOException e) {
        throw new RuntimeException("Could not create thumbnail upload path: " + thumbnailUploadPath, e);
      }
    }
    if (!Files.isWritable(thumbnailUploadPathDir)) {
      throw new RuntimeException("Thumbnail upload path is not writable: " + thumbnailUploadPath);
    }
  }

  /**
   * Extracts text from speech in a video file.
   *
   * @param filePath Path to the video file
   * @param filename Name of the video file
   * @return CompletableFuture<Video> containing processed video information
   */
  @Cacheable(value = "videos", key = "#filename")
  public CompletableFuture<Video> extractTextFromSpeech(Path filePath, String filename) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        byte[] fileBytes = Files.readAllBytes(filePath);
        byte[] audioData = extractAudioFromVideo(new ByteArrayInputStream(fileBytes));
        String transcriptionText = performSpeechRecognition(audioData);
        String thumbnailPath = extractThumbnail(new ByteArrayInputStream(fileBytes), filename);

        Video video = new Video();
        video.setFileName(filename);
        video.setTranscriptionText(transcriptionText);
        video.setThumbnailPath(thumbnailPath);
        video.setProcessedAt(LocalDateTime.now());

        logger.info("Video processed successfully: {}", filename);
        return video;
      } catch (Exception e) {
        logger.error("Error processing video: {}", filename, e);
        throw new VideoProcessingException("Error processing video: " + filename, e);
      }
    }, executorService);
  }

  /**
   * Extracts audio from a video input stream.
   *
   * @param videoInputStream InputStream of the video file
   * @return byte array containing the extracted audio data
   * @throws IOException if an I/O error occurs
   */
  @Cacheable(value = "audioExtractions", key = "#videoInputStream.hashCode()")
  public byte[] extractAudioFromVideo(InputStream videoInputStream) throws IOException {
    File tempFile = File.createTempFile("audio", ".mp3");
    try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(tempFile, 0);
         FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoInputStream)) {
      grabber.start();
      recorder.setAudioCodec(avcodec.AV_CODEC_ID_MP3);
      recorder.setSampleRate(grabber.getSampleRate());
      recorder.setAudioChannels(grabber.getAudioChannels());
      recorder.setAudioQuality(AUDIO_QUALITY);
      recorder.setAudioBitrate(BIT_RATE);
      recorder.start();

      Frame frame;
      while ((frame = grabber.grab()) != null) {
        if (frame.samples != null) {
          recorder.record(frame);
        }
      }

      recorder.stop();
      grabber.stop();

      byte[] audioData = Files.readAllBytes(tempFile.toPath());
      if (!tempFile.delete()) {
        logger.warn("Failed to delete temporary audio file: {}", tempFile.getAbsolutePath());
      }
      return audioData;
    }
  }

  /**
   * Performs speech recognition on audio data.
   *
   * @param audioData byte array containing audio data
   * @return String containing the transcribed text
   */
  @Cacheable(value = "transcriptions", key = "#audioData.hashCode()")
  public String performSpeechRecognition(byte[] audioData) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + openAiConfig.getApiKey());

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    try {
      File tempFile = File.createTempFile("audio", ".mp3");
      try (FileOutputStream fos = new FileOutputStream(tempFile)) {
        fos.write(audioData);
      }

      body.add("file", new FileSystemResource(tempFile));
      body.add("model", "whisper-1");

      HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

      ResponseEntity<TranscriptionResponse> response = restTemplate.exchange(
              openAiConfig.getApiUrl(),
              HttpMethod.POST,
              requestEntity,
              TranscriptionResponse.class);

      if (!tempFile.delete()) {
        logger.warn("Failed to delete temporary speech file: {}", tempFile.getAbsolutePath());
      }

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        return response.getBody().getText();
      } else {
        throw new OpenAiException("Failed to transcribe audio: " + response.getStatusCode(), null);
      }
    } catch (IOException e) {
      throw new VideoProcessingException("Error creating temporary file for audio data", e);
    } catch (HttpClientErrorException e) {
      throw new OpenAiException("Error performing speech recognition: " + e.getResponseBodyAsString(), e);
    }
  }

  /**
   * Extracts a thumbnail from a video input stream.
   *
   * @param videoInputStream InputStream of the video file
   * @param filename Name of the video file
   * @return String containing the path to the extracted thumbnail
   * @throws IOException if an I/O error occurs
   */
  @Cacheable(value = "thumbnails", key = "#filename")
  public String extractThumbnail(InputStream videoInputStream, String filename) throws IOException {
    try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoInputStream);
         Java2DFrameConverter converter = new Java2DFrameConverter()) {
      grabber.start();
      long durationInMicroseconds = grabber.getLengthInTime();
      if (durationInMicroseconds <= 0) {
        throw new VideoProcessingException("Unable to determine video duration", null);
      }

      long targetPosition = durationInMicroseconds / 10;
      grabber.setTimestamp(targetPosition, true);
      Frame frame = grabber.grabImage();

      if (frame != null && frame.image != null) {
        BufferedImage bufferedImage = converter.getBufferedImage(frame);
        if (bufferedImage != null) {
          String thumbnailFileName = UUID.randomUUID() + ".png";
          Path thumbnailPath = Paths.get(thumbnailUploadPath, thumbnailFileName);
          if (ImageIO.write(bufferedImage, "png", thumbnailPath.toFile())) {
            return thumbnailFileName;
          }
        }
      }

      throw new VideoProcessingException("Could not extract a valid thumbnail from the video", null);
    } catch (FFmpegFrameGrabber.Exception e) {
      throw new IOException("Error processing video with FFmpeg", e);
    }
  }

  /**
   * Clears caches for a specific video file.
   *
   * @param filename Name of the video file
   */
  @CacheEvict(value = { "videos", "audioExtractions", "transcriptions", "thumbnails" }, key = "#filename")
  public void clearCaches(String filename) {
    logger.info("Cleared caches for video: {}", filename);
  }

  /**
   * Shuts down the executor service.
   */
  @PreDestroy
  public void shutdown() {
    executorService.shutdown();
    try {
      if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
      }
    } catch (InterruptedException e) {
      executorService.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Inner class representing the response from the transcription service.
   */
  @Getter
  @Setter
  protected static class TranscriptionResponse {
    private String text;
  }
}
