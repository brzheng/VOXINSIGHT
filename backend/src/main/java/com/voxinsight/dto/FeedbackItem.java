package com.voxinsight.dto;

public record FeedbackItem(
    String category, String content, String status, String createdAt, String reply, String repliedAt) {}
