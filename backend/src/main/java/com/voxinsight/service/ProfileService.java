package com.voxinsight.service;

import com.voxinsight.dto.AnonymousSummary;
import com.voxinsight.dto.FeedbackItem;
import com.voxinsight.dto.GoalResponse;
import com.voxinsight.dto.ProfileResponse;
import com.voxinsight.dto.ProfileResponse.ProfileUser;
import com.voxinsight.dto.ProfileUpdateRequest;
import com.voxinsight.dto.SettingsResponse;
import com.voxinsight.mapper.ProfileMapper;
import com.voxinsight.mapper.TrainingMapper;
import com.voxinsight.model.Rows.AvatarRow;
import com.voxinsight.model.Rows.DashboardRow;
import com.voxinsight.model.Rows.SettingsRow;
import com.voxinsight.model.Rows.SummaryRow;
import com.voxinsight.model.Rows.UserRow;
import com.voxinsight.web.BadRequestException;
import com.voxinsight.web.NotFoundException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
  private static final DateTimeFormatter REPLY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private static final Set<String> AVATAR_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
  private static final Set<String> FEEDBACK_CATEGORIES = Set.of("功能建议", "问题反馈", "其他");

  private final ProfileMapper profileMapper;
  private final TrainingMapper trainingMapper;
  private final CurrentUser currentUser;

  public ProfileService(
      ProfileMapper profileMapper, TrainingMapper trainingMapper, CurrentUser currentUser) {
    this.profileMapper = profileMapper;
    this.trainingMapper = trainingMapper;
    this.currentUser = currentUser;
  }

  public ProfileResponse load() {
    long userId = currentUser.requireId();
    UserRow user = profileMapper.findUser(userId);
    DashboardRow dashboard = trainingMapper.findDashboard(userId);
    if (user == null || dashboard == null) {
      throw new NotFoundException("没有找到这个用户。");
    }
    return new ProfileResponse(
        new ProfileUser(
            user.nickname,
            user.loginName,
            HomeService.initial(user.nickname),
            user.school,
            user.major,
            user.bio,
            user.hasAvatar != null && user.hasAvatar != 0),
        HistoryService.stats(trainingMapper.findStats(userId)),
        dashboard.weeklyCompletedCount == null ? 0 : dashboard.weeklyCompletedCount,
        dashboard.weeklyTargetCount == null ? 3 : dashboard.weeklyTargetCount);
  }

  @Transactional
  public ProfileResponse update(ProfileUpdateRequest request) {
    String nickname = request.nickname().trim();
    if (nickname.isEmpty()) {
      throw new BadRequestException("请填写昵称。");
    }
    int updated =
        profileMapper.updateProfile(
            currentUser.requireId(),
            nickname,
            blankToNull(request.school()),
            blankToNull(request.major()),
            blankToNull(request.bio()));
    if (updated == 0) {
      throw new NotFoundException("没有找到这个用户。");
    }
    return load();
  }

  @Transactional
  public void updateAvatar(byte[] bytes, String mimeType) {
    if (bytes == null || bytes.length == 0 || bytes.length > 512 * 1024) {
      throw new BadRequestException("头像需要是 512KB 以内的图片。");
    }
    String mime = mimeType == null ? "" : mimeType.split(";", 2)[0].trim().toLowerCase();
    if (!AVATAR_TYPES.contains(mime)) {
      throw new BadRequestException("头像只支持 JPG、PNG 或 WebP。");
    }
    profileMapper.updateAvatar(currentUser.requireId(), mime, bytes);
  }

  public AvatarRow avatar() {
    AvatarRow row = profileMapper.findAvatar(currentUser.requireId());
    if (row == null || row.data == null || row.data.length == 0) {
      throw new NotFoundException("还没有上传头像。");
    }
    return row;
  }

  public SettingsResponse settings() {
    return toSettings(requiredSettings(currentUser.requireId()));
  }

  @Transactional
  public GoalResponse updateGoal(int targetCount) {
    int updated = profileMapper.updateGoal(currentUser.requireId(), targetCount);
    if (updated == 0) {
      throw new NotFoundException("没有找到这个用户。");
    }
    return new GoalResponse(targetCount);
  }

  @Transactional
  public SettingsResponse updateSettings(SettingsResponse request) {
    long userId = currentUser.requireId();
    LocalTime reminderTime = parseTime(request.reminderTime());
    int notices =
        profileMapper.updateNotification(
            userId,
            bit(request.reminderOn()),
            bit(request.reportNoticeOn()),
            bit(request.goalNoticeOn()),
            reminderTime);
    int privacy =
        profileMapper.updatePrivacy(userId, bit(request.keepHistory()), bit(request.anonymousStats()));
    if (notices == 0 || privacy == 0) {
      throw new NotFoundException("没有找到这个用户。");
    }
    return request;
  }

  public AnonymousSummary anonymousSummary() {
    SummaryRow row = profileMapper.anonymousSummary();
    if (row == null) {
      return new AnonymousSummary(0, 0);
    }
    return new AnonymousSummary(
        row.participants == null ? 0 : row.participants,
        row.weeklySessions == null ? 0 : row.weeklySessions);
  }

  @Transactional
  public void clearHistory() {
    trainingMapper.deleteHistory(currentUser.requireId());
  }

  @Transactional
  public void submitFeedback(String category, String content) {
    if (!FEEDBACK_CATEGORIES.contains(category)) {
      throw new BadRequestException("请选择反馈类别。");
    }
    String text = content == null ? "" : content.trim();
    if (text.isEmpty()) {
      throw new BadRequestException("请填写反馈内容。");
    }
    profileMapper.insertFeedback(currentUser.requireId(), category, text);
  }

  public List<FeedbackItem> feedbacks() {
    return profileMapper.listFeedback(currentUser.requireId()).stream()
        .map(
            row ->
                new FeedbackItem(
                    row.category,
                    row.content,
                    row.status,
                    row.createdAt == null ? "" : row.createdAt.toLocalDate().toString(),
                    row.replyContent == null ? "" : row.replyContent,
                    stamp(row.repliedAt)))
        .toList();
  }

  private static String stamp(LocalDateTime time) {
    return time == null ? "" : time.format(REPLY_TIME);
  }

  private SettingsRow requiredSettings(long userId) {
    SettingsRow row = profileMapper.findSettings(userId);
    if (row == null) {
      throw new NotFoundException("没有找到这个用户。");
    }
    return row;
  }

  private static SettingsResponse toSettings(SettingsRow row) {
    String time = row.reminderTime == null ? "20:00" : row.reminderTime.format(TIME);
    return new SettingsResponse(
        flag(row.reminderOn),
        flag(row.reportNoticeOn),
        flag(row.goalNoticeOn),
        time,
        flag(row.keepHistory),
        flag(row.anonymousStats));
  }

  private static LocalTime parseTime(String value) {
    String text = value == null || value.isBlank() ? "20:00" : value.trim();
    if (text.length() < 5) {
      throw new BadRequestException("提醒时间不正确。");
    }
    try {
      return LocalTime.parse(text.substring(0, 5), TIME);
    } catch (DateTimeParseException exception) {
      throw new BadRequestException("提醒时间不正确。");
    }
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private static boolean flag(Integer value) {
    return value != null && value != 0;
  }

  private static int bit(boolean value) {
    return value ? 1 : 0;
  }
}
