package com.example.jobmatch.model.mongo;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "resumes")
public class ResumeDocument {

  @Id
  private String id;

  private String fileName;
  private String rawText;
  private List<String> extractedSkills;
  private Instant createdAt;

  public String getId() {
    return id;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getRawText() {
    return rawText;
  }

  public void setRawText(String rawText) {
    this.rawText = rawText;
  }

  public List<String> getExtractedSkills() {
    return extractedSkills;
  }

  public void setExtractedSkills(List<String> extractedSkills) {
    this.extractedSkills = extractedSkills;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}

