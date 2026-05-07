package com.example.jobmatch.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.jobmatch.model.mongo.JobDocument;
import com.example.jobmatch.repository.JobRepository;

@Configuration
public class DataSeeder {

  @Bean
  public CommandLineRunner seedJobs(JobRepository jobRepository) {
    return args -> {
      try {
        if (jobRepository.count() > 0) {
          return;
        }

        JobDocument javaDev = new JobDocument();
        javaDev.setTitle("Java Backend Developer");
        javaDev.setDescription(
            "Build REST APIs using Java and Spring Boot. Strong knowledge of Java, REST, MySQL, MongoDB, and application security. " +
            "Experience with NLP/AI is a plus.");
        javaDev.setRequiredSkills(List.of(
            "Java", "Spring Boot", "REST", "MySQL", "MongoDB", "Security"));

        JobDocument frontend = new JobDocument();
        frontend.setTitle("Frontend Developer (React)");
        frontend.setDescription(
            "Create responsive user interfaces with React. Build components, manage state, and integrate with backend APIs. " +
            "Strong HTML/CSS fundamentals.");
        frontend.setRequiredSkills(List.of(
            "React", "HTML", "CSS", "JavaScript", "API Integration"));

        JobDocument dataScientist = new JobDocument();
        dataScientist.setTitle("Data Scientist / NLP Engineer");
        dataScientist.setDescription(
            "Develop NLP pipelines for resume parsing and job recommendation. Use BERT-style embeddings and spaCy for entity extraction. " +
            "Implement similarity scoring and skill gap analysis.");
        dataScientist.setRequiredSkills(List.of(
            "Python", "NLP", "BERT", "spaCy", "Resume Parsing", "Machine Learning"));

        jobRepository.saveAll(List.of(javaDev, frontend, dataScientist));
      } catch (Exception ignored) {
        // Demo mode: if MongoDB isn't running, skip seeding.
      }
    };
  }
}

