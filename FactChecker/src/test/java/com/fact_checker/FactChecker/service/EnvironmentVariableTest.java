package com.fact_checker.FactChecker.service;

import java.io.IOException;

public class EnvironmentVariableTest {
    public static void main(String[] args) throws InterruptedException, IOException {
        // Print environment variable directly
        System.out.println("GROQ_API_KEY from System.getenv(): " + System.getenv("$GROQ_API_KEY"));

        // Use ProcessBuilder to echo the environment variable
        ProcessBuilder pb = new ProcessBuilder("echo", "$GROQ_API_KEY");
        pb.inheritIO();
        Process p = pb.start();
        p.waitFor();

        // Print all environment variables
        System.getenv().forEach((key, value) -> System.out.println(key + "=" + value));
    }
}
