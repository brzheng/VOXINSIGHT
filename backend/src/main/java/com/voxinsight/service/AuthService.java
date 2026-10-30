package com.voxinsight.service;

import com.voxinsight.dto.AuthResponse;
import com.voxinsight.mapper.AuthMapper;
import com.voxinsight.web.BadRequestException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private static final SecureRandom RANDOM = new SecureRandom();
  private final AuthMapper authMapper;

  public AuthService(AuthMapper authMapper) {
    this.authMapper = authMapper;
  }

  public AuthResponse login(String loginName, String password) {
    String name = normalizeLogin(loginName);
    String hash = authMapper.findPasswordHash(name);
    if (hash == null || !PasswordHasher.matches(password, hash)) {
      throw new BadRequestException("账号或密码不正确。");
    }
    Long userId = authMapper.findUserIdByLogin(name);
    String nickname = authMapper.findNickname(userId);
    return new AuthResponse(issueToken(userId), name, nickname == null ? name : nickname, roleOf(userId));
  }

  @Transactional
  public AuthResponse register(String loginName, String password, String nickname) {
    String name = normalizeLogin(loginName);
    if (!name.matches("[a-z0-9_]{3,32}")) {
      throw new BadRequestException("账号用 3 到 32 位小写字母、数字或下划线。");
    }
    if (password == null || password.length() < 6 || password.length() > 32) {
      throw new BadRequestException("密码需要 6 到 32 位。");
    }
    if (authMapper.findUserIdByLogin(name) != null) {
      throw new BadRequestException("这个账号已经注册。");
    }
    String shown = nickname == null || nickname.isBlank() ? name : nickname.trim();
    if (shown.length() > 32) {
      throw new BadRequestException("昵称太长。");
    }
    authMapper.insertUser(name, PasswordHasher.hash(password), shown);
    Long userId = authMapper.findNewUserId(name);
    authMapper.insertGoal(userId);
    authMapper.insertNotification(userId);
    authMapper.insertPrivacy(userId);
    return new AuthResponse(issueToken(userId), name, shown, roleOf(userId));
  }

  public void logout(String header) {
    if (header != null && header.startsWith("Bearer ")) {
      authMapper.deleteToken(header.substring(7).trim());
    }
  }

  private String issueToken(long userId) {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    String token = HexFormat.of().formatHex(bytes);
    authMapper.insertToken(token, userId, LocalDateTime.now().plusDays(30));
    return token;
  }

  private String roleOf(long userId) {
    String role = authMapper.findRole(userId);
    return role == null ? "user" : role;
  }

  private static String normalizeLogin(String loginName) {
    return loginName == null ? "" : loginName.trim().toLowerCase();
  }
}
