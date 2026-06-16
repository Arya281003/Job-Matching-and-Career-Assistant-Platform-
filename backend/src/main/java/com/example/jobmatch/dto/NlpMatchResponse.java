package com.example.jobmatch.dto;

import java.util.List;

public class NlpMatchResponse {
  private List<JobMatch> topMatches;
  private List<String> extractedSkills;
  private List<String> skillGaps;
  private List<String> learningSuggestions;
  private List<String> careerRecommendations;
  private List<String> interviewQuestions;
  private ScoreBreakdown scoreBreakdown;

  public List<JobMatch> getTopMatches() {
    return topMatches;
  }

  public void setTopMatches(List<JobMatch> topMatches) {
    this.topMatches = topMatches;
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

  public ScoreBreakdown getScoreBreakdown() {
    return scoreBreakdown;
  }

  public void setScoreBreakdown(ScoreBreakdown scoreBreakdown) {
    this.scoreBreakdown = scoreBreakdown;
  }
}

