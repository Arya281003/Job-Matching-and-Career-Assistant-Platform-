package com.example.jobmatch.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.jobmatch.dto.MatchResponse;
import com.example.jobmatch.dto.NlpExtractResponse;
import com.example.jobmatch.dto.NlpJobPayload;
import com.example.jobmatch.dto.NlpMatchResponse;
import com.example.jobmatch.dto.ResumeParseResponse;
import com.example.jobmatch.model.mongo.JobDocument;
import com.example.jobmatch.model.mongo.ResumeDocument;
import com.example.jobmatch.repository.JobRepository;
import com.example.jobmatch.repository.ResumeRepository;

@Service
public class MatchingService {

  private final JobRepository jobRepository;
  private final ResumeRepository resumeRepository;
  private final NlpClientService nlpClientService;

  public MatchingService(JobRepository jobRepository, ResumeRepository resumeRepository, NlpClientService nlpClientService) {
    this.jobRepository = jobRepository;
    this.resumeRepository = resumeRepository;
    this.nlpClientService = nlpClientService;
  }

  private List<JobDocument> demoJobs() {
    JobDocument javaDev = new JobDocument();
    javaDev.setTitle("Java Backend Developer");
    javaDev.setDescription(
        "Build REST APIs using Java and Spring Boot. Strong knowledge of Java, REST, MySQL, MongoDB, and application security. " +
        "Experience with NLP/AI is a plus.");
    javaDev.setRequiredSkills(List.of(
        "Java", "Spring Boot", "REST", "MySQL", "MongoDB", "Security"));

    JobDocument frontend = new JobDocument();
    frontend.setTitle("Frontend Developer (React)");
    frontend.setDescription(
        "Create responsive user interfaces with React. Build components, manage state, and integrate with backend APIs. " +
        "Strong HTML/CSS fundamentals.");
    frontend.setRequiredSkills(List.of(
        "React", "HTML", "CSS", "JavaScript", "API Integration"));

    JobDocument dataScientist = new JobDocument();
    dataScientist.setTitle("Data Scientist / NLP Engineer");
    dataScientist.setDescription(
        "Develop NLP pipelines for resume parsing and job recommendation. Use BERT-style embeddings and spaCy for entity extraction. " +
        "Implement similarity scoring and skill gap analysis.");
    dataScientist.setRequiredSkills(List.of(
        "Python", "NLP", "BERT", "spaCy", "Resume Parsing", "Machine Learning"));

    return List.of(javaDev, frontend, dataScientist);
  }

  public ResumeParseResponse parseAndStoreResume(byte[] fileBytes, String fileName) {
    NlpExtractResponse extract = nlpClientService.extractSkills(fileBytes, fileName);

    ResumeDocument resume = new ResumeDocument();
    resume.setFileName(fileName);
    resume.setRawText(extract.getText());
    resume.setExtractedSkills(extract.getSkills());
    resume.setCreatedAt(Instant.now());

    try {
      resumeRepository.save(resume);
    } catch (Exception ignored) {
      // Demo mode: if MongoDB isn't running, still return extracted output.
      // We'll just keep resumeId as null and generate one for the response.
    }

    ResumeParseResponse out = new ResumeParseResponse();
    out.setResumeId(resume.getId() == null ? "demo-" + java.util.UUID.randomUUID() : resume.getId());
    out.setFileName(resume.getFileName());
    out.setRawText(resume.getRawText());
    out.setExtractedSkills(resume.getExtractedSkills());
    return out;
  }

  public MatchResponse matchFromFile(byte[] fileBytes, String fileName) {
    NlpExtractResponse extract = nlpClientService.extractSkills(fileBytes, fileName);

    ResumeDocument resume = new ResumeDocument();
    resume.setFileName(fileName);
    resume.setRawText(extract.getText());
    resume.setExtractedSkills(extract.getSkills());
    resume.setCreatedAt(Instant.now());
    try {
      resumeRepository.save(resume);
    } catch (Exception ignored) {
      // Demo mode: continue even if Mongo isn't reachable.
    }

    List<JobDocument> jobs;
    try {
      jobs = jobRepository.findAll();
      if (jobs == null || jobs.isEmpty()) {
        jobs = demoJobs();
      }
    } catch (Exception ignored) {
      jobs = demoJobs();
    }
    List<NlpJobPayload> jobPayloads = jobs.stream().map(j -> {
      NlpJobPayload p = new NlpJobPayload();
      p.setTitle(j.getTitle());
      p.setDescription(j.getDescription());
      p.setRequiredSkills(j.getRequiredSkills());
      return p;
    }).toList();

    NlpMatchResponse nlpResp = nlpClientService.matchJobs(extract.getText(), jobPayloads);

    MatchResponse out = new MatchResponse();
    out.setResumeId(resume.getId() == null ? "demo-" + java.util.UUID.randomUUID() : resume.getId());
    out.setMatches(nlpResp.getTopMatches());
    out.setExtractedSkills(nlpResp.getExtractedSkills());
    out.setSkillGaps(nlpResp.getSkillGaps());
    out.setLearningSuggestions(nlpResp.getLearningSuggestions());
    out.setCareerRecommendations(nlpResp.getCareerRecommendations());
    out.setInterviewQuestions(nlpResp.getInterviewQuestions());
    return out;
  }
}

