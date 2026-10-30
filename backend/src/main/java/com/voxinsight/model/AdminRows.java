package com.voxinsight.model;

public final class AdminRows {

  private AdminRows() {}

  public static class OverviewRow {
    public int userCount;
    public int topicCount;
    public int sessionCount;
    public int pendingFeedbackCount;
  }

  public static class UserRow {
    public long id;
    public String loginName;
    public String nickname;
    public String school;
    public String major;
    public String bio;
    public String role;
    public String createdAt;
    public int sessionCount;
  }

  public static class CategoryRow {
    public long id;
    public String name;
    public int sortOrder;
  }

  public static class TopicRow {
    public long id;
    public long categoryId;
    public String categoryName;
    public String title;
    public String difficulty;
    public int durationMin;
    public int durationMax;
    public String guide;
    public int recommended;
    public int sortOrder;
    public int checkpointCount;
  }

  public static class TopicWrite {
    public Long id;
    public long categoryId;
    public String title;
    public String difficulty;
    public int durationMin;
    public int durationMax;
    public String guide;
    public int recommended;
    public int sortOrder;
  }

  public static class CheckpointRow {
    public String label;
    public String keywords;
  }

  public static class SessionRow {
    public long id;
    public String loginName;
    public String nickname;
    public String title;
    public String status;
    public String startedAt;
    public String endedAt;
    public int durationSeconds;
    public Long reportId;
    public Integer score;
  }

  public static class SessionDetailRow {
    public long id;
    public String loginName;
    public String nickname;
    public long topicId;
    public String title;
    public String status;
    public String startedAt;
    public String endedAt;
    public int durationSeconds;
    public String failReason;
    public Long reportId;
    public Integer score;
    public Integer speechRate;
    public Integer pauseCount;
    public Integer fluency;
    public Integer contentScore;
    public String transcript;
    public String contentSummary;
    public String resultLabel;
    public int hasAudio;
  }

  public static class FeedbackRow {
    public long id;
    public String loginName;
    public String nickname;
    public String category;
    public String content;
    public String status;
    public String createdAt;
    public String reply;
    public String repliedAt;
  }
}
