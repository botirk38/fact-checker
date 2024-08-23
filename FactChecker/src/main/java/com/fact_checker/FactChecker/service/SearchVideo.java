package com.fact_checker.FactChecker.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchVideo {
    static VectorConverter vectorConverter=new VectorConverter();


    public List<String> search(String keyword){
        Map<String, Double> keywordVector=vectorConverter.textToVector(keyword);

        //CosSimilarity scores Keyword==>ALL documents:
        Map<String, Double> similarityScores = new HashMap<>();
        for (String docId : vectorConverter.documentVectors.keySet()) {
            Map<String, Double> documentVector = vectorConverter.getDocumentVector(docId);
            double similarity = vectorConverter.calculateCosineSimilarity(keywordVector, documentVector);
            similarityScores.put(docId, similarity);
        }

        //Sort
        List<Map.Entry<String, Double>> sortedEntries = new ArrayList<>(similarityScores.entrySet());
        sortedEntries.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        // Retrieve the top 3 documents
        List<String> top3Results = new ArrayList<>();
        for (int i = 0; i < Math.min(3, sortedEntries.size()); i++) {
            top3Results.add(sortedEntries.get(i).getKey());
        }

        return top3Results;
    }
    public static void main(String[] args){
        // Example usage
        Map<String, String> documents = new HashMap<>();
        documents.put("doc1", "the cat is on the mat");
        documents.put("doc2", "the dog is on the mat");
        documents.put("doc3", "the cat and the dog are friends");
        documents.put("doc4", "the dog loves to play on the mat");

        SearchVideo searchEngine = new SearchVideo();
        vectorConverter.processDocuments(documents);

        // Perform a search
        String keyword = "cat mat";
        List<String> top3Results = searchEngine.search(keyword);

        // Print the top 3 results
        System.out.println("Top 3 results for keyword '" + keyword + "':");
        for (String docId : top3Results) {
            System.out.println(docId);
        }
    }
}
