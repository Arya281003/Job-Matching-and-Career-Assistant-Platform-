package com.example.jobmatch.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.jobmatch.dto.AuthResponse;
import com.example.jobmatch.dto.LoginRequest;
import com.example.jobmatch.dto.RegisterRequest;
import com.example.jobmatch.model.User;
import com.example.jobmatch.repository.UserRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest request) {
    var existing = userRepository.findByEmail(request.getEmail());
    if (existing.isPresent()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email already registered");
    }

    User user = new User();
    user.setEmail(request.getEmail().toLowerCase());
    user.setFullName(request.getFullName());
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    userRepository.save(user);

    return ResponseEntity.status(HttpStatus.CREATED).body(
        new AuthResponse(user.getId(), user.getEmail(), user.getFullName()));
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request) {
    var userOpt = userRepository.findByEmail(request.getEmail().toLowerCase());
    if (userOpt.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }

    User user = userOpt.get();
    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }

    return ResponseEntity.ok(new AuthResponse(user.getId(), user.getEmail(), user.getFullName()));
  }
}

