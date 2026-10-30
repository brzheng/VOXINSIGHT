package com.voxinsight.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ContentAssessment {

  private ContentAssessment() {}

  public record Point(String label, List<String> keywords) {}

  public record Result(int score, String summary) {}

  public static Result assess(String transcript, List<Point> points) {
    String spoken = transcript == null ? "" : transcript.trim();
    if (spoken.isEmpty()) {
      return new Result(0, "没有识别到文字，无法对照题目要点。");
    }
    if (points == null || points.isEmpty()) {
      return new Result(0, "这道题还没有配置内容要点。");
    }
    List<String> covered = new ArrayList<>();
    List<String> missing = new ArrayList<>();
    for (Point point : points) {
      if (covers(spoken, point.keywords())) {
        covered.add(point.label());
      } else {
        missing.add(point.label());
      }
    }
    int score = (int) Math.round(covered.size() * 100.0 / points.size());
    return new Result(score, summary(covered, missing));
  }

  public static Result coverage(List<Point> points, List<String> coveredLabels, String comment) {
    if (points == null || points.isEmpty()) {
      return new Result(0, "这道题还没有配置内容要点。");
    }
    Set<String> wanted = new LinkedHashSet<>();
    if (coveredLabels != null) {
      for (String label : coveredLabels) {
        if (label != null && !label.isBlank()) {
          wanted.add(label.trim());
        }
      }
    }
    List<String> covered = new ArrayList<>();
    List<String> missing = new ArrayList<>();
    for (Point point : points) {
      if (wanted.contains(point.label())) {
        covered.add(point.label());
      } else {
        missing.add(point.label());
      }
    }
    int score = (int) Math.round(covered.size() * 100.0 / points.size());
    String text = "语义评价。" + summary(covered, missing);
    String note = comment == null ? "" : comment.trim();
    if (!note.isEmpty()) {
      text = text + note;
    }
    if (text.length() > 500) {
      text = text.substring(0, 500);
    }
    return new Result(score, text);
  }

  public static List<String> keywords(String stored) {
    if (stored == null || stored.isBlank()) {
      return List.of();
    }
    String[] parts = stored.split("[,，]");
    List<String> keywords = new ArrayList<>();
    for (String part : parts) {
      String keyword = part.trim();
      if (!keyword.isEmpty()) {
        keywords.add(keyword);
      }
    }
    return List.copyOf(keywords);
  }

  private static boolean covers(String spoken, List<String> keywords) {
    if (keywords == null) {
      return false;
    }
    for (String keyword : keywords) {
      if (keyword != null && !keyword.isBlank() && spoken.contains(keyword.trim())) {
        return true;
      }
    }
    return false;
  }

  private static String summary(List<String> covered, List<String> missing) {
    if (missing.isEmpty()) {
      return "已覆盖全部要点：" + String.join("、", covered) + "。";
    }
    if (covered.isEmpty()) {
      return "还没提到：" + String.join("、", missing) + "。";
    }
    return "已覆盖：" + String.join("、", covered) + "。还没提到：" + String.join("、", missing) + "。";
  }
}
