package com.voxinsight.service;

import com.voxinsight.dto.HomeResponse;
import com.voxinsight.dto.HomeResponse.Brand;
import com.voxinsight.dto.HomeResponse.LatestTraining;
import com.voxinsight.dto.HomeResponse.RecommendedTopic;
import com.voxinsight.dto.HomeResponse.UserBrief;
import com.voxinsight.mapper.TrainingMapper;
import com.voxinsight.model.Rows.DashboardRow;
import com.voxinsight.model.Rows.ReportRow;
import com.voxinsight.model.Rows.ScoreRow;
import com.voxinsight.model.Rows.TopicRow;
import com.voxinsight.web.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class HomeService {

  private final TrainingMapper trainingMapper;
  private final CurrentUser currentUser;
  private final NoticeService noticeService;

  public HomeService(TrainingMapper trainingMapper, CurrentUser currentUser, NoticeService noticeService) {
    this.trainingMapper = trainingMapper;
    this.currentUser = currentUser;
    this.noticeService = noticeService;
  }

  public HomeResponse load() {
    long userId = currentUser.requireId();
    noticeService.refreshPracticeReminder(userId);
    DashboardRow dashboard = trainingMapper.findDashboard(userId);
    if (dashboard == null) {
      throw new NotFoundException("没有找到这个用户。");
    }
    return new HomeResponse(
        new Brand(dashboard.brandNameZh, dashboard.brandNameEn, dashboard.slogan),
        new UserBrief(
            dashboard.nickname, initial(dashboard.nickname), dashboard.hasAvatar != null && dashboard.hasAvatar != 0),
        value(dashboard.weeklyCompletedCount),
        dashboard.weeklyTargetCount == null ? 3 : dashboard.weeklyTargetCount,
        latest(userId, dashboard.latestReportId),
        previousScore(userId),
        recommended(trainingMapper.findRecommended()),
        noticeService.unread(userId));
  }

  private LatestTraining latest(long userId, Long reportId) {
    if (reportId == null) {
      return null;
    }
    ReportRow report = trainingMapper.findReport(userId, reportId);
    if (report == null) {
      return null;
    }
    return new LatestTraining(
        report.id,
        report.topicId,
        report.title,
        DisplayTime.dateLabel(report.endedAt),
        DisplayTime.durationLabel(report.durationSeconds),
        report.score);
  }

  private Integer previousScore(long userId) {
    ScoreRow row = trainingMapper.findPreviousScore(userId);
    return row == null ? null : row.totalScore;
  }

  private static RecommendedTopic recommended(TopicRow topic) {
    if (topic == null) {
      return null;
    }
    return new RecommendedTopic(
        topic.id, topic.title, topic.category, topic.difficulty, topic.durationMin, topic.durationMax);
  }

  static String initial(String nickname) {
    if (nickname == null || nickname.isEmpty()) {
      return "";
    }
    return nickname.substring(0, 1);
  }

  private static int value(Integer number) {
    return number == null ? 0 : number;
  }
}
