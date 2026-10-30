package com.voxinsight.dto;

public record SessionState(
    long sessionId,
    long topicId,
    String status,
    int durationSeconds,
    boolean hasAudio,
    String failReason) {}
