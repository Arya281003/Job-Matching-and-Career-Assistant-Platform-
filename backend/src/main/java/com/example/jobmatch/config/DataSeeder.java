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

        jobRepository.saveAll(List.of(
            job("Java Backend Developer",
                "Build secure REST APIs using Java, Spring Boot, MySQL, MongoDB, JWT, and cloud-ready backend practices.",
                List.of("Java", "Spring Boot", "REST", "MySQL", "MongoDB", "JWT", "Security", "Docker")),
            job("Frontend Developer (React)",
                "Create responsive React interfaces, reusable components, stateful forms, file upload flows, and polished dashboards.",
                List.of("React", "JavaScript", "HTML", "CSS", "Responsive Design", "API Integration", "UI/UX")),
            job("Full Stack Developer",
                "Develop end-to-end web features across React, Spring Boot, MySQL, MongoDB, authentication, and deployment workflows.",
                List.of("React", "Java", "Spring Boot", "MySQL", "MongoDB", "REST", "Git", "Docker")),
            job("Data Scientist / NLP Engineer",
                "Develop NLP pipelines for resume parsing, skill extraction, semantic matching, embeddings, and model evaluation.",
                List.of("Python", "NLP", "BERT", "spaCy", "Machine Learning", "Embeddings")),
            job("Machine Learning Engineer",
                "Build ML pipelines, train models, evaluate performance, serve predictions, and monitor model quality in production.",
                List.of("Python", "Machine Learning", "Scikit-learn", "Pandas", "NumPy", "Model Evaluation", "MLOps")),
            job("Data Analyst",
                "Analyze business datasets, build dashboards, write SQL queries, and communicate insights through visual reporting.",
                List.of("SQL", "Excel", "Power BI", "Tableau", "Data Visualization", "Statistics", "Python")),
            job("DevOps Engineer",
                "Automate builds, deployments, containerization, monitoring, CI/CD pipelines, and cloud infrastructure operations.",
                List.of("Docker", "Kubernetes", "CI/CD", "Linux", "AWS", "Monitoring", "GitHub Actions")),
            job("Cloud Engineer",
                "Design and maintain cloud services, networking, storage, security, deployments, and scalable application infrastructure.",
                List.of("AWS", "Azure", "Cloud Computing", "Docker", "Kubernetes", "Networking", "Security")),
            job("QA Automation Engineer",
                "Create automated test suites for web APIs and frontend workflows using Selenium, API testing, and CI integration.",
                List.of("Testing", "Selenium", "JUnit", "Postman", "API Testing", "Java", "CI/CD")),
            job("Cybersecurity Analyst",
                "Monitor security risks, analyze vulnerabilities, secure APIs, review authentication flows, and improve application security.",
                List.of("Cybersecurity", "Security", "OWASP", "JWT", "API Security", "Linux", "Networking")),
            job("Database Developer",
                "Design schemas, optimize SQL queries, model relational data, tune indexes, and manage database-backed applications.",
                List.of("MySQL", "SQL", "Database Design", "Indexing", "JPA", "Query Optimization")),
            job("Mobile App Developer",
                "Build mobile applications, integrate APIs, manage app state, create responsive screens, and publish production builds.",
                List.of("React Native", "Mobile Development", "JavaScript", "API Integration", "UI/UX", "Git")),
            job("Product Analyst",
                "Use product data, funnels, experiments, dashboards, and user behavior analysis to guide product decisions.",
                List.of("SQL", "Analytics", "Excel", "Data Visualization", "Statistics", "Product Thinking")),
            job("AI Application Developer",
                "Build AI-powered applications using embeddings, prompt workflows, APIs, vector search, and full-stack integrations.",
                List.of("Python", "AI", "Embeddings", "Vector Search", "React", "API Integration", "Prompt Engineering")),
            job("Technical Support Engineer",
                "Troubleshoot customer issues, analyze logs, reproduce bugs, write SQL checks, and communicate technical solutions.",
                List.of("Troubleshooting", "SQL", "Linux", "Networking", "Communication", "API Testing"))
        ));
      } catch (Exception ignored) {
        // If MongoDB is not running, the app can still start; jobs can be added later.
      }
    };
  }

  private JobDocument job(String title, String description, List<String> requiredSkills) {
    JobDocument job = new JobDocument();
    job.setTitle(title);
    job.setDescription(description);
    job.setRequiredSkills(requiredSkills);
    return job;
  }
}
