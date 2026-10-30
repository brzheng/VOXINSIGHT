package com.voxinsight.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.voxinsight.config.AppProperties;
import com.voxinsight.service.ContentAssessment.Point;
import com.voxinsight.service.ContentAssessment.Result;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BailianContentJudge {

  private static final Logger log = LoggerFactory.getLogger(BailianContentJudge.class);
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final String SYSTEM =
      "你是表达练习的内容评委。只判断识别文本有没有说到给定要点，并返回 JSON。";

  private final AppProperties properties;
  private final HttpClient http;

  public BailianContentJudge(AppProperties properties) {
    this.properties = properties;
    this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  }

  public Result judge(String title, String guide, List<Point> points, String transcript) {
    String apiKey = properties.getDashscopeApiKey();
    if (apiKey == null || apiKey.isBlank() || points == null || points.isEmpty()) {
      return null;
    }
    try {
      HttpRequest request = HttpRequest.newBuilder(endpoint())
          .timeout(Duration.ofSeconds(20))
          .header("Authorization", "Bearer " + apiKey.trim())
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(body(title, guide, points, transcript), StandardCharsets.UTF_8))
          .build();
      HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        log.warn("百炼内容评价返回 {}", response.statusCode());
        return null;
      }
      return BailianCoverage.interpret(message(response.body()), points);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      log.warn("百炼内容评价被中断");
      return null;
    } catch (Exception exception) {
      log.warn("百炼内容评价失败");
      return null;
    }
  }

  private URI endpoint() {
    String root = properties.getDashscopeBaseUrl();
    if (root == null || root.isBlank()) {
      root = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    }
    root = root.trim();
    if (root.endsWith("/")) {
      root = root.substring(0, root.length() - 1);
    }
    return URI.create(root + "/chat/completions");
  }

  private String body(String title, String guide, List<Point> points, String transcript) throws Exception {
    ObjectNode root = JSON.createObjectNode();
    String model = properties.getDashscopeModel();
    root.put("model", model == null || model.isBlank() ? "qwen-plus" : model.trim());
    root.put("temperature", 0.1);
    root.putObject("response_format").put("type", "json_object");
    ArrayNode messages = root.putArray("messages");
    messages.addObject().put("role", "system").put("content", SYSTEM);
    messages.addObject().put("role", "user").put("content", BailianCoverage.prompt(title, guide, points, transcript));
    return JSON.writeValueAsString(root);
  }

  private static String message(String responseBody) throws Exception {
    JsonNode content = JSON.readTree(responseBody).path("choices").path(0).path("message").path("content");
    if (!content.isTextual() || content.asText().isBlank()) {
      throw new IllegalArgumentException("百炼没有返回评价内容。");
    }
    return content.asText();
  }
}
