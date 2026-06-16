package com.example.jobmatch.dto;

import java.util.List;

public class NlpMatchRequest {
  private String resumeText;
  private String profileText;
  private List<NlpJobPayload> jobs;

  public String getResumeText() {
    return resumeText;
  }

  public void setResumeText(String resumeText) {
    this.resumeText = resumeText;
  }

  public String getProfileText() {
    return profileText;
  }

  public void setProfileText(String profileText) {
    this.profileText = profileText;
  }

  public List<NlpJobPayload> getJobs() {
    return jobs;
  }

  public void setJobs(List<NlpJobPayload> jobs) {
    this.jobs = jobs;
  }
}

