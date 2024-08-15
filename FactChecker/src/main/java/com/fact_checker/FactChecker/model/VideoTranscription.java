package com.fact_checker.FactChecker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "video_transcriptions")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class VideoTranscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name")
    private String fileName;

    @Lob
    @Column(name = "transcription_text")
    private String transcriptionText;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;


}
