package com.voxinsight.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class Rows {

  private Rows() {}

  public static class DashboardRow {
    public String nickname;
    public String brandNameZh;
    public String brandNameEn;
    public String slogan;
    public Integer weeklyTargetCount;
    public Integer weeklyCompletedCount;
    public Integer latestScore;
    public Long latestReportId;
    public Integer hasAvatar;
  }

  public static class TopicRow {
    public Long id;
    public String category;
    public String title;
    public String difficulty;
    public Integer durationMin;
    public Integer durationMax;
    public String guide;
    public Integer favorite;
  }

  public static class ReportRow {
    public Long id;
    public Long topicId;
    public String title;
    public LocalDateTime endedAt;
    public Integer durationSeconds;
    public Integer score;
    public Integer speechRate;
    public Integer pauseCount;
    public Integer fluency;
    public String label;
    public String transcript;
    public Integer hasAudio;
    public Integer contentScore;
    public String contentSummary;
  }

  public static class AudioRow {
    public String mimeType;
    public byte[] audioData;
  }

  public static class FeedbackRow {
    public String feedbackType;
    public String content;
  }

  public static class HistoryRow {
    public Long reportId;
    public Long sessionId;
    public Long topicId;
    public String topicTitle;
    public LocalDateTime trainedAt;
    public Integer durationSeconds;
    public Integer totalScore;
    public String status;
    public String failReason;
  }

  public static class SessionRow {
    public Long sessionId;
    public Long topicId;
    public String status;
    public Integer durationSeconds;
    public String failReason;
    public Integer hasAudio;
  }

  public static class CheckpointRow {
    public String label;
    public String keywords;
  }

  public static class StatsRow {
    public Integer totalSessionCount;
    public Integer totalDurationSeconds;
    public BigDecimal averageScore;
  }

  public static class UserRow {
    public String nickname;
    public String loginName;
    public String school;
    public String major;
    public String bio;
    public Integer hasAvatar;
  }

  public static class SettingsRow {
    public Integer reminderOn;
    public Integer reportNoticeOn;
    public Integer goalNoticeOn;
    public java.time.LocalTime reminderTime;
    public Integer keepHistory;
    public Integer anonymousStats;
  }

  public static class MetaRow {
    public String brandNameZh;
    public String brandNameEn;
    public String slogan;
    public String aboutText;
    public String versionName;
    public LocalDate asOfDate;
  }

  public static class PreviousRow {
    public Integer totalScore;
    public Integer speechRate;
    public Integer pauseCount;
    public Integer fluencyScore;
    public String resultLabel;
  }

  public static class ScoreRow {
    public Integer totalScore;
  }

  public static class SessionWrite {
    public Long sessionId;
    public long userId;
    public long topicId;
    public LocalDateTime startedAt;
    public LocalDateTime endedAt;
    public int durationSeconds;
  }

  public static class ReportWrite {
    public Long reportId;
    public long sessionId;
    public int totalScore;
    public int speechRate;
    public int pauseCount;
    public int fluencyScore;
    public String resultLabel;
    public String transcript;
    public Integer contentScore;
    public String contentSummary;
    public LocalDateTime analyzedAt;
  }

  public static class ArticleRow {
    public String title;
    public String content;
  }

  public static class NoticeRow {
    public Long id;
    public String content;
  }

  public static class FeedbackRowItem {
    public String category;
    public String content;
    public String status;
    public java.time.LocalDateTime createdAt;
    public String replyContent;
    public java.time.LocalDateTime repliedAt;
  }

  public static class SummaryRow {
    public Integer participants;
    public Integer weeklySessions;
  }

  public static class AvatarRow {
    public String mimeType;
    public byte[] data;
  }
}
