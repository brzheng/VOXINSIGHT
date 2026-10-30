package com.voxinsight.service;

import com.voxinsight.dto.NoticeItem;
import com.voxinsight.mapper.NoticeMapper;
import com.voxinsight.mapper.ProfileMapper;
import com.voxinsight.model.Rows.SettingsRow;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NoticeService {

  private final NoticeMapper noticeMapper;
  private final ProfileMapper profileMapper;

  public NoticeService(NoticeMapper noticeMapper, ProfileMapper profileMapper) {
    this.noticeMapper = noticeMapper;
    this.profileMapper = profileMapper;
  }

  public void refreshPracticeReminder(long userId) {
    SettingsRow settings = profileMapper.findSettings(userId);
    if (settings == null || settings.reminderOn == null || settings.reminderOn == 0) {
      return;
    }
    LocalTime reminder = settings.reminderTime == null ? LocalTime.of(20, 0) : settings.reminderTime;
    if (LocalTime.now().isBefore(reminder) || noticeMapper.countTodayPractice(userId) > 0) {
      return;
    }
    if (noticeMapper.countNoticeSince(userId, "practice", LocalDate.now().atStartOfDay()) > 0) {
      return;
    }
    noticeMapper.insertNotice(userId, "practice", "今天的练习时间到了，还没有完成训练。", LocalDateTime.now());
  }

  public void onReportCreated(long userId, int score) {
    SettingsRow settings = profileMapper.findSettings(userId);
    if (settings == null) {
      return;
    }
    if (settings.reportNoticeOn != null && settings.reportNoticeOn != 0) {
      noticeMapper.insertNotice(userId, "report", "分析报告已生成，总分 " + score + "。", LocalDateTime.now());
    }
    if (settings.goalNoticeOn == null || settings.goalNoticeOn == 0) {
      return;
    }
    LocalDate today = LocalDate.now();
    LocalDateTime weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1L).atStartOfDay();
    int done = noticeMapper.countThisWeek(userId, weekStart);
    int target = profileMapper.findWeeklyTarget(userId);
    if (done >= target && noticeMapper.countNoticeSince(userId, "goal", weekStart) == 0) {
      noticeMapper.insertNotice(userId, "goal", "本周练习目标已完成，共 " + done + " 次。", LocalDateTime.now());
    }
  }

  public List<NoticeItem> unread(long userId) {
    return noticeMapper.listUnread(userId).stream()
        .map(row -> new NoticeItem(row.id, row.content))
        .toList();
  }

  public void markRead(long userId, long noticeId) {
    noticeMapper.markRead(userId, noticeId);
  }
}
