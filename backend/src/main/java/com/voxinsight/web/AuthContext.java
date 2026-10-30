package com.voxinsight.web;

public final class AuthContext {

  private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

  private AuthContext() {}

  public static void set(long userId) {
    USER_ID.set(userId);
  }

  public static Long id() {
    return USER_ID.get();
  }

  public static void clear() {
    USER_ID.remove();
  }
}
