package com.voxinsight.service;

import java.util.ArrayList;
import java.util.List;

public final class SpeechAssessment {

  public static final String RESULT_LABEL = "语音识别结果";

  private SpeechAssessment() {}

  public record Previous(int speechRate, int pauseCount) {}

  public record FeedbackLine(String type, String content, int sortOrder) {}

  public record Result(
      int totalScore,
      int speechRate,
      int pauseCount,
      int fluencyScore,
      String resultLabel,
      String transcript,
      List<FeedbackLine> feedbacks) {}

  public static Result assess(
      String transcript, int pauseCount, double durationSeconds, String categoryName, Previous previous) {
    String spoken = transcript == null ? "" : transcript.trim();
    int characters = countCharacters(spoken);
    double seconds = Math.max(durationSeconds, 0.25);
    int pauses = clamp(pauseCount, 0, 6000);
    int speechRate = characters == 0 ? 0 : clamp((int) Math.round(characters * 60.0 / seconds), 0, 2000);
    double pausePerMinute = pauses * 60.0 / seconds;
    double rateGap = Math.abs(speechRate - 170);
    int pausePenalty = (int) Math.min(70, Math.round(pausePerMinute * 3));
    int ratePenalty = (int) Math.min(30, Math.round(rateGap * 0.15));
    int fluency = characters == 0 ? 0 : clamp(100 - pausePenalty - ratePenalty, 0, 100);
    int closeness = characters == 0 ? 0 : 100 - Math.min(100, (int) Math.round(rateGap));
    int score =
        characters == 0 ? 0 : clamp((int) Math.round(fluency * 0.72 + closeness * 0.28), 0, 100);

    List<FeedbackLine> feedbacks = new ArrayList<>();
    feedbacks.add(new FeedbackLine("strength", strength(spoken, speechRate, pauses, previous), 1));
    feedbacks.add(new FeedbackLine("improvement", improvement(characters, speechRate, pauses, pausePerMinute, previous), 2));
    feedbacks.add(new FeedbackLine("suggestion", suggestionForPace(characters, speechRate, pauses, pausePerMinute, previous), 3));
    feedbacks.add(new FeedbackLine("suggestion", suggestionForStructure(categoryName), 4));
    feedbacks.forEach(line -> {
      if (line.content().length() > 255) {
        throw new IllegalStateException("反馈文案超过 255 字");
      }
    });
    return new Result(score, speechRate, pauses, fluency, RESULT_LABEL, spoken, List.copyOf(feedbacks));
  }

  static int countCharacters(String text) {
    int count = 0;
    for (int i = 0; i < text.length(); ) {
      int codePoint = text.codePointAt(i);
      i += Character.charCount(codePoint);
      if (Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN || Character.isDigit(codePoint)) {
        count++;
      }
    }
    return count;
  }

  private static String strength(String spoken, int speechRate, int pauseCount, Previous previous) {
    boolean naturalRate = speechRate >= 140 && speechRate <= 190;
    if (spoken.isEmpty()) {
      return "没有识别到清晰语句。";
    }
    String snippet = excerpt(spoken);
    if (previous != null && pauseCount < previous.pauseCount() && naturalRate) {
      return "识别到「"
          + snippet
          + "」。语速 "
          + speechRate
          + " 字/分钟，停顿从 "
          + previous.pauseCount()
          + " 次降到 "
          + pauseCount
          + " 次。";
    }
    if (naturalRate) {
      return "识别到「" + snippet + "」。语速接近自然交谈，当前为 " + speechRate + " 字/分钟。";
    }
    return "识别到「" + snippet + "」。本次语速为 " + speechRate + " 字/分钟，停顿 " + pauseCount + " 次。";
  }

  private static String improvement(
      int characters, int speechRate, int pauseCount, double pausePerMinute, Previous previous) {
    if (characters == 0) {
      return "录音里没有识别出汉字，语速按 0 字/分钟计。";
    }
    boolean pausesIncreased = previous != null && pauseCount > previous.pauseCount();
    if (pausePerMinute >= 5 || pausesIncreased) {
      return "句中停顿有 " + pauseCount + " 次，约每分钟 " + Math.round(pausePerMinute) + " 次。";
    }
    if (speechRate > 220) {
      return "语速为 " + speechRate + " 字/分钟，偏快。";
    }
    if (speechRate < 120) {
      return "语速为 " + speechRate + " 字/分钟，偏慢。";
    }
    return "语句已经能听清，还可以把关键信息说得更具体。";
  }

  private static String suggestionForPace(
      int characters, int speechRate, int pauseCount, double pausePerMinute, Previous previous) {
    if (characters == 0) {
      return "靠近麦克风，用完整的句子再录一次。";
    }
    boolean pausesIncreased = previous != null && pauseCount > previous.pauseCount();
    if (pausePerMinute >= 5 || pausesIncreased) {
      return "少停半拍，把一句话连续说完。本次停顿为 " + pauseCount + " 次。";
    }
    if (speechRate > 220) {
      return "先把语速放到大约 170 字/分钟。本次为 " + speechRate + " 字/分钟。";
    }
    if (speechRate < 120) {
      return "适当加快，减少句与句之间的空档。本次为 " + speechRate + " 字/分钟。";
    }
    return "语速合适，下一步把每个例子说到具体的结果。";
  }

  private static String suggestionForStructure(String categoryName) {
    if ("面试表达".equals(categoryName)) {
      return "建议按照「背景—行动—结果」组织面试回答。";
    }
    if ("演讲训练".equals(categoryName)) {
      return "可以先给听众一个明确的主题句，再展开两层理由。";
    }
    return "试着用日常对话的语气，把一件具体的事讲完整。";
  }

  private static String excerpt(String spoken) {
    String compact = spoken.replaceAll("\\s+", "");
    int end = Math.min(compact.length(), 18);
    return end == compact.length() ? compact : compact.substring(0, end) + "…";
  }

  private static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }
}
