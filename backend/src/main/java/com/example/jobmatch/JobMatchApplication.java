package com.example.jobmatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JobMatchApplication {
  public static void main(String[] args) {
    SpringApplication.run(JobMatchApplication.class, args);
  }
}

