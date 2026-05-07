package com.example.jobmatch.dto;

import java.util.List;

public class MatchResponse {
  private String resumeId;
  private List<JobMatch> matches;
  private List<String> extractedSkills;
  private List<String> skillGaps;
  private List<String> learningSuggestions;
  private List<String> careerRecommendations;
  private List<String> interviewQuestions;

  public String getResumeId() {
    return resumeId;
  }

  public void setResumeId(String resumeId) {
    this.resumeId = resumeId;
  }

  public List<JobMatch> getMatches() {
    return matches;
  }

  public void setMatches(List<JobMatch> matches) {
    this.matches = matches;
  }

  public List<String> getExtractedSkills() {
    return extractedSkills;
  }

  public void setExtractedSkills(List<String> extractedSkills) {
    this.extractedSkills = extractedSkills;
  }

  public List<String> getSkillGaps() {
    return skillGaps;
  }

  public void setSkillGaps(List<String> skillGaps) {
    this.skillGaps = skillGaps;
  }

  public List<String> getLearningSuggestions() {
    return learningSuggestions;
  }

  public void setLearningSuggestions(List<String> learningSuggestions) {
    this.learningSuggestions = learningSuggestions;
  }

  public List<String> getCareerRecommendations() {
    return careerRecommendations;
  }

  public void setCareerRecommendations(List<String> careerRecommendations) {
    this.careerRecommendations = careerRecommendations;
  }

  public List<String> getInterviewQuestions() {
    return interviewQuestions;
  }

  public void setInterviewQuestions(List<String> interviewQuestions) {
    this.interviewQuestions = interviewQuestions;
  }
}

