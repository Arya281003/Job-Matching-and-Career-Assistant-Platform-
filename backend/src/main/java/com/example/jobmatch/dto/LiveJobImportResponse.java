package com.example.jobmatch.dto;

public class LiveJobImportResponse {
  private int fetched;
  private int saved;
  private String source;
  private String query;

  public LiveJobImportResponse(int fetched, int saved, String source, String query) {
    this.fetched = fetched;
    this.saved = saved;
    this.source = source;
    this.query = query;
  }

  public int getFetched() {
    return fetched;
  }

  public int getSaved() {
    return saved;
  }

  public String getSource() {
    return source;
  }

  public String getQuery() {
    return query;
  }
}
