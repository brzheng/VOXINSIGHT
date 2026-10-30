package com.voxinsight.service;

import com.voxinsight.service.CurrentUser;
import com.voxinsight.dto.AudioFile;
import com.voxinsight.dto.ReportResponse;
import com.voxinsight.mapper.TrainingMapper;
import com.voxinsight.model.Rows.AudioRow;
import com.voxinsight.model.Rows.FeedbackRow;
import com.voxinsight.model.Rows.ReportRow;
import com.voxinsight.web.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

  private final TrainingMapper trainingMapper;
  private final CurrentUser currentUser;

  public ReportService(TrainingMapper trainingMapper, CurrentUser currentUser) {
    this.trainingMapper = trainingMapper;
    this.currentUser = currentUser;
  }

  public ReportResponse load(long reportId) {
    ReportRow report = trainingMapper.findReport(currentUser.requireId(), reportId);
    if (report == null) {
      throw new NotFoundException("没有找到这份报告。");
    }
    List<FeedbackRow> feedbacks = trainingMapper.listFeedbacks(reportId);
    return new ReportResponse(
        report.id,
        report.topicId,
        report.title,
        DisplayTime.dateLabel(report.endedAt),
        DisplayTime.shortDate(report.endedAt),
        DisplayTime.durationLabel(report.durationSeconds),
        report.score,
        report.speechRate,
        report.pauseCount,
        report.fluency,
        report.label,
        texts(feedbacks, "strength"),
        texts(feedbacks, "improvement"),
        texts(feedbacks, "suggestion"),
        report.transcript == null || report.transcript.isBlank() ? null : report.transcript,
        report.hasAudio != null && report.hasAudio != 0,
        report.contentScore,
        report.contentSummary);
  }

  public AudioFile loadAudio(long reportId) {
    AudioRow audio = trainingMapper.findAudio(currentUser.requireId(), reportId);
    if (audio == null || audio.audioData == null || audio.audioData.length == 0) {
      throw new NotFoundException("这次练习没有保存录音。");
    }
    return new AudioFile(audio.mimeType, audio.audioData);
  }

  private static List<String> texts(List<FeedbackRow> feedbacks, String type) {
    return feedbacks.stream()
        .filter(item -> type.equals(item.feedbackType))
        .map(item -> item.content)
        .toList();
  }
}
