package com.fact_checker.FactChecker.service;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service class for processing video files, extracting audio, and performing speech recognition.
 * This class uses JavaCV for video processing and OpenAI's Whisper API for speech recognition.
 */
@Service
public class VideoProcessor {

    /** ExecutorService for handling asynchronous tasks. */
    private final ExecutorService executorService;

    /** RestTemplate for making HTTP requests. */
    private final RestTemplate restTemplate;

    /** OpenAI API key for authentication. */
    @Value("${openai.api.key}")
    private String openaiApiKey;


    private final static int BIT_RATE = 19200;
    private final static int AUDIO_QUALITY = 0;
    private static final Logger logger = LoggerFactory.getLogger(VideoProcessor.class);

    /**
     * Constructor for VideoProcessor.
     * @param restTemplate RestTemplate bean for making HTTP requests.
     */
    public VideoProcessor(RestTemplate restTemplate) {
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.restTemplate = restTemplate;
    }

    /**
     * Extracts text from speech in a video file asynchronously.
     * @param videoInputStream InputStream of the video file.
     * @return CompletableFuture<String> containing the extracted text.
     */
    public CompletableFuture<Video> extractTextFromSpeech(InputStream videoInputStream, String filename) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                byte[] audioData = extractAudioFromVideo(videoInputStream);
                String transcriptionText =  performSpeechRecognition(audioData);

                Video videoTranscription = new Video();
                videoTranscription.setFileName(filename);
                videoTranscription.setTranscriptionText(transcriptionText);
                videoTranscription.setProcessedAt(LocalDateTime.now());

                return videoTranscription;
            } catch (Exception e) {
                logger.error("Error processing video", e);
                throw new RuntimeException("Error processing video", e);
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
        try(
                FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(tempFile, 0);
                FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoInputStream)

        ) {
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

            // Read the temporary file into a byte array
            byte[] audioData = Files.readAllBytes(tempFile.toPath());

            // Delete the temporary file
            boolean deleted = tempFile.delete();

            if (!deleted) {
                logger.error("Error deleting temporary file");
                throw new RuntimeException("Error deleting temporary file");
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
        headers.set("Authorization", "Bearer " + openaiApiKey);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        // Create a temporary file from the byte array
        try {
            File tempFile = File.createTempFile("audio", ".mp3");
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(audioData);
            fos.close();

            // Add the file to the request
            body.add("file", new FileSystemResource(tempFile));
            body.add("model", "whisper-1");

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<TranscriptionResponse> response = restTemplate.exchange(
                    "https://api.openai.com/v1/audio/transcriptions",
                    HttpMethod.POST,
                    requestEntity,
                    TranscriptionResponse.class
            );

            // Delete the temporary file
            boolean deleted = tempFile.delete();

            if (!deleted) {
                logger.error("Error deleting temporary file");
                throw new RuntimeException("Error deleting temporary file");
            }

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody().getText();
            } else {
                throw new RuntimeException("Failed to transcribe audio: " + response.getStatusCode());
            }
        } catch (IOException e) {
            throw new RuntimeException("Error creating temporary file for audio data", e);
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Error performing speech recognition: " + e.getResponseBodyAsString(), e);
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
