package com.fact_checker.FactChecker.service;

import com.fact_checker.FactChecker.model.User;
import com.fact_checker.FactChecker.model.Video;
import com.fact_checker.FactChecker.repository.VideoRepository;
import com.fact_checker.FactChecker.exceptions.FileProcessingException;
import com.fact_checker.FactChecker.exceptions.InvalidFileException;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Service class for handling video processing operations.
 * This class is responsible for validating, saving, and processing video files.
 */
@Service
public class VideoService {
  private static final Logger logger = LoggerFactory.getLogger(VideoService.class);
  private static final List<String> SUPPORTED_FILE_EXTENSIONS = Arrays.asList("mp4", "mov", "avi", "wmv", "flv");

  private final VideoRepository videoRepository;
  private final VideoProcessor videoProcessor;
  private final String uploadPath;

  /**
   * Constructs a new VideoService.
   *
   * @param videoRepository Repository for Video entities
   * @param videoProcessor  Processor for video files
   * @param uploadPath      Path where uploaded files will be stored
   * @throws RuntimeException if the upload directory cannot be created or is not
   *                          writable
   */
  public VideoService(VideoRepository videoRepository, VideoProcessor videoProcessor,
      @Value("${video.upload.path}") String uploadPath) {
    this.videoRepository = videoRepository;
    this.videoProcessor = videoProcessor;
    this.uploadPath = uploadPath;

    // Check upload directory
    Path uploadDir = Paths.get(uploadPath);
    if (!Files.exists(uploadDir)) {
      try {
        Files.createDirectories(uploadDir);
        logger.info("Upload directory created: {}", uploadPath);
      } catch (IOException e) {
        logger.error("Failed to create upload directory: {}", e.getMessage());
        throw new RuntimeException("Unable to create upload directory", e);
      }
    }

    if (!Files.isWritable(uploadDir)) {
      logger.error("Upload directory is not writable: {}", uploadPath);
      throw new RuntimeException("Upload directory is not writable");
    }

    logger.info("Upload directory is ready: {}", uploadPath);
  }

  /**
   * Processes and saves a video file asynchronously.
   *
   * @param file The MultipartFile representing the video to be processed
   * @return A CompletableFuture that will contain the processed Video entity
   * @throws InvalidFileException    if the file is empty or has an unsupported
   *                                 extension
   * @throws FileProcessingException if there's an error during file processing
   */
  @Transactional
  public CompletableFuture<Video> processAndSaveVideo(MultipartFile file, User user) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        validateFile(file);
        String filename = saveFile(file);
        Video video = processVideo(filename);
        video.setUser(user);
        return videoRepository.save(video);
      } catch (InvalidFileException | FileProcessingException e) {
        logger.error("File is invalid: {}", file.getOriginalFilename());
        throw e;
      }
    });
  }

  /**
   * Saves the uploaded file to the file system.
   *
   * @param file The MultipartFile to be saved
   * @return The filename of the saved file
   * @throws InvalidFileException    if the file has no extension
   * @throws FileProcessingException if there's an error while saving the file
   */
  private String saveFile(MultipartFile file) throws InvalidFileException {
    String fileExtension = getFileExtension(file)
        .orElseThrow(() -> new InvalidFileException("File has no extension"));

    String randomFilename = UUID.randomUUID() + "." + fileExtension;
    Path filePath = Paths.get(uploadPath, randomFilename);

    try {
      Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
      logger.info("File saved successfully: {}", randomFilename);
      return randomFilename;
    } catch (IOException e) {
      logger.error("Error while saving file: {}", e.getMessage());
      throw new FileProcessingException("Failed to save file", e);
    }
  }

  /**
   * Validates the uploaded file.
   *
   * @param file The MultipartFile to be validated
   * @throws InvalidFileException if the file is empty or has an unsupported
   *                              extension
   */
  private void validateFile(MultipartFile file) throws InvalidFileException {
    if (file.isEmpty()) {
      throw new InvalidFileException("File is empty");
    }

    getFileExtension(file)
        .filter(SUPPORTED_FILE_EXTENSIONS::contains)
        .orElseThrow(() -> new InvalidFileException("File extension is not supported"));
  }

  /**
   * Extracts the file extension from the MultipartFile.
   *
   * @param file The MultipartFile to extract the extension from
   * @return An Optional containing the file extension, or empty if no extension
   *         is found
   */
  private Optional<String> getFileExtension(MultipartFile file) {
    return Optional.ofNullable(file.getOriginalFilename())
        .filter(f -> !f.isEmpty())
        .map(FilenameUtils::getExtension);
  }

  /**
   * Processes the video file to extract text from speech.
   *
   * @param filename The name of the file to process
   * @return The processed Video entity
   * @throws FileProcessingException if there's an error during video processing
   */
  private Video processVideo(String filename) {
    Path filePath = Paths.get(uploadPath, filename);
    try {
      Video video = videoProcessor.extractTextFromSpeech(filePath, filename)
          .exceptionally(ex -> {
            logger.error("Error processing video: {}", ex.getMessage());
            throw new FileProcessingException("Failed to process video", ex);
          })
          .join();

      video.setFilePath(filePath.toString());
      return video;
    } catch (Exception e) {
      logger.error("Error reading file: {}", e.getMessage());
      throw new FileProcessingException("Failed to read video file", e);
    }
  }

  public List<Video> getAllVideos() {
    return videoRepository.findAll();
  }


  public Video getVideo(Long id) {
        return videoRepository.findById(id).orElseThrow(() -> new RuntimeException("Video not found"));
    }
}
