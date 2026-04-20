package com.example.jobmatch.dto;

import java.util.List;

public class ResumeParseResponse {
  private String resumeId;
  private String fileName;
  private String rawText;
  private List<String> extractedSkills;

  public String getResumeId() {
    return resumeId;
  }

  public void setResumeId(String resumeId) {
    this.resumeId = resumeId;
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
}

