package com.voxinsight.dto;

public record TopicCard(
    long id,
    String category,
    String title,
    String difficulty,
    int durationMin,
    int durationMax,
    String guide,
    boolean favorite) {}
