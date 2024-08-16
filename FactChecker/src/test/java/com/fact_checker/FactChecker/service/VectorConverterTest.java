package com.fact_checker.FactChecker.service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VectorConverterTest {

    private VectorConverter vectorConverter;

    @BeforeEach
    void setUp() {
        vectorConverter = new VectorConverter();
    }

    @Test
    void testTextToVector() {
        Map<String, String> documents = new HashMap<>();
        documents.put("doc1", "This is a test");
        documents.put("doc2", "This is another test");
        vectorConverter.processDocuments(documents);

        Map<String, Double> vector = vectorConverter.textToVector("This is a test document");

        assertNotNull(vector);
        System.out.println(vector);
        assertTrue(vector.containsKey("this"));
        assertTrue(vector.containsKey("is"));
        assertTrue(vector.containsKey("a"));
        assertTrue(vector.containsKey("test"));
        assertTrue(vector.containsKey("document"));
    }


    @Test
    void testProcessDocuments() {
        Map<String, String> documents = new HashMap<>();
        documents.put("doc1", "This is a test");
        documents.put("doc2", "This is another test");

        vectorConverter.processDocuments(documents);

        assertNotNull(vectorConverter.getDocumentVector("doc1"));
        assertNotNull(vectorConverter.getDocumentVector("doc2"));
    }

    @Test
    void testCalculateCosineSimilarity() {
        Map<String, String> documents = new HashMap<>();
        documents.put("doc1", "This is a test");
        documents.put("doc2", "This is another test");
        documents.put("doc3", "Something completely different");

        vectorConverter.processDocuments(documents);

        double similarity12 = vectorConverter.calculateCosineSimilarity(
                vectorConverter.getDocumentVector("doc1"),
                vectorConverter.getDocumentVector("doc2")
        );
        double similarity13 = vectorConverter.calculateCosineSimilarity(
                vectorConverter.getDocumentVector("doc1"),
                vectorConverter.getDocumentVector("doc3")
        );

        assertTrue(similarity12 > similarity13);
        assertTrue(similarity12 > 0 && similarity12 <= 1);
    }
}