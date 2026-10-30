package com.voxinsight.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.voxinsight.service.ContentAssessment.Point;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContentAssessmentTest {

  private static final List<Point> INTRODUCTION =
      List.of(
          new Point("身份", List.of("我是", "我叫", "名叫")),
          new Point("学校或专业", List.of("大学", "专业", "学院", "学校")),
          new Point("经历", List.of("项目", "实习", "负责", "经历")),
          new Point("意向", List.of("希望", "应聘", "求职", "岗位")));

  @Test
  void fullAnswerCoversEveryPoint() {
    ContentAssessment.Result result =
        ContentAssessment.assess("我是林晓，来自明德大学软件工程专业，做过项目，希望应聘后端。", INTRODUCTION);
    assertEquals(100, result.score());
    assertTrue(result.summary().contains("全部要点"));
  }

  @Test
  void partialAnswerScoresTheCoveredShare() {
    ContentAssessment.Result result = ContentAssessment.assess("我是林晓，专业是软件工程。", INTRODUCTION);
    assertEquals(50, result.score());
    assertTrue(result.summary().contains("身份"));
    assertTrue(result.summary().contains("经历"));
  }

  @Test
  void emptyTranscriptScoresZero() {
    ContentAssessment.Result result = ContentAssessment.assess("   ", INTRODUCTION);
    assertEquals(0, result.score());
    assertTrue(result.summary().contains("没有识别到文字"));
  }

  @Test
  void splitsStoredKeywords() {
    assertEquals(List.of("我是", "我叫"), ContentAssessment.keywords("我是，我叫"));
  }
}
