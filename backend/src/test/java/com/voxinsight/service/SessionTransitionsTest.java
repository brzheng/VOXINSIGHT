package com.voxinsight.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.voxinsight.web.BadRequestException;
import org.junit.jupiter.api.Test;

class SessionTransitionsTest {

  @Test
  void pauseOnlyFromRecording() {
    assertDoesNotThrow(() -> SessionTransitions.pause("recording"));
    assertThrows(BadRequestException.class, () -> SessionTransitions.pause("paused"));
  }

  @Test
  void resumeOnlyFromPaused() {
    assertDoesNotThrow(() -> SessionTransitions.resume("paused"));
    assertThrows(BadRequestException.class, () -> SessionTransitions.resume("failed"));
  }

  @Test
  void failedSessionCanBeAnalyzedAgain() {
    assertDoesNotThrow(() -> SessionTransitions.analyze("failed"));
    assertThrows(BadRequestException.class, () -> SessionTransitions.analyze("completed"));
  }

  @Test
  void completedSessionCannotBeDiscarded() {
    assertThrows(BadRequestException.class, () -> SessionTransitions.discard("completed"));
    assertDoesNotThrow(() -> SessionTransitions.discard("paused"));
  }
}
