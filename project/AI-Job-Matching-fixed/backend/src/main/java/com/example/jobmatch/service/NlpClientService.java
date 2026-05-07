package com.example.jobmatch.service;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.example.jobmatch.dto.NlpExtractResponse;
import com.example.jobmatch.dto.NlpJobPayload;
import com.example.jobmatch.dto.NlpMatchRequest;
import com.example.jobmatch.dto.NlpMatchResponse;

@Service
public class NlpClientService {

  private final RestTemplate restTemplate;
  private final String baseUrl;

  public NlpClientService(RestTemplate restTemplate, @Value("${nlp.service.base-url}") String baseUrl) {
    this.restTemplate = restTemplate;
    this.baseUrl = baseUrl;
  }

  public NlpExtractResponse extractSkills(byte[] fileBytes, String fileName) {
    String url = baseUrl + "/extract-skills";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    ByteArrayResource resource = new ByteArrayResource(fileBytes) {
      @Override
      public String getFilename() {
        return fileName;
      }
    };

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", resource);

    HttpEntity<MultiValueMap<String, Object>> requestEntity =
        new HttpEntity<>(body, headers);

    ResponseEntity<NlpExtractResponse> resp =
        restTemplate.postForEntity(url, requestEntity, NlpExtractResponse.class);
    return resp.getBody();
  }

  public NlpMatchResponse matchJobs(String resumeText, java.util.List<NlpJobPayload> jobs) {
    String url = baseUrl + "/match-jobs";

    NlpMatchRequest req = new NlpMatchRequest();
    req.setResumeText(resumeText);
    req.setJobs(jobs == null ? Collections.emptyList() : jobs);

    ResponseEntity<NlpMatchResponse> resp =
        restTemplate.postForEntity(url, req, NlpMatchResponse.class);
    return resp.getBody();
  }
}

