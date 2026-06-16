package com.example.jobmatch.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.jobmatch.model.mongo.AnalysisDocument;

@Repository
public interface AnalysisRepository extends MongoRepository<AnalysisDocument, String> {
  List<AnalysisDocument> findByUserIdOrderByCreatedAtDesc(Long userId);
}
