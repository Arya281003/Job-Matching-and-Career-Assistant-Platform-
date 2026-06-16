package com.example.jobmatch.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.jobmatch.dto.MatchResponse;
import com.example.jobmatch.dto.ProfileUpsertRequest;
import com.example.jobmatch.service.MatchingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/matches")
public class MatchingController {

  private final MatchingService matchingService;

  public MatchingController(MatchingService matchingService) {
    this.matchingService = matchingService;
  }

  @PostMapping
  public ResponseEntity<MatchResponse> match(
      @RequestParam("resume") MultipartFile resume,
      @RequestParam(value = "userId", required = false) Long userId) throws Exception {
    if (resume == null || resume.isEmpty()) {
      return ResponseEntity.badRequest().build();
    }

    byte[] bytes = resume.getBytes();
    String fileName = resume.getOriginalFilename() == null ? "resume" : resume.getOriginalFilename();
    return ResponseEntity.ok(matchingService.matchFromFile(bytes, fileName, userId));
  }

  @PostMapping("/profile")
  public ResponseEntity<MatchResponse> matchProfile(@RequestBody @Valid ProfileUpsertRequest request) {
    return ResponseEntity.ok(matchingService.matchFromProfile(request));
  }
}

