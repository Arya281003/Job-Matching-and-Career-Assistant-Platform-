package com.example.jobmatch.dto;

public class JobMatch {
  private String title;
  private double score;

  public JobMatch() {}

  public JobMatch(String title, double score) {
    this.title = title;
    this.score = score;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public double getScore() {
    return score;
  }

  public void setScore(double score) {
    this.score = score;
  }
}

