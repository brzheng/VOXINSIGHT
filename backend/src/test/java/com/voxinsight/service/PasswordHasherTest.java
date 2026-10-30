package com.voxinsight.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {

  private static final String DEMO_HASH =
      "obLD1OX2BxgpOktcbX6PkA==:KrBcaQjQfH3fBwnuWTokuOFkwhs3JDMbI2Gz6Yh243A=";

  @Test
  void demoPasswordMatchesTheSeedHash() {
    assertTrue(PasswordHasher.matches("demo1234", DEMO_HASH));
    assertFalse(PasswordHasher.matches("wrong-password", DEMO_HASH));
  }
}
