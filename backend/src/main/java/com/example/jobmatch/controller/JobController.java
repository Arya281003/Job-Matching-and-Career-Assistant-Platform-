package com.example.jobmatch.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.jobmatch.dto.LiveJobImportResponse;
import com.example.jobmatch.dto.JobRequest;
import com.example.jobmatch.model.mongo.JobDocument;
import com.example.jobmatch.repository.JobRepository;
import com.example.jobmatch.service.LiveJobImportService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

  private final JobRepository jobRepository;
  private final LiveJobImportService liveJobImportService;

  public JobController(JobRepository jobRepository, LiveJobImportService liveJobImportService) {
    this.jobRepository = jobRepository;
    this.liveJobImportService = liveJobImportService;
  }

  @GetMapping
  public List<JobDocument> list() {
    return jobRepository.findAll();
  }

  @PostMapping
  public JobDocument create(@RequestBody @Valid JobRequest request) {
    JobDocument job = new JobDocument();
    apply(job, request);
    return jobRepository.save(job);
  }

  @PostMapping("/import/live")
  public LiveJobImportResponse importLive(@RequestParam(required = false) String query) {
    return liveJobImportService.importLatest(query);
  }

  @PutMapping("/{id}")
  public ResponseEntity<JobDocument> update(@PathVariable String id, @RequestBody @Valid JobRequest request) {
    return jobRepository.findById(id)
        .map(job -> {
          apply(job, request);
          return ResponseEntity.ok(jobRepository.save(job));
        })
        .orElse(ResponseEntity.notFound().build());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable String id) {
    if (!jobRepository.existsById(id)) {
      return ResponseEntity.notFound().build();
    }
    jobRepository.deleteById(id);
    return ResponseEntity.noContent().build();
  }

  private void apply(JobDocument job, JobRequest request) {
    job.setTitle(request.getTitle());
    job.setDescription(request.getDescription());
    job.setRequiredSkills(request.getRequiredSkills() == null ? List.of() : request.getRequiredSkills());
  }
}
