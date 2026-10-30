package com.voxinsight.dto;

public record AdminSession(
    long id,
    String loginName,
    String nickname,
    String title,
    String status,
    String startedAt,
    String endedAt,
    int durationSeconds,
    Long reportId,
    Integer score) {}
