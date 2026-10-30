package com.voxinsight.dto;

import java.util.List;

public record AdminTopicRequest(
    long categoryId,
    String title,
    String difficulty,
    int durationMin,
    int durationMax,
    String guide,
    boolean recommended,
    int sortOrder,
    List<AdminCheckpoint> checkpoints) {}
