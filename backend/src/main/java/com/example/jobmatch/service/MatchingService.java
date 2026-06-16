package com.example.jobmatch.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.jobmatch.dto.MatchResponse;
import com.example.jobmatch.dto.NlpExtractResponse;
import com.example.jobmatch.dto.NlpJobPayload;
import com.example.jobmatch.dto.NlpMatchResponse;
import com.example.jobmatch.dto.ProfileUpsertRequest;
import com.example.jobmatch.dto.ResumeParseResponse;
import com.example.jobmatch.model.UserProfile;
import com.example.jobmatch.model.mongo.AnalysisDocument;
import com.example.jobmatch.model.mongo.JobDocument;
import com.example.jobmatch.model.mongo.ResumeDocument;
import com.example.jobmatch.repository.AnalysisRepository;
import com.example.jobmatch.repository.JobRepository;
import com.example.jobmatch.repository.ResumeRepository;
import com.example.jobmatch.repository.UserProfileRepository;

@Service
public class MatchingService {

  private static final String IGNORED_SKILL = "Resume Parsing";

  private final JobRepository jobRepository;
  private final ResumeRepository resumeRepository;
  private final AnalysisRepository analysisRepository;
  private final UserProfileRepository userProfileRepository;
  private final NlpClientService nlpClientService;

  public MatchingService(
      JobRepository jobRepository,
      ResumeRepository resumeRepository,
      AnalysisRepository analysisRepository,
      UserProfileRepository userProfileRepository,
      NlpClientService nlpClientService) {
    this.jobRepository = jobRepository;
    this.resumeRepository = resumeRepository;
    this.analysisRepository = analysisRepository;
    this.userProfileRepository = userProfileRepository;
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
        "Python", "NLP", "BERT", "spaCy", "Machine Learning"));

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

  public MatchResponse matchFromProfile(ProfileUpsertRequest request) {
    UserProfile profile = saveProfile(request);
    String profileText = profileToText(profile);
    List<NlpJobPayload> jobPayloads = loadJobPayloads();
    NlpMatchResponse nlpResp = nlpClientService.matchJobs("", profileText, jobPayloads);

    MatchResponse out = toMatchResponse(nlpResp);
    out.setResumeId("profile-only-" + java.util.UUID.randomUUID());

    try {
      AnalysisDocument analysis = new AnalysisDocument();
      analysis.setUserId(request.getUserId());
      analysis.setResumeId(out.getResumeId());
      analysis.setFileName("Candidate profile");
      analysis.setMatches(out.getMatches());
      analysis.setExtractedSkills(out.getExtractedSkills());
      analysis.setSkillGaps(out.getSkillGaps());
      analysis.setLearningSuggestions(out.getLearningSuggestions());
      analysis.setCareerRecommendations(out.getCareerRecommendations());
      analysis.setInterviewQuestions(out.getInterviewQuestions());
      analysis.setScoreBreakdown(out.getScoreBreakdown());
      analysis.setCreatedAt(Instant.now());
      analysisRepository.save(analysis);
      out.setAnalysisId(analysis.getId());
    } catch (Exception ignored) {
      // Profile-only matching should still work if MongoDB is temporarily unavailable.
    }

    return out;
  }

  public MatchResponse matchFromFile(byte[] fileBytes, String fileName, Long userId) {
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

    List<NlpJobPayload> jobPayloads = loadJobPayloads();

    String profileText = loadProfileText(userId);
    NlpMatchResponse nlpResp = nlpClientService.matchJobs(extract.getText(), profileText, jobPayloads);

    MatchResponse out = toMatchResponse(nlpResp);
    out.setResumeId(resume.getId() == null ? "demo-" + java.util.UUID.randomUUID() : resume.getId());

    try {
      AnalysisDocument analysis = new AnalysisDocument();
      analysis.setUserId(userId);
      analysis.setResumeId(out.getResumeId());
      analysis.setFileName(fileName);
      analysis.setMatches(out.getMatches());
      analysis.setExtractedSkills(out.getExtractedSkills());
      analysis.setSkillGaps(out.getSkillGaps());
      analysis.setLearningSuggestions(out.getLearningSuggestions());
      analysis.setCareerRecommendations(out.getCareerRecommendations());
      analysis.setInterviewQuestions(out.getInterviewQuestions());
      analysis.setScoreBreakdown(out.getScoreBreakdown());
      analysis.setCreatedAt(Instant.now());
      analysisRepository.save(analysis);
      out.setAnalysisId(analysis.getId());
    } catch (Exception ignored) {
      // If MongoDB is temporarily unavailable, still return the live analysis.
    }
    return out;
  }

  private UserProfile saveProfile(ProfileUpsertRequest request) {
    UserProfile profile = userProfileRepository.findByUserId(request.getUserId()).orElseGet(() -> {
      UserProfile next = new UserProfile();
      next.setUserId(request.getUserId());
      return next;
    });
    profile.setEducation(request.getEducation());
    profile.setSkills(request.getSkills());
    profile.setExperience(request.getExperience());
    profile.setCareerPreferences(request.getCareerPreferences());
    return userProfileRepository.save(profile);
  }

  private List<NlpJobPayload> loadJobPayloads() {
    List<JobDocument> jobs;
    try {
      jobs = jobRepository.findAll();
      if (jobs == null || jobs.isEmpty()) {
        jobs = demoJobs();
      }
    } catch (Exception ignored) {
      jobs = demoJobs();
    }
    return jobs.stream().map(j -> {
      NlpJobPayload p = new NlpJobPayload();
      p.setTitle(j.getTitle());
      p.setDescription(j.getDescription());
      p.setRequiredSkills(filterIgnoredSkills(j.getRequiredSkills()));
      return p;
    }).toList();
  }

  private List<String> filterIgnoredSkills(List<String> skills) {
    if (skills == null) {
      return List.of();
    }
    return skills.stream()
        .filter(skill -> skill != null && !IGNORED_SKILL.equalsIgnoreCase(skill.trim()))
        .toList();
  }

  private MatchResponse toMatchResponse(NlpMatchResponse nlpResp) {
    MatchResponse out = new MatchResponse();
    out.setMatches(nlpResp.getTopMatches());
    out.setExtractedSkills(nlpResp.getExtractedSkills());
    out.setSkillGaps(nlpResp.getSkillGaps());
    out.setLearningSuggestions(nlpResp.getLearningSuggestions());
    out.setCareerRecommendations(nlpResp.getCareerRecommendations());
    out.setInterviewQuestions(nlpResp.getInterviewQuestions());
    out.setScoreBreakdown(nlpResp.getScoreBreakdown());
    return out;
  }

  private String loadProfileText(Long userId) {
    if (userId == null) {
      return "";
    }
    try {
      return userProfileRepository.findByUserId(userId)
          .map(this::profileToText)
          .orElse("");
    } catch (Exception ignored) {
      return "";
    }
  }

  private String profileToText(UserProfile profile) {
    return String.join("\n",
        nullToEmpty(profile.getEducation()),
        nullToEmpty(profile.getSkills()),
        nullToEmpty(profile.getExperience()),
        nullToEmpty(profile.getCareerPreferences()));
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}

