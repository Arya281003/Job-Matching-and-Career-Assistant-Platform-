package com.example.jobmatch.dto;

public class ScoreBreakdown {
  private double semanticMatch;
  private double skillsMatch;
  private double experienceMatch;

  public double getSemanticMatch() {
    return semanticMatch;
  }

  public void setSemanticMatch(double semanticMatch) {
    this.semanticMatch = semanticMatch;
  }

  public double getSkillsMatch() {
    return skillsMatch;
  }

  public void setSkillsMatch(double skillsMatch) {
    this.skillsMatch = skillsMatch;
  }

  public double getExperienceMatch() {
    return experienceMatch;
  }

  public void setExperienceMatch(double experienceMatch) {
    this.experienceMatch = experienceMatch;
  }
}
