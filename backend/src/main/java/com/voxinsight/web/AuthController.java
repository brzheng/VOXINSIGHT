package com.voxinsight.web;

import com.voxinsight.dto.AuthResponse;
import com.voxinsight.dto.LoginRequest;
import com.voxinsight.dto.RegisterRequest;
import com.voxinsight.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/api/auth/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request.loginName(), request.password());
  }

  @PostMapping("/api/auth/register")
  public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
    return authService.register(request.loginName(), request.password(), request.nickname());
  }

  @PostMapping("/api/auth/logout")
  public void logout(HttpServletRequest request) {
    authService.logout(request.getHeader("Authorization"));
  }
}
