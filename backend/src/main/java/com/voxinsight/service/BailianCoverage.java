package com.voxinsight.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voxinsight.service.ContentAssessment.Point;
import com.voxinsight.service.ContentAssessment.Result;
import java.util.ArrayList;
import java.util.List;

final class BailianCoverage {

  private static final ObjectMapper JSON = new ObjectMapper();
  private static final int COMMENT_LIMIT = 80;

  private BailianCoverage() {}

  static String prompt(String title, String guide, List<Point> points, String transcript) {
    StringBuilder text = new StringBuilder();
    text.append("题目：").append(title == null ? "" : title.trim()).append('\n');
    text.append("录音提示：").append(guide == null ? "" : guide.trim()).append('\n');
    text.append("内容要点：\n");
    for (Point point : points) {
      text.append("- ").append(point.label()).append('\n');
    }
    text.append("识别文本：\n").append(transcript == null ? "" : transcript.trim()).append('\n');
    text.append("只返回 JSON，不要使用 Markdown。格式为 ");
    text.append("{\"covered\":[\"要点名称\"],\"comment\":\"一两句说明\"}。");
    text.append("covered 只能使用上面列出的要点名称。按语义判断有没有说到，不要只看是否出现相同的词。");
    text.append("comment 不超过 80 字，不要评价性格、心理或面试能否通过。");
    return text.toString();
  }

  static Result interpret(String modelContent, List<Point> points) {
    JsonNode root = read(modelContent);
    JsonNode coveredNode = root.get("covered");
    if (coveredNode == null || !coveredNode.isArray()) {
      throw new IllegalArgumentException("百炼返回缺少 covered。");
    }
    List<String> covered = new ArrayList<>();
    coveredNode.forEach(item -> {
      if (item != null && item.isTextual()) {
        covered.add(item.asText());
      }
    });
    String comment = "";
    JsonNode commentNode = root.get("comment");
    if (commentNode != null && commentNode.isTextual()) {
      comment = commentNode.asText().trim();
      if (comment.length() > COMMENT_LIMIT) {
        comment = comment.substring(0, COMMENT_LIMIT);
      }
    }
    return ContentAssessment.coverage(points, covered, comment);
  }

  private static JsonNode read(String modelContent) {
    String text = modelContent == null ? "" : modelContent.trim();
    if (text.startsWith("```")) {
      int firstLine = text.indexOf('\n');
      int closing = text.lastIndexOf("```");
      if (firstLine >= 0 && closing > firstLine) {
        text = text.substring(firstLine + 1, closing).trim();
      }
    }
    try {
      return JSON.readTree(text);
    } catch (Exception exception) {
      throw new IllegalArgumentException("百炼返回不是 JSON。");
    }
  }
}
