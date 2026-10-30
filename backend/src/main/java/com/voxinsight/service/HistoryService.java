package com.voxinsight.service;

import com.voxinsight.dto.HistoryResponse;
import com.voxinsight.dto.HistoryResponse.HistoryItem;
import com.voxinsight.dto.TrainingStats;
import com.voxinsight.mapper.ProfileMapper;
import com.voxinsight.mapper.TrainingMapper;
import com.voxinsight.model.Rows.HistoryRow;
import com.voxinsight.model.Rows.SettingsRow;
import com.voxinsight.model.Rows.StatsRow;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HistoryService {

  private final TrainingMapper trainingMapper;
  private final ProfileMapper profileMapper;
  private final CurrentUser currentUser;

  public HistoryService(
      TrainingMapper trainingMapper, ProfileMapper profileMapper, CurrentUser currentUser) {
    this.trainingMapper = trainingMapper;
    this.profileMapper = profileMapper;
    this.currentUser = currentUser;
  }

  public HistoryResponse load() {
    long userId = currentUser.requireId();
    List<HistoryItem> records =
        trainingMapper.listHistory(userId).stream().map(HistoryService::toItem).toList();
    SettingsRow settings = profileMapper.findSettings(userId);
    boolean keepHistory = settings != null && settings.keepHistory != null && settings.keepHistory != 0;
    return new HistoryResponse(stats(trainingMapper.findStats(userId)), records, keepHistory);
  }

  static TrainingStats stats(StatsRow row) {
    if (row == null) {
      return new TrainingStats(0, 0, "0秒", null);
    }
    int count = row.totalSessionCount == null ? 0 : row.totalSessionCount;
    int seconds = row.totalDurationSeconds == null ? 0 : row.totalDurationSeconds;
    return new TrainingStats(count, seconds, DisplayTime.durationLabel(seconds), average(row.averageScore));
  }

  private static HistoryItem toItem(HistoryRow row) {
    int seconds = row.durationSeconds == null ? 0 : row.durationSeconds;
    return new HistoryItem(
        row.reportId,
        row.sessionId == null ? 0 : row.sessionId,
        row.topicId,
        row.topicTitle,
        DisplayTime.dateLabel(row.trainedAt),
        DisplayTime.shortDate(row.trainedAt),
        DisplayTime.durationLabel(seconds),
        row.totalScore,
        row.status,
        row.failReason);
  }

  private static Double average(BigDecimal value) {
    return value == null ? null : value.doubleValue();
  }
}
