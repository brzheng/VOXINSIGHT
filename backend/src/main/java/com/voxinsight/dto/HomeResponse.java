package com.voxinsight.dto;

public record HomeResponse(
    Brand brand,
    UserBrief user,
    int weeklyDone,
    int weeklyTarget,
    LatestTraining latest,
    Integer previousScore,
    RecommendedTopic recommended,
    java.util.List<NoticeItem> notices) {

  public record Brand(String zh, String en, String slogan) {}

  public record UserBrief(String nickname, String initial, boolean hasAvatar) {}

  public record LatestTraining(
      long reportId, long topicId, String title, String date, String duration, int score) {}

  public record RecommendedTopic(
      long id,
      String title,
      String category,
      String difficulty,
      int durationMin,
      int durationMax) {}
}
