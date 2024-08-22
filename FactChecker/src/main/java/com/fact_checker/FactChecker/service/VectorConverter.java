package com.fact_checker.FactChecker.service;
import java.util.*;

/**
 * VectorConverter class for converting text to vectors and calculating similarities.
 * This class implements TF-IDF (Term Frequency-Inverse Document Frequency) vectorization
 * and cosine similarity calculation for text documents.
 */
public class VectorConverter {
    private final Map<String, Double> idf;
    private final Map<String, Map<String, Double>> documentVectors;

    /**
     * Constructor for VectorConverter.
     * Initializes the IDF map and document vectors map.
     */
    public VectorConverter() {
        this.idf = new HashMap<>();
        this.documentVectors = new HashMap<>();
    }

    /**
     * Converts a given text to a TF-IDF vector.
     * @param text The input text to be converted.
     * @return A map representing the TF-IDF vector of the input text.
     */
    public Map<String, Double> textToVector(String text) {
        Map<String, Double> vector = new HashMap<>();
        String[] words = text.toLowerCase().split("\\s+");
        int totalWords = words.length;

        // Calculate term frequency (TF)
        Map<String, Integer> wordCount = new HashMap<>();
        for (String word : words) {
            wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
        }

        // Calculate TF-IDF
        for (Map.Entry<String, Integer> entry : wordCount.entrySet()) {
            String word = entry.getKey();
            double tf = (double) entry.getValue() / totalWords;
            double idfValue = idf.getOrDefault(word, 0.0);
            vector.put(word, tf * idfValue);
        }

        return vector;
    }

    /**
     * Processes a set of documents to calculate IDF and document vectors.
     * @param documents A map of document IDs to their text content.
     */
    public void processDocuments(Map<String, String> documents) {
        // Calculate IDF
        Map<String, Integer> wordDocumentCount = new HashMap<>();
        int totalDocuments = documents.size();

        for (String document : documents.values()) {
            Set<String> uniqueWords = new HashSet<>(Arrays.asList(document.toLowerCase().split("\\s+")));
            for (String word : uniqueWords) {
                wordDocumentCount.put(word, wordDocumentCount.getOrDefault(word, 0) + 1);
            }
        }

        for (Map.Entry<String, Integer> entry : wordDocumentCount.entrySet()) {
            double idfValue = Math.log((double) totalDocuments / entry.getValue());
            idf.put(entry.getKey(), idfValue);
        }

        // Calculate vectors for each document
        for (Map.Entry<String, String> entry : documents.entrySet()) {
            String docId = entry.getKey();
            String text = entry.getValue();
            documentVectors.put(docId, textToVector(text));
        }
    }

    /**
     * Retrieves the vector for a specific document.
     * @param docId The ID of the document.
     * @return The TF-IDF vector of the specified document.
     */
    public Map<String, Double> getDocumentVector(String docId) {
        return documentVectors.get(docId);
    }

    /**
     * Calculates the cosine similarity between two vectors.
     * @param vector1 The first vector.
     * @param vector2 The second vector.
     * @return The cosine similarity between the two vectors.
     */
    public double calculateCosineSimilarity(Map<String, Double> vector1, Map<String, Double> vector2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (Map.Entry<String, Double> entry : vector1.entrySet()) {
            String key = entry.getKey();
            double value1 = entry.getValue();
            double value2 = vector2.getOrDefault(key, 0.0);

            dotProduct += value1 * value2;
            norm1 += Math.pow(value1, 2);
        }

        for (double value : vector2.values()) {
            norm2 += Math.pow(value, 2);
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
