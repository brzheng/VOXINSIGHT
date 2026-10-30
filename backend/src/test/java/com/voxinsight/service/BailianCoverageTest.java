package com.voxinsight.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.voxinsight.service.ContentAssessment.Point;
import java.util.List;
import org.junit.jupiter.api.Test;

class BailianCoverageTest {

  private static final List<Point> INTRODUCTION =
      List.of(
          new Point("身份", List.of("我是")),
          new Point("学校或专业", List.of("大学")),
          new Point("经历", List.of("项目")),
          new Point("意向", List.of("希望")));

  @Test
  void scoreUsesOnlyKnownPointLabels() {
    ContentAssessment.Result result =
        BailianCoverage.interpret(
            "{\"covered\":[\"身份\",\"学校或专业\",\"不存在的要点\"],\"comment\":\"提到了身份和学校。\"}",
            INTRODUCTION);
    assertEquals(50, result.score());
    assertTrue(result.summary().startsWith("语义评价。"));
    assertTrue(result.summary().contains("身份"));
    assertTrue(result.summary().contains("经历"));
    assertTrue(result.summary().contains("提到了身份和学校。"));
  }

  @Test
  void stripsMarkdownFence() {
    ContentAssessment.Result result =
        BailianCoverage.interpret("```json\n{\"covered\":[\"身份\",\"学校或专业\",\"经历\",\"意向\"]}\n```", INTRODUCTION);
    assertEquals(100, result.score());
    assertTrue(result.summary().contains("全部要点"));
  }

  @Test
  void rejectsTextThatIsNotJson() {
    assertThrows(IllegalArgumentException.class, () -> BailianCoverage.interpret("不是 JSON", INTRODUCTION));
  }

  @Test
  void promptAsksForMeaningRatherThanKeywordLists() {
    String prompt = BailianCoverage.prompt("自我介绍", "自然表达", INTRODUCTION, "我来自明德大学。");
    assertTrue(prompt.contains("身份"));
    assertTrue(prompt.contains("按语义判断"));
    assertFalse(prompt.contains("我是"));
  }
}
