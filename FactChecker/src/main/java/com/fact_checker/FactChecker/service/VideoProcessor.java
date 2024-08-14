package com.fact_checker.FactChecker.service;

import org.bytedeco.javacv.*;
import org.bytedeco.ffmpeg.global.avcodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.*;
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
    public CompletableFuture<String> extractTextFromSpeech(InputStream videoInputStream) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                byte[] audioData = extractAudioFromVideo(videoInputStream);
                return performSpeechRecognition(audioData);
            } catch (Exception e) {
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
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoInputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(byteArrayOutputStream, 0);

        try {
            grabber.start();

            int sampleRate = grabber.getSampleRate();
            int audioChannels = grabber.getAudioChannels();

            recorder.setAudioCodec(avcodec.AV_CODEC_ID_MP3);
            recorder.setSampleRate(sampleRate);
            recorder.setAudioChannels(audioChannels);
            recorder.setAudioQuality(0);
            recorder.setAudioBitrate(192000);

            recorder.start();

            Frame frame;
            while ((frame = grabber.grab()) != null) {
                if (frame.samples != null) {
                    recorder.record(frame);
                }
            }

            recorder.stop();
            grabber.stop();

            return byteArrayOutputStream.toByteArray();
        } finally {
            grabber.release();
            recorder.release();
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
        body.add("file", new ByteArrayResource(audioData));
        body.add("model", "whisper-1");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<TranscriptionResponse> response = restTemplate.exchange(
                "https://api.openai.com/v1/audio/transcriptions",
                HttpMethod.POST,
                requestEntity,
                TranscriptionResponse.class
        );

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody().getText();
        } else {
            throw new RuntimeException("Failed to transcribe audio: " + response.getStatusCode());
        }
    }

    /**
     * Inner class representing the response from the OpenAI Whisper API.
     */
    protected static class TranscriptionResponse {
        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
