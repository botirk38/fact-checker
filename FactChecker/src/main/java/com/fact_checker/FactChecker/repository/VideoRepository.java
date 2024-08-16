package com.fact_checker.FactChecker.repository;


import com.fact_checker.FactChecker.model.Video;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VideoRepository extends JpaRepository<Video, Long> {

    Optional<Video> findByFileName(String fileName);

    List<Video> findByProcessedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT v FROM Video v WHERE LOWER(CAST(v.transcriptionText AS string)) LIKE LOWER(CONCAT('%', :text, '%'))")
    List<Video> findByTranscriptionTextContainingIgnoreCase(@Param("text") String text);

    @Query("SELECT vt FROM Video vt ORDER BY vt.processedAt DESC")
    List<Video> findMostRecentTranscriptions(Pageable pageable);

    @Modifying
    @Query("DELETE FROM Video vt WHERE vt.processedAt < :date")
    void deleteTranscriptionsOlderThan(@Param("date") LocalDateTime date);













}