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

/**
 * Repository interface for Video entity operations.
 * Extends JpaRepository to inherit basic CRUD operations.
 */
public interface VideoRepository extends JpaRepository<Video, Long> {

    /**
     * Finds a video by its file name.
     *
     * @param fileName the name of the file to search for
     * @return an Optional containing the Video if found, or empty if not found
     */
    Optional<Video> findByFileName(String fileName);

    /**
     * Finds all videos processed between two given dates.
     *
     * @param start the start date and time
     * @param end the end date and time
     * @return a List of Videos processed within the given time range
     */
    List<Video> findByProcessedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Searches for videos whose transcription text contains the given text, ignoring case.
     *
     * @param text the text to search for in the transcriptions
     * @return a List of Videos whose transcriptions contain the given text
     */
    @Query("SELECT v FROM Video v WHERE LOWER(CAST(v.transcriptionText AS string)) LIKE LOWER(CONCAT('%', :text, '%'))")
    List<Video> findByTranscriptionTextContainingIgnoreCase(@Param("text") String text);

    /**
     * Retrieves the most recently processed videos.
     *
     * @param pageable pagination information
     * @return a List of Videos ordered by processedAt date in descending order
     */
    @Query("SELECT vt FROM Video vt ORDER BY vt.processedAt DESC")
    List<Video> findMostRecentTranscriptions(Pageable pageable);

    /**
     * Deletes all videos processed before a given date.
     *
     * @param date the cut-off date; videos processed before this date will be deleted
     */
    @Modifying
    @Query("DELETE FROM Video vt WHERE vt.processedAt < :date")
    void deleteTranscriptionsOlderThan(@Param("date") LocalDateTime date);
}
