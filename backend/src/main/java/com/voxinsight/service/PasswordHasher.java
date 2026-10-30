package com.voxinsight.service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {

  private static final int ITERATIONS = 120_000;
  private static final int BITS = 256;
  private static final SecureRandom RANDOM = new SecureRandom();

  private PasswordHasher() {}

  public static String hash(String password) {
    byte[] salt = new byte[16];
    RANDOM.nextBytes(salt);
    byte[] digest = pbkdf2(password, salt);
    return encode(salt) + ":" + encode(digest);
  }

  public static boolean matches(String password, String stored) {
    if (password == null || stored == null || !stored.contains(":")) {
      return false;
    }
    String[] parts = stored.split(":", 2);
    byte[] salt = Base64.getDecoder().decode(parts[0]);
    byte[] expected = Base64.getDecoder().decode(parts[1]);
    return MessageDigest.isEqual(expected, pbkdf2(password, salt));
  }

  private static byte[] pbkdf2(String password, byte[] salt) {
    try {
      KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, BITS);
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    } catch (Exception exception) {
      throw new IllegalStateException("无法处理密码。", exception);
    }
  }

  private static String encode(byte[] bytes) {
    return Base64.getEncoder().encodeToString(bytes);
  }
}
