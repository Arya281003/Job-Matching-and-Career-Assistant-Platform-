package com.example.jobmatch.controller;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.jobmatch.dto.ProfileResponse;
import com.example.jobmatch.dto.ProfileUpsertRequest;
import com.example.jobmatch.model.UserProfile;
import com.example.jobmatch.repository.UserProfileRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
  private final UserProfileRepository userProfileRepository;

  public ProfileController(UserProfileRepository userProfileRepository) {
    this.userProfileRepository = userProfileRepository;
  }

  @GetMapping("/{userId}")
  public ResponseEntity<ProfileResponse> get(@PathVariable("userId") Long userId) {
    return userProfileRepository.findByUserId(userId)
        .map(profile -> ResponseEntity.ok(toResponse(profile)))
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  public ResponseEntity<ProfileResponse> upsert(@RequestBody @Valid ProfileUpsertRequest request) {
    UserProfile p = userProfileRepository.findByUserId(request.getUserId()).orElseGet(() -> {
      UserProfile profile = new UserProfile();
      profile.setUserId(request.getUserId());
      return profile;
    });

    p.setEducation(request.getEducation());
    p.setSkills(request.getSkills());
    p.setExperience(request.getExperience());
    p.setCareerPreferences(request.getCareerPreferences());

    return ResponseEntity.ok(toResponse(userProfileRepository.save(p)));
  }

  private ProfileResponse toResponse(UserProfile p) {
    ProfileResponse r = new ProfileResponse();
    r.setUserId(p.getUserId());
    r.setEducation(p.getEducation());
    r.setSkills(p.getSkills());
    r.setExperience(p.getExperience());
    r.setCareerPreferences(p.getCareerPreferences());
    r.setCreatedAt(p.getCreatedAt() == null ? Instant.now() : p.getCreatedAt());
    return r;
  }
}
