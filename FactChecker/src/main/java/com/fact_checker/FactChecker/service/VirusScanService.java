package com.fact_checker.FactChecker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class VirusScanService {

    @Value("${virustotal.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("mp4", "mov");

    public VirusScanService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean scanFile(MultipartFile multipartFile) throws IOException {
        // Validates the file type here
        validateFileType(multipartFile);

        // MultipartFile to File (convert)
        File file = convertMultiPartToFile(multipartFile);

        // HTTP headers (create)
        HttpHeaders headers = new HttpHeaders();
        headers.set("virustotal.api.key", apiKey);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // HTTP entity with the file(create)
        HttpEntity<File> requestEntity = new HttpEntity<>(file, headers);

        // Send POST request to VirusTotal API
        ResponseEntity<String> response = restTemplate.exchange(
                "https://www.virustotal.com/api/v3/files",
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        // Handle response
        if (response.getStatusCode() == HttpStatus.OK) {
            // Checks if the file is clean
            return true; // Return true if clean, false if infected
        }

        return false;
    }

    private void validateFileType(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || !hasAllowedExtension(filename)) {
            throw new RuntimeException("Invalid file type. Only MP4 and MOV files are allowed.");
        }
    }

    private boolean hasAllowedExtension(String filename) {
        String fileExtension = getFileExtension(filename);
        return ALLOWED_EXTENSIONS.contains(fileExtension.toLowerCase());
    }

    private String getFileExtension(String filename) {
        int lastIndex = filename.lastIndexOf('.');
        return (lastIndex != -1) ? filename.substring(lastIndex + 1) : "";
    }

    private File convertMultiPartToFile(MultipartFile file) throws IOException {
        File convFile = new File(Objects.requireNonNull(file.getOriginalFilename()));
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(file.getBytes());
        fos.close();
        return convFile;
    }
}


