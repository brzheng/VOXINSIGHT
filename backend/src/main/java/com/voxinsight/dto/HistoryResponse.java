package com.voxinsight.dto;

import java.util.List;

public record HistoryResponse(TrainingStats stats, List<HistoryItem> records, boolean keepHistory) {

  public record HistoryItem(
      Long reportId,
      long sessionId,
      long topicId,
      String title,
      String date,
      String shortDate,
      String duration,
      Integer score,
      String status,
      String failReason) {}
}
