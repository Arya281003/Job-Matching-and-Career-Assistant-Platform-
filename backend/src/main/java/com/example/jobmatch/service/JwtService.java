package com.example.jobmatch.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final byte[] secret;

  public JwtService(@Value("${app.jwt.secret:change-this-dev-secret-before-production}") String secret) {
    this.secret = secret.getBytes(StandardCharsets.UTF_8);
  }

  public String createToken(Long userId, String email) {
    try {
      long expiresAt = Instant.now().plusSeconds(60 * 60 * 8).getEpochSecond();
      String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
      String payload = "{\"sub\":\"" + userId + "\",\"email\":\"" + escape(email) + "\",\"exp\":" + expiresAt + "}";
      String unsigned = base64Url(header.getBytes(StandardCharsets.UTF_8)) + "." +
          base64Url(payload.getBytes(StandardCharsets.UTF_8));
      return unsigned + "." + sign(unsigned);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to create JWT", e);
    }
  }

  public Optional<Long> validateAndGetUserId(String token) {
    try {
      String[] parts = token.split("\\.");
      if (parts.length != 3) {
        return Optional.empty();
      }
      String unsigned = parts[0] + "." + parts[1];
      if (!constantTimeEquals(sign(unsigned), parts[2])) {
        return Optional.empty();
      }

      String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
      long exp = Long.parseLong(extractJsonValue(payload, "exp"));
      if (Instant.now().getEpochSecond() > exp) {
        return Optional.empty();
      }
      return Optional.of(Long.parseLong(extractJsonValue(payload, "sub")));
    } catch (Exception ignored) {
      return Optional.empty();
    }
  }

  private String sign(String unsigned) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret, "HmacSHA256"));
    return base64Url(mac.doFinal(unsigned.getBytes(StandardCharsets.UTF_8)));
  }

  private String base64Url(byte[] input) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(input);
  }

  private String escape(String value) {
    return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private String extractJsonValue(String json, String key) {
    String marker = "\"" + key + "\":";
    int start = json.indexOf(marker);
    if (start < 0) {
      return "";
    }
    start += marker.length();
    if (json.charAt(start) == '"') {
      int end = json.indexOf('"', start + 1);
      return json.substring(start + 1, end);
    }
    int end = json.indexOf(',', start);
    if (end < 0) {
      end = json.indexOf('}', start);
    }
    return json.substring(start, end).trim();
  }

  private boolean constantTimeEquals(String a, String b) {
    if (a == null || b == null || a.length() != b.length()) {
      return false;
    }
    int result = 0;
    for (int i = 0; i < a.length(); i++) {
      result |= a.charAt(i) ^ b.charAt(i);
    }
    return result == 0;
  }
}
