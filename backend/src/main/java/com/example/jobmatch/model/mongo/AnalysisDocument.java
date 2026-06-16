package com.example.jobmatch.model.mongo;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.example.jobmatch.dto.JobMatch;
import com.example.jobmatch.dto.ScoreBreakdown;

@Document(collection = "resume_analyses")
public class AnalysisDocument {

  @Id
  private String id;

  private Long userId;
  private String resumeId;
  private String fileName;
  private List<JobMatch> matches;
  private List<String> extractedSkills;
  private List<String> skillGaps;
  private List<String> learningSuggestions;
  private List<String> careerRecommendations;
  private List<String> interviewQuestions;
  private ScoreBreakdown scoreBreakdown;
  private Instant createdAt;

  public String getId() {
    return id;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

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

  public ScoreBreakdown getScoreBreakdown() {
    return scoreBreakdown;
  }

  public void setScoreBreakdown(ScoreBreakdown scoreBreakdown) {
    this.scoreBreakdown = scoreBreakdown;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
