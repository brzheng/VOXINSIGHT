package com.voxinsight.dto;

public record AdminSessionDetail(
    long id,
    String loginName,
    String nickname,
    long topicId,
    String title,
    String status,
    String startedAt,
    String endedAt,
    int durationSeconds,
    String failReason,
    Long reportId,
    Integer score,
    Integer speechRate,
    Integer pauseCount,
    Integer fluency,
    Integer contentScore,
    String transcript,
    String contentSummary,
    String resultLabel,
    boolean hasAudio) {}
