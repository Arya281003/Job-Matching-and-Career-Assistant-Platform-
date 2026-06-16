package com.example.jobmatch.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.jobmatch.config.AuthInterceptor;
import com.example.jobmatch.model.mongo.AnalysisDocument;
import com.example.jobmatch.repository.AnalysisRepository;

@RestController
@RequestMapping("/api/analyses")
public class AnalysisController {

  private final AnalysisRepository analysisRepository;

  public AnalysisController(AnalysisRepository analysisRepository) {
    this.analysisRepository = analysisRepository;
  }

  @GetMapping
  public List<AnalysisDocument> listMine(@RequestAttribute(AuthInterceptor.USER_ID_ATTRIBUTE) Long userId) {
    return analysisRepository.findByUserIdOrderByCreatedAtDesc(userId);
  }
}
