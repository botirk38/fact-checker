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

@Service
public class VideoProcessor {

    private final ExecutorService executorService;

    private final RestTemplate restTemplate;

    @Value("${openai.api.key}")
    private String openaiApiKey;

    public VideoProcessor(RestTemplate restTemplate) {
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.restTemplate = restTemplate;
    }

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

    String performSpeechRecognition(byte[] audioData){
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
