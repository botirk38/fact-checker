package com.fact_checker.FactChecker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class FactCheckerApplication {

  public static void main(String[] args) {
    SpringApplication.run(FactCheckerApplication.class, args);
  }

}
