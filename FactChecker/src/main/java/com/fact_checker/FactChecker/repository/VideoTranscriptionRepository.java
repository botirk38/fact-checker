package com.fact_checker.FactChecker.repository;


import com.fact_checker.FactChecker.model.VideoTranscription;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VideoTranscriptionRepository extends JpaRepository<VideoTranscription, Long> {

    Optional<VideoTranscription> findByFileName(String fileName);

    List<VideoTranscription> findByProcessedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT v FROM VideoTranscription v WHERE LOWER(CAST(v.transcriptionText AS string)) LIKE LOWER(CONCAT('%', :text, '%'))")
    List<VideoTranscription> findByTranscriptionTextContainingIgnoreCase(@Param("text") String text);

    @Query("SELECT vt FROM VideoTranscription vt ORDER BY vt.processedAt DESC")
    List<VideoTranscription> findMostRecentTranscriptions(Pageable pageable);

    @Modifying
    @Query("DELETE FROM VideoTranscription vt WHERE vt.processedAt < :date")
    void deleteTranscriptionsOlderThan(@Param("date") LocalDateTime date);













}