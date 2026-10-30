package com.voxinsight.service;

import com.voxinsight.dto.CreatedSession;
import com.voxinsight.dto.SessionState;
import com.voxinsight.mapper.TopicMapper;
import com.voxinsight.mapper.TrainingMapper;
import com.voxinsight.model.Rows.AudioRow;
import com.voxinsight.model.Rows.CheckpointRow;
import com.voxinsight.model.Rows.PreviousRow;
import com.voxinsight.model.Rows.ReportWrite;
import com.voxinsight.model.Rows.SessionRow;
import com.voxinsight.model.Rows.SessionWrite;
import com.voxinsight.model.Rows.TopicRow;
import com.voxinsight.service.AudioUpload.Accepted;
import com.voxinsight.service.ContentAssessment.Point;
import com.voxinsight.service.ContentAssessment.Result;
import com.voxinsight.service.SpeechAssessment.Previous;
import com.voxinsight.web.BadRequestException;
import com.voxinsight.web.NotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class SessionService {

  private static final Logger log = LoggerFactory.getLogger(SessionService.class);

  private final TrainingMapper trainingMapper;
  private final TopicMapper topicMapper;
  private final CurrentUser currentUser;
  private final NoticeService noticeService;
  private final AudioDecoder audioDecoder;
  private final AudioJoiner audioJoiner;
  private final SpeechRecognizer speechRecognizer;
  private final BailianContentJudge bailianContentJudge;
  private final TransactionTemplate transaction;
  private final TransactionTemplate statusTransaction;

  public SessionService(
      TrainingMapper trainingMapper,
      TopicMapper topicMapper,
      CurrentUser currentUser,
      NoticeService noticeService,
      AudioDecoder audioDecoder,
      AudioJoiner audioJoiner,
      SpeechRecognizer speechRecognizer,
      BailianContentJudge bailianContentJudge,
      PlatformTransactionManager transactions) {
    this.trainingMapper = trainingMapper;
    this.topicMapper = topicMapper;
    this.currentUser = currentUser;
    this.noticeService = noticeService;
    this.audioDecoder = audioDecoder;
    this.audioJoiner = audioJoiner;
    this.speechRecognizer = speechRecognizer;
    this.bailianContentJudge = bailianContentJudge;
    this.transaction = new TransactionTemplate(transactions);
    this.statusTransaction = new TransactionTemplate(transactions);
    this.statusTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  public SessionState start(long topicId) {
    long userId = currentUser.requireId();
    requireTopic(topicId);
    SessionRow active = trainingMapper.findActive(userId, topicId);
    if (active != null) {
      return toState(active);
    }
    SessionWrite session = new SessionWrite();
    session.userId = userId;
    session.topicId = topicId;
    session.startedAt = LocalDateTime.now().withNano(0);
    trainingMapper.insertOpenSession(session);
    if (session.sessionId == null) {
      throw new IllegalStateException("未能开始这次训练。");
    }
    return new SessionState(session.sessionId, topicId, "recording", 0, false, null);
  }

  public SessionState findActive(long topicId) {
    SessionRow row = trainingMapper.findActive(currentUser.requireId(), topicId);
    return row == null ? null : toState(row);
  }

  public SessionState pause(long sessionId, int durationSeconds) {
    SessionRow row = requiredSession(sessionId);
    SessionTransitions.pause(row.status);
    mark(sessionId, "paused", null, null, clampDuration(durationSeconds), "recording");
    return toState(requiredSession(sessionId));
  }

  public SessionState resume(long sessionId) {
    SessionRow row = requiredSession(sessionId);
    SessionTransitions.resume(row.status);
    mark(sessionId, "recording", null, null, null, "paused");
    return toState(requiredSession(sessionId));
  }

  public void saveDraft(
      long sessionId,
      int durationSeconds,
      byte[] audio,
      String mimeType,
      byte[] prefix,
      String prefixMime) {
    SessionRow row = requiredSession(sessionId);
    SessionTransitions.analyze(row.status);
    Accepted accepted = incoming(audio, mimeType, prefix, prefixMime);
    trainingMapper.upsertAudio(
        sessionId, accepted.mimeType(), accepted.bytes().length, accepted.bytes());
    trainingMapper.updateStatus(
        currentUser.requireId(),
        sessionId,
        row.status,
        row.failReason,
        null,
        clampDuration(durationSeconds),
        row.status);
  }

  public CreatedSession finish(long sessionId, Integer durationSeconds, byte[] audio, String mimeType) {
    return finish(sessionId, durationSeconds, audio, mimeType, null, null);
  }

  public CreatedSession finish(
      long sessionId,
      Integer durationSeconds,
      byte[] audio,
      String mimeType,
      byte[] prefix,
      String prefixMime) {
    SessionRow row = requiredSession(sessionId);
    SessionTransitions.analyze(row.status);
    Accepted accepted =
        audio == null ? storedAudio(sessionId) : incoming(audio, mimeType, prefix, prefixMime);
    mark(sessionId, "analyzing", null, LocalDateTime.now().withNano(0), null, "recording,paused,failed,analyzing");
    try {
      return transaction.execute(status -> complete(sessionId, row.topicId, durationSeconds, accepted));
    } catch (RuntimeException exception) {
      mark(
          sessionId,
          "failed",
          reason(exception),
          LocalDateTime.now().withNano(0),
          null,
          "analyzing");
      if (exception instanceof BadRequestException badRequest) {
        throw badRequest;
      }
      throw new BadRequestException("分析没有完成，可以重试。");
    }
  }

  public void discard(long sessionId) {
    SessionRow row = requiredSession(sessionId);
    SessionTransitions.discard(row.status);
    if (trainingMapper.discardSession(currentUser.requireId(), sessionId) == 0) {
      throw new BadRequestException("这次训练不能放弃。");
    }
  }

  public AudioRow audio(long sessionId) {
    requiredSession(sessionId);
    AudioRow audio = trainingMapper.findSessionAudio(currentUser.requireId(), sessionId);
    if (audio == null || audio.audioData == null || audio.audioData.length == 0) {
      throw new NotFoundException("这次训练还没有保存录音。");
    }
    return audio;
  }

  private CreatedSession complete(long sessionId, long topicId, Integer durationSeconds, Accepted accepted) {
    long userId = currentUser.requireId();
    TopicRow topic = requireTopic(topicId);
    byte[] pcm = audioDecoder.toPcm(accepted.bytes(), accepted.mimeType());
    String transcript = speechRecognizer.transcribe(pcm);
    double audioSeconds = PauseDetector.durationSeconds(pcm);
    int measuredPauses = PauseDetector.countPauses(pcm);
    PreviousRow previous = trainingMapper.findLatestReportForTopic(userId, topicId);
    SpeechAssessment.Result analysis =
        SpeechAssessment.assess(
            transcript, measuredPauses, audioSeconds, topic.category, comparable(previous));
    Result content = judgeContent(topic, transcript, points(topicId));
    int storedDuration =
        audioSeconds > 0
            ? clampDuration(audioSeconds)
            : clampDuration(durationSeconds == null ? 0 : durationSeconds);
    LocalDateTime endedAt = LocalDateTime.now().withNano(0);

    ReportWrite report = new ReportWrite();
    report.sessionId = sessionId;
    report.totalScore = analysis.totalScore();
    report.speechRate = analysis.speechRate();
    report.pauseCount = analysis.pauseCount();
    report.fluencyScore = analysis.fluencyScore();
    report.resultLabel = analysis.resultLabel();
    report.transcript = analysis.transcript();
    report.contentScore = content.score();
    report.contentSummary = limit(content.summary(), 500);
    report.analyzedAt = endedAt;
    trainingMapper.insertReport(report);
    if (report.reportId == null) {
      throw new IllegalStateException("未能写入分析报告。");
    }
    analysis
        .feedbacks()
        .forEach(
            line ->
                trainingMapper.insertFeedback(
                    report.reportId, line.type(), line.content(), line.sortOrder()));
    trainingMapper.upsertAudio(
        sessionId, accepted.mimeType(), accepted.bytes().length, accepted.bytes());
    int completed =
        trainingMapper.updateStatus(
            userId, sessionId, "completed", null, endedAt, storedDuration, "analyzing");
    if (completed != 1) {
      throw new IllegalStateException("未能完成这次训练。");
    }
    noticeService.onReportCreated(userId, analysis.totalScore());
    return new CreatedSession(report.reportId, sessionId);
  }

  private Result judgeContent(TopicRow topic, String transcript, List<Point> checkpoints) {
    Result local = ContentAssessment.assess(transcript, checkpoints);
    if (transcript == null || transcript.isBlank() || checkpoints.isEmpty()) {
      return local;
    }
    try {
      Result judged = bailianContentJudge.judge(topic.title, topic.guide, checkpoints, transcript);
      if (judged != null) {
        return judged;
      }
    } catch (RuntimeException exception) {
      log.warn("百炼内容评价失败，改用关键词对照");
    }
    return new Result(local.score(), limit("本次按关键词对照。" + local.summary(), 500));
  }

  private List<Point> points(long topicId) {
    return trainingMapper.listCheckpoints(topicId).stream()
        .map(row -> new Point(row.label, ContentAssessment.keywords(row.keywords)))
        .toList();
  }

  private Accepted incoming(byte[] audio, String mimeType, byte[] prefix, String prefixMime) {
    Accepted segment = AudioUpload.accept(audio, mimeType);
    if (prefix == null || prefix.length == 0) {
      return segment;
    }
    Accepted earlier = AudioUpload.accept(prefix, prefixMime);
    byte[] joined =
        audioJoiner.join(earlier.bytes(), earlier.mimeType(), segment.bytes(), segment.mimeType());
    return AudioUpload.accept(joined, "audio/webm");
  }

  private Accepted storedAudio(long sessionId) {
    AudioRow audio = trainingMapper.findSessionAudio(currentUser.requireId(), sessionId);
    if (audio == null || audio.audioData == null || audio.audioData.length == 0) {
      throw new BadRequestException("还没有可分析的录音。");
    }
    return AudioUpload.accept(audio.audioData, audio.mimeType);
  }

  private void mark(
      long sessionId,
      String status,
      String failReason,
      LocalDateTime endedAt,
      Integer durationSeconds,
      String allowed) {
    statusTransaction.executeWithoutResult(
        tx -> {
          int updated =
              trainingMapper.updateStatus(
                  currentUser.requireId(),
                  sessionId,
                  status,
                  failReason,
                  endedAt,
                  durationSeconds,
                  allowed);
          if (updated != 1) {
            throw new BadRequestException("当前状态不能这样更改。");
          }
        });
  }

  private SessionRow requiredSession(long sessionId) {
    SessionRow row = trainingMapper.findSession(currentUser.requireId(), sessionId);
    if (row == null || row.status == null) {
      throw new NotFoundException("没有找到这次训练。");
    }
    return row;
  }

  private TopicRow requireTopic(long topicId) {
    TopicRow topic = topicMapper.findTopic(topicId);
    if (topic == null) {
      throw new NotFoundException("没有找到这道题目。");
    }
    return topic;
  }

  private static SessionState toState(SessionRow row) {
    return new SessionState(
        row.sessionId,
        row.topicId,
        row.status,
        row.durationSeconds == null ? 0 : row.durationSeconds,
        row.hasAudio != null && row.hasAudio != 0,
        row.failReason);
  }

  private static Previous comparable(PreviousRow previous) {
    if (previous == null
        || previous.speechRate == null
        || previous.pauseCount == null
        || !SpeechAssessment.RESULT_LABEL.equals(previous.resultLabel)) {
      return null;
    }
    return new Previous(previous.speechRate, previous.pauseCount);
  }

  private static int clampDuration(double seconds) {
    return Math.max(0, Math.min(1800, (int) Math.round(seconds)));
  }

  private static String reason(RuntimeException exception) {
    String message = exception instanceof BadRequestException ? exception.getMessage() : "分析没有完成，可以重试。";
    if (message == null || message.isBlank()) {
      return "分析没有完成，可以重试。";
    }
    return message.length() <= 120 ? message : message.substring(0, 120);
  }

  private static String limit(String text, int max) {
    if (text == null || text.length() <= max) {
      return text;
    }
    return text.substring(0, max);
  }
}
