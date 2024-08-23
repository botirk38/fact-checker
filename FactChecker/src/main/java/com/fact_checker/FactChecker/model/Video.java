package com.fact_checker.FactChecker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents a video entity in the fact-checking system.
 * This class is mapped to the "videos" table in the database.
 */
@Entity
@Table(name = "videos")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Video {

    /**
     * The unique identifier for the video.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The name of the video file.
     */
    @Column(name = "file_name", nullable = false)
    private String fileName;

    /**
     * The path where the video file is stored.
     */
    @Column(name = "file_path", nullable = false, unique = true)
    private String filePath;

    /**
     * The transcribed text of the video content.
     * Stored as a Large Object (LOB) in the database.
     */
    @Lob
    @Column(name = "transcription_text", nullable = false, columnDefinition = "LONGTEXT")
    private String transcriptionText;

    /**
     * The date and time when the video was processed.
     */
    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    /**
     * The path where the thumbnail of the video is stored.
     */

    @Column(name="thumbnail_path", nullable = false)
    private String thumbnailPath;
  
  /**
     * The user who uploaded the video.
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;


    /**
     * The fact percentage score of the video after text analysis.
     */
    @Column(name = "fact_percentage")
    private Integer factPercentage;
}
