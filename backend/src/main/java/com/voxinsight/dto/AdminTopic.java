package com.voxinsight.dto;

public record AdminTopic(
    long id,
    long categoryId,
    String categoryName,
    String title,
    String difficulty,
    int durationMin,
    int durationMax,
    String guide,
    boolean recommended,
    int sortOrder,
    int checkpointCount) {}
