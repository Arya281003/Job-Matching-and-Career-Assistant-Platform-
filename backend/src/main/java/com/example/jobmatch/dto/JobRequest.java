package com.example.jobmatch.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public class JobRequest {
  @NotBlank
  private String title;

  @NotBlank
  private String description;

  private List<String> requiredSkills;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<String> getRequiredSkills() {
    return requiredSkills;
  }

  public void setRequiredSkills(List<String> requiredSkills) {
    this.requiredSkills = requiredSkills;
  }
}
