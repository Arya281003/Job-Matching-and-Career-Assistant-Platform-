package com.example.jobmatch.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.jobmatch.dto.LiveJobImportResponse;
import com.example.jobmatch.model.mongo.JobDocument;
import com.example.jobmatch.repository.JobRepository;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class LiveJobImportService {

  private static final String SOURCE = "Remotive";
  private static final List<String> KNOWN_SKILLS = List.of(
      "Java", "Spring Boot", "REST", "MySQL", "MongoDB", "JWT", "Security", "Docker",
      "React", "JavaScript", "HTML", "CSS", "Responsive Design", "API Integration", "UI/UX",
      "Python", "NLP", "BERT", "spaCy", "Machine Learning", "Embeddings", "Scikit-learn",
      "Pandas", "NumPy", "MLOps", "SQL", "Excel", "Power BI", "Tableau", "Data Visualization",
      "Statistics", "Kubernetes", "CI/CD", "Linux", "AWS", "Azure", "Cloud Computing",
      "Monitoring", "GitHub Actions", "Testing", "Selenium", "JUnit", "Postman", "API Testing",
      "Cybersecurity", "OWASP", "Database Design", "Indexing", "JPA", "Query Optimization",
      "React Native", "Mobile Development", "Git", "Analytics", "Product Thinking", "AI",
      "Vector Search", "Prompt Engineering", "Troubleshooting", "Networking", "Communication");

  private final JobRepository jobRepository;
  private final RestTemplate restTemplate;
  private final String apiUrl;
  private final String defaultQuery;
  private final int importLimit;
  private final boolean scheduledImportEnabled;

  public LiveJobImportService(
      JobRepository jobRepository,
      RestTemplate restTemplate,
      @Value("${app.jobs.live.api-url:https://remotive.com/api/remote-jobs}") String apiUrl,
      @Value("${app.jobs.live.default-query:software}") String defaultQuery,
      @Value("${app.jobs.live.import-limit:30}") int importLimit,
      @Value("${app.jobs.live.scheduled:false}") boolean scheduledImportEnabled) {
    this.jobRepository = jobRepository;
    this.restTemplate = restTemplate;
    this.apiUrl = apiUrl;
    this.defaultQuery = defaultQuery;
    this.importLimit = importLimit;
    this.scheduledImportEnabled = scheduledImportEnabled;
  }

  public LiveJobImportResponse importLatest(String query) {
    String search = isBlank(query) ? defaultQuery : query.trim();
    URI uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
        .queryParam("search", search)
        .build()
        .toUri();

    JsonNode response = restTemplate.getForObject(uri, JsonNode.class);
    JsonNode jobs = response == null ? null : response.path("jobs");
    if (jobs == null || !jobs.isArray()) {
      return new LiveJobImportResponse(0, 0, SOURCE, search);
    }

    int fetched = 0;
    int saved = 0;
    for (JsonNode node : jobs) {
      if (fetched >= importLimit) {
        break;
      }
      fetched++;
      JobDocument job = toJobDocument(node);
      if (isBlank(job.getExternalId()) || isBlank(job.getTitle())) {
        continue;
      }
      JobDocument existing = jobRepository.findBySourceAndExternalId(SOURCE, job.getExternalId())
          .orElse(null);
      if (existing != null) {
        applyImportedFields(existing, job);
        jobRepository.save(existing);
      } else {
        jobRepository.save(job);
      }
      saved++;
    }

    return new LiveJobImportResponse(fetched, saved, SOURCE, search);
  }

  @Scheduled(fixedDelayString = "${app.jobs.live.refresh-ms:21600000}")
  public void scheduledImportLatest() {
    if (!scheduledImportEnabled) {
      return;
    }
    importLatest(defaultQuery);
  }

  private JobDocument toJobDocument(JsonNode node) {
    JobDocument job = new JobDocument();
    job.setTitle(text(node, "title"));
    job.setCompany(text(node, "company_name"));
    job.setLocation(text(node, "candidate_required_location"));
    job.setSource(SOURCE);
    job.setExternalId(text(node, "id"));
    job.setUrl(text(node, "url"));
    job.setPublishedAt(text(node, "publication_date"));

    String description = cleanDescription(text(node, "description"));
    job.setDescription(description);
    job.setRequiredSkills(extractSkills(job.getTitle() + " " + description));
    return job;
  }

  private void applyImportedFields(JobDocument target, JobDocument source) {
    target.setTitle(source.getTitle());
    target.setCompany(source.getCompany());
    target.setLocation(source.getLocation());
    target.setDescription(source.getDescription());
    target.setRequiredSkills(source.getRequiredSkills());
    target.setUrl(source.getUrl());
    target.setPublishedAt(source.getPublishedAt());
  }

  private List<String> extractSkills(String text) {
    String normalized = normalize(text);
    Set<String> found = new LinkedHashSet<>();
    for (String skill : KNOWN_SKILLS) {
      if (normalized.contains(normalize(skill))) {
        found.add(skill);
      }
    }
    return new ArrayList<>(found);
  }

  private String cleanDescription(String value) {
    return value == null ? "" : value
        .replaceAll("<[^>]+>", " ")
        .replace("&amp;", "&")
        .replace("&nbsp;", " ")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replaceAll("\\s+", " ")
        .trim();
  }

  private String text(JsonNode node, String field) {
    JsonNode value = node.path(field);
    if (value == null || value.isMissingNode() || value.isNull()) {
      return "";
    }
    return value.asText("");
  }

  private String normalize(String value) {
    return (value == null ? "" : value).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9+#.]+", " ").trim();
  }

  private boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
