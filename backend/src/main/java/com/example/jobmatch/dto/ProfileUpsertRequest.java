package com.example.jobmatch.dto;

import jakarta.validation.constraints.NotNull;

public class ProfileUpsertRequest {
  @NotNull
  private Long userId;

  private String education;
  private String skills;
  private String experience;
  private String careerPreferences;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getEducation() {
    return education;
  }

  public void setEducation(String education) {
    this.education = education;
  }

  public String getSkills() {
    return skills;
  }

  public void setSkills(String skills) {
    this.skills = skills;
  }

  public String getExperience() {
    return experience;
  }

  public void setExperience(String experience) {
    this.experience = experience;
  }

  public String getCareerPreferences() {
    return careerPreferences;
  }

  public void setCareerPreferences(String careerPreferences) {
    this.careerPreferences = careerPreferences;
  }
}

