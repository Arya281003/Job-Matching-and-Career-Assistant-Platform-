package com.example.jobmatch.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.jobmatch.model.mongo.JobDocument;

@Repository
public interface JobRepository extends MongoRepository<JobDocument, String> {
  Optional<JobDocument> findBySourceAndExternalId(String source, String externalId);
}

