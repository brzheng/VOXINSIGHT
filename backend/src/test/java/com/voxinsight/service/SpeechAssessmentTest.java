package com.voxinsight.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.voxinsight.service.SpeechAssessment.Previous;
import com.voxinsight.service.SpeechAssessment.Result;
import org.junit.jupiter.api.Test;

class SpeechAssessmentTest {

  @Test
  void scoresFromRecognizedCharactersAndMeasuredPauses() {
    Result result = SpeechAssessment.assess("我是林晓，来自明德大学软件工程专业。", 1, 10, "面试表达", null);

    assertEquals(16, SpeechAssessment.countCharacters("我是林晓，来自明德大学软件工程专业。"));
    assertEquals(96, result.speechRate());
    assertEquals(1, result.pauseCount());
    assertTrue(result.fluencyScore() > 0);
    assertTrue(result.totalScore() > 0);
    assertEquals(SpeechAssessment.RESULT_LABEL, result.resultLabel());
    assertTrue(text(result, "strength").contains("我是林晓"));
    assertTrue(text(result, "strength").contains("96 字/分钟"));
    assertTrue(joinedSuggestions(result).contains("背景—行动—结果"));
  }

  @Test
  void givesAZeroScoreWhenNothingWasRecognized() {
    Result result = SpeechAssessment.assess("   ", 0, 8, "日常口语", null);

    assertEquals(0, result.speechRate());
    assertEquals(0, result.totalScore());
    assertEquals(0, result.fluencyScore());
    assertTrue(text(result, "strength").contains("没有识别到"));
  }

  @Test
  void mentionsFewerPausesOnlyAgainstAnEarlierRecognition() {
    Result result =
        SpeechAssessment.assess(
            "今天把自我介绍又说了一遍，我来自明德大学，正在准备软件工程岗位的面试。",
            2,
            12,
            "演讲训练",
            new Previous(140, 6));

    assertTrue(text(result, "strength").contains("从 6 次降到 2 次"));
    assertTrue(joinedSuggestions(result).contains("主题句"));
  }

  private static String text(Result result, String type) {
    return result.feedbacks().stream()
        .filter(line -> line.type().equals(type))
        .map(line -> line.content())
        .findFirst()
        .orElseThrow();
  }

  private static String joinedSuggestions(Result result) {
    return result.feedbacks().stream()
        .filter(line -> line.type().equals("suggestion"))
        .map(line -> line.content())
        .reduce("", (left, right) -> left + right);
  }
}
