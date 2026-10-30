package com.voxinsight.mapper;

import com.voxinsight.model.Rows.ArticleRow;
import com.voxinsight.model.Rows.AudioRow;
import com.voxinsight.model.Rows.CheckpointRow;
import com.voxinsight.model.Rows.SessionRow;
import com.voxinsight.model.Rows.DashboardRow;
import com.voxinsight.model.Rows.FeedbackRow;
import com.voxinsight.model.Rows.HistoryRow;
import com.voxinsight.model.Rows.MetaRow;
import com.voxinsight.model.Rows.PreviousRow;
import com.voxinsight.model.Rows.ReportRow;
import com.voxinsight.model.Rows.ReportWrite;
import com.voxinsight.model.Rows.ScoreRow;
import com.voxinsight.model.Rows.SessionWrite;
import com.voxinsight.model.Rows.StatsRow;
import com.voxinsight.model.Rows.TopicRow;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TrainingMapper {

  DashboardRow findDashboard(@Param("userId") long userId);

  TopicRow findRecommended();

  ScoreRow findPreviousScore(@Param("userId") long userId);

  ReportRow findReport(@Param("userId") long userId, @Param("reportId") long reportId);

  AudioRow findAudio(@Param("userId") long userId, @Param("reportId") long reportId);

  List<FeedbackRow> listFeedbacks(@Param("reportId") long reportId);

  StatsRow findStats(@Param("userId") long userId);

  List<HistoryRow> listHistory(@Param("userId") long userId);

  MetaRow findMeta();

  List<ArticleRow> listArticles();

  PreviousRow findLatestReportForTopic(@Param("userId") long userId, @Param("topicId") long topicId);

  LocalDateTime findLatestEndedAt(@Param("userId") long userId);

  int insertSession(SessionWrite session);

  int insertReport(ReportWrite report);

  int insertFeedback(
      @Param("reportId") long reportId,
      @Param("feedbackType") String feedbackType,
      @Param("content") String content,
      @Param("sortOrder") int sortOrder);

  int deleteHistory(@Param("userId") long userId);

  int insertAudio(
      @Param("sessionId") long sessionId,
      @Param("mimeType") String mimeType,
      @Param("byteSize") int byteSize,
      @Param("audioData") byte[] audioData);

  int insertOpenSession(SessionWrite session);

  SessionRow findActive(@Param("userId") long userId, @Param("topicId") long topicId);

  SessionRow findSession(@Param("userId") long userId, @Param("sessionId") long sessionId);

  int updateStatus(
      @Param("userId") long userId,
      @Param("sessionId") long sessionId,
      @Param("status") String status,
      @Param("failReason") String failReason,
      @Param("endedAt") LocalDateTime endedAt,
      @Param("durationSeconds") Integer durationSeconds,
      @Param("allowed") String allowed);

  int upsertAudio(
      @Param("sessionId") long sessionId,
      @Param("mimeType") String mimeType,
      @Param("byteSize") int byteSize,
      @Param("audioData") byte[] audioData);

  AudioRow findSessionAudio(@Param("userId") long userId, @Param("sessionId") long sessionId);

  int discardSession(@Param("userId") long userId, @Param("sessionId") long sessionId);

  List<CheckpointRow> listCheckpoints(@Param("topicId") long topicId);
}
