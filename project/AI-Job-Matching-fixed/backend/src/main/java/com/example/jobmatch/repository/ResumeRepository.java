package com.example.jobmatch.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.jobmatch.model.mongo.ResumeDocument;

@Repository
public interface ResumeRepository extends MongoRepository<ResumeDocument, String> {}

