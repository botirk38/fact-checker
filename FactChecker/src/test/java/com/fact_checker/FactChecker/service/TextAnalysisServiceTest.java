package com.fact_checker.FactChecker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TextAnalysisServiceTest {



    @Autowired
    private TextAnalysisService textAnalysisService;


    @Autowired
    private IGroqApiClient apiClient;

    @Value("${groq.api.key}")
    private String apiKey;

    @BeforeEach
    void setup() {
        apiKey = System.getenv("GROQ_API_KEY");
    }

    @Test
    public void testAnalyzeText() {
        // Mock the API responses
        System.out.println("API Key in Test: " + apiKey);
        String text = "The sky is blue. The earth is flat.";
        int result = textAnalysisService.analyzeText(text);
        assertTrue(result >= 0 && result <= 100, "Result was: " + result);
    }


    @Test
    public void testRateClaimsByFacts() {
        System.out.println("API Key in Test: " + apiKey);
        String text = "the sky is blue but some people say it is green";
        StringBuilder sb = new StringBuilder(text);
        StringBuilder statements = textAnalysisService.rateClaimsByFacts(sb);
        System.out.println("Statements: " + statements.toString());
        assertNotNull(statements, "Statements should not be null");
    }

    @Test
    public void testGenerateClaimsSeparatedByAsterisks() {
        String text = "the sky is blue but some people say it is green";
        StringBuilder sb = new StringBuilder(text);
        StringBuilder claims = textAnalysisService.generateClaimsSeparatedByAsterisks(sb);
        System.out.println("Claims: " + claims.toString());
        assertNotNull(claims, "Claims should not be null");
    }

    @Test
    public void testExtractScores() {
        String jsonString = "{\"statement1\": 11.01, \"statement2\": 98, \"statement3\": 54.4}";
        List<Double> scores = textAnalysisService.extractScores(jsonString);
        System.out.println("Scores: " + scores.toString());
        assertNotNull(scores, "Scores should not be null");
        assertFalse(scores.isEmpty(), "Scores should not be empty");
    }

    @Test
    void testSumList() {
        List<Double> list = new ArrayList<>();
        list.add(10.0);
        list.add(20.0);
        list.add(30.0);

        double sum = textAnalysisService.sumList(list);
        assertEquals(60.0, sum, 0.001);
    }


}
