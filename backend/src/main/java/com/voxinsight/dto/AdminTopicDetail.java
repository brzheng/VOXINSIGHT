package com.voxinsight.dto;

import java.util.List;

public record AdminTopicDetail(
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
    List<AdminCheckpoint> checkpoints) {}
