package com.example.jobmatch.dto;

import java.util.List;

public class NlpExtractResponse {
  private String text;
  private List<String> skills;

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public List<String> getSkills() {
    return skills;
  }

  public void setSkills(List<String> skills) {
    this.skills = skills;
  }
}

