package com.fact_checker.FactChecker.service;

import com.fact_checker.FactChecker.exceptions.OpenAiException;
import com.fact_checker.FactChecker.exceptions.VideoProcessingException;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.Setter;
import org.bytedeco.javacv.*;
import org.bytedeco.ffmpeg.global.avcodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.fact_checker.FactChecker.model.Video;
import com.fact_checker.FactChecker.config.OpenAIConfig;
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
 * Service class for processing video files, extracting audio, and performing speech recognition.
 * This class uses JavaCV for video processing and OpenAI's Whisper API for speech recognition.
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
     */
    public VideoProcessor(RestTemplate restTemplate, OpenAIConfig openAiConfig,
                          @Value("${thumbnail.upload.path}") String thumbnailUploadPath) {
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.restTemplate = restTemplate;
        this.openAiConfig = openAiConfig;
        this.thumbnailUploadPath = thumbnailUploadPath;

        Path thumbnailUploadPathDir = Paths.get(thumbnailUploadPath);


        if (!Files.exists(thumbnailUploadPathDir)) {
            try {
                Files.createDirectories(thumbnailUploadPathDir);
            } catch (IOException e) {
                throw new RuntimeException("Could not create thumbnail upload path: " + thumbnailUploadPath, e);
            }
            logger.info("Thumbnail upload directory created: {}", thumbnailUploadPath);
        }

        if(!Files.isWritable(thumbnailUploadPathDir)) {
            throw new RuntimeException("Thumbnail upload path is not writable: " + thumbnailUploadPath);
        }
    }

    /**
     * Extracts text from speech in a video file asynchronously.
     * @param videoInputStream InputStream of the video file.
     * @param filename Name of the video file.
     * @return CompletableFuture<Video> containing the processed video information.
     */
    public CompletableFuture<Video> extractTextFromSpeech(InputStream videoInputStream, String filename) {
        return CompletableFuture.supplyAsync(() -> {
            File tempFile = null;
            try {
                // Save the input stream to a temporary file
                tempFile = File.createTempFile("video", ".tmp");
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = videoInputStream.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }

                // Use the temporary file to create new input streams for each operation
                byte[] audioData = extractAudioFromVideo(new FileInputStream(tempFile));
                String transcriptionText = performSpeechRecognition(audioData);
                String thumbnailPath = extractThumbnail(new FileInputStream(tempFile));

                Video video = new Video();
                video.setFileName(filename);
                video.setTranscriptionText(transcriptionText);
                video.setThumbnailPath(thumbnailPath);
                video.setProcessedAt(LocalDateTime.now());

                return video;

            } catch (Exception e) {
                logger.error("Error processing video", e);
                throw new VideoProcessingException("Error processing video", e);
            } finally {
                // Clean up the temporary file
                if (tempFile != null && tempFile.exists()) {
                    if (!tempFile.delete()) {
                        logger.warn("Failed to delete temporary video file");
                    }
                }
            }
        }, executorService);
    }
    /**
     * Extracts audio from a video file.
     * @param videoInputStream InputStream of the video file.
     * @return byte array containing the extracted audio data.
     * @throws IOException if there's an error processing the video.
     */
    byte[] extractAudioFromVideo(InputStream videoInputStream) throws IOException {
        File tempFile = File.createTempFile("audio", ".mp3");
        try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(tempFile, 0);
             FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoInputStream)) {

            grabber.start();

            int sampleRate = grabber.getSampleRate();
            int audioChannels = grabber.getAudioChannels();

            recorder.setAudioCodec(avcodec.AV_CODEC_ID_MP3);
            recorder.setSampleRate(sampleRate);
            recorder.setAudioChannels(audioChannels);
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
                logger.warn("Failed to delete temporary audio file");
            }

            return audioData;
        }
    }

    /**
     * Performs speech recognition on the given audio data using OpenAI's Whisper API.
     * @param audioData byte array containing the audio data.
     * @return String containing the transcribed text.
     */
    String performSpeechRecognition(byte[] audioData) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Authorization", "Bearer " + openAiConfig.getApiKey());

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
                    TranscriptionResponse.class
            );

            if (!tempFile.delete()) {
                logger.warn("Failed to delete temporary speech file");
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
     * Extracts a thumbnail from the video.
     * @param videoInputStream InputStream of the video file.
     * @return String path of the extracted thumbnail.
     * @throws IOException if there's an error processing the video.
     */
    String extractThumbnail(InputStream videoInputStream) throws IOException {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoInputStream);
             Java2DFrameConverter converter = new Java2DFrameConverter()) {

            grabber.start();
            grabber.setTimestamp(1000000); // Set to 1 second

            Frame frame = grabber.grab();
            if (frame == null) {
                throw new VideoProcessingException("Error extracting thumbnail from video", null);
            }

            BufferedImage bufferedImage = converter.getBufferedImage(frame);

            String thumbnailFileName = UUID.randomUUID() + ".png";
            Path thumbnailPath = Paths.get(thumbnailUploadPath, thumbnailFileName);


            // Use FileOutputStream to write the image
            try (FileOutputStream fos = new FileOutputStream(thumbnailPath.toFile())) {
                if (!ImageIO.write(bufferedImage, "png", fos)) {
                    throw new IOException("Could not write image file");
                }
            }

            return thumbnailFileName;
        }
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    /**
     * Inner class representing the response from the OpenAI Whisper API.
     */
    @Getter
    @Setter
    protected static class TranscriptionResponse {
        private String text;
    }



}
