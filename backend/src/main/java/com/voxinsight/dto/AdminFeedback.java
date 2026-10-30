package com.voxinsight.dto;

public record AdminFeedback(
    long id,
    String loginName,
    String nickname,
    String category,
    String content,
    String status,
    String createdAt,
    String reply,
    String repliedAt) {}
