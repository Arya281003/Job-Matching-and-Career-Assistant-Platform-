package com.example.jobmatch.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public class NlpMatchResponse {

  @JsonProperty("topMatches")
  private List<JobMatch> topMatches;

  private List<String> extractedSkills;
  private List<String> skillGaps;
  private List<String> learningSuggestions;
  private List<String> careerRecommendations;
  private List<String> interviewQuestions;

  public List<JobMatch> getTopMatches() { return topMatches; }
  public void setTopMatches(List<JobMatch> topMatches) { this.topMatches = topMatches; }
  public List<String> getExtractedSkills() { return extractedSkills; }
  public void setExtractedSkills(List<String> v) { this.extractedSkills = v; }
  public List<String> getSkillGaps() { return skillGaps; }
  public void setSkillGaps(List<String> v) { this.skillGaps = v; }
  public List<String> getLearningSuggestions() { return learningSuggestions; }
  public void setLearningSuggestions(List<String> v) { this.learningSuggestions = v; }
  public List<String> getCareerRecommendations() { return careerRecommendations; }
  public void setCareerRecommendations(List<String> v) { this.careerRecommendations = v; }
  public List<String> getInterviewQuestions() { return interviewQuestions; }
  public void setInterviewQuestions(List<String> v) { this.interviewQuestions = v; }
}
