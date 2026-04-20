package com.example.jobmatch.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.jobmatch.dto.ResumeParseResponse;
import com.example.jobmatch.service.MatchingService;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

  private final MatchingService matchingService;

  public ResumeController(MatchingService matchingService) {
    this.matchingService = matchingService;
  }

  @PostMapping("/parse")
  public ResponseEntity<ResumeParseResponse> parse(@RequestParam("resume") MultipartFile resume) throws Exception {
    if (resume == null || resume.isEmpty()) {
      return ResponseEntity.badRequest().build();
    }

    byte[] bytes = resume.getBytes();
    String fileName = resume.getOriginalFilename() == null ? "resume" : resume.getOriginalFilename();
    return ResponseEntity.ok(matchingService.parseAndStoreResume(bytes, fileName));
  }
}

