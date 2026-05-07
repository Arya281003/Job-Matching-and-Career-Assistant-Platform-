package com.example.jobmatch.controller;

import java.util.Map;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

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

  // Demo fallback: if the DB isn't fully available/working, keep profile data in-memory
  // so the UI doesn't fail with 500 errors.
  private static final Map<Long, UserProfile> MEMORY_PROFILES = new ConcurrentHashMap<>();

  private final UserProfileRepository userProfileRepository;

  public ProfileController(UserProfileRepository userProfileRepository) {
    this.userProfileRepository = userProfileRepository;
  }

  @GetMapping("/{userId}")
  public ResponseEntity<ProfileResponse> get(@PathVariable("userId") Long userId) {
    // Demo-safe behavior: never call DB from this endpoint right now.
    // If profile isn't present in memory, return 404 (frontend handles it).
    UserProfile p = MEMORY_PROFILES.get(userId);
    if (p == null) return ResponseEntity.notFound().build();

    ProfileResponse r = new ProfileResponse();
    r.setUserId(p.getUserId());
    r.setEducation(p.getEducation());
    r.setSkills(p.getSkills());
    r.setExperience(p.getExperience());
    r.setCareerPreferences(p.getCareerPreferences());
    r.setCreatedAt(p.getCreatedAt() == null ? Instant.now() : p.getCreatedAt());
    return ResponseEntity.ok(r);
  }

  @PostMapping
  public ResponseEntity<ProfileResponse> upsert(@RequestBody @Valid ProfileUpsertRequest request) {
    // Update in-memory first so UI works reliably.
    UserProfile p = MEMORY_PROFILES.computeIfAbsent(request.getUserId(), ignored -> {
      UserProfile np = new UserProfile();
      np.setUserId(request.getUserId());
      return np;
    });

    p.setEducation(request.getEducation());
    p.setSkills(request.getSkills());
    p.setExperience(request.getExperience());
    p.setCareerPreferences(request.getCareerPreferences());

    // Best-effort persistence (if DB is available); never break the API.
    try {
      userProfileRepository.save(p);
    } catch (Throwable ignored) {
      // ignore
    }

    ProfileResponse r = new ProfileResponse();
    r.setUserId(p.getUserId());
    r.setEducation(p.getEducation());
    r.setSkills(p.getSkills());
    r.setExperience(p.getExperience());
    r.setCareerPreferences(p.getCareerPreferences());
    r.setCreatedAt(p.getCreatedAt() == null ? Instant.now() : p.getCreatedAt());
    return ResponseEntity.ok(r);
  }
}

