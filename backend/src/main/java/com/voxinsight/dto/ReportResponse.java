package com.voxinsight.dto;

import java.util.List;

public record ReportResponse(
    long id,
    long topicId,
    String title,
    String date,
    String shortDate,
    String duration,
    int score,
    int speechRate,
    int pauseCount,
    int fluency,
    String label,
    List<String> strengths,
    List<String> improvements,
    List<String> suggestions,
    String transcript,
    boolean hasAudio,
    Integer contentScore,
    String contentSummary) {}
