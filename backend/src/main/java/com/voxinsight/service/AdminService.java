package com.voxinsight.service;

import com.voxinsight.dto.AdminCategory;
import com.voxinsight.dto.AdminCheckpoint;
import com.voxinsight.dto.AdminFeedback;
import com.voxinsight.dto.AdminFeedbackUpdate;
import com.voxinsight.dto.AdminOverview;
import com.voxinsight.dto.AdminSession;
import com.voxinsight.dto.AdminSessionDetail;
import com.voxinsight.dto.AdminTopic;
import com.voxinsight.dto.AdminTopicDetail;
import com.voxinsight.dto.AdminTopicRequest;
import com.voxinsight.dto.AdminUser;
import com.voxinsight.dto.AdminUserUpdate;
import com.voxinsight.dto.AudioFile;
import com.voxinsight.mapper.AdminMapper;
import com.voxinsight.mapper.AuthMapper;
import com.voxinsight.model.AdminRows.CategoryRow;
import com.voxinsight.model.AdminRows.FeedbackRow;
import com.voxinsight.model.AdminRows.OverviewRow;
import com.voxinsight.model.AdminRows.SessionDetailRow;
import com.voxinsight.model.AdminRows.SessionRow;
import com.voxinsight.model.AdminRows.TopicRow;
import com.voxinsight.model.AdminRows.TopicWrite;
import com.voxinsight.model.AdminRows.UserRow;
import com.voxinsight.model.Rows.AudioRow;
import com.voxinsight.web.BadRequestException;
import com.voxinsight.web.ForbiddenException;
import com.voxinsight.web.NotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

  private static final Set<String> DIFFICULTIES = Set.of("入门", "进阶", "挑战");
  private static final Set<String> ROLES = Set.of("user", "admin");
  private static final Set<String> SESSION_STATUSES =
      Set.of("pending", "recording", "paused", "analyzing", "completed", "failed");
  private static final Set<String> FEEDBACK_STATUSES = Set.of("待处理", "已回复");

  private final AdminMapper adminMapper;
  private final AuthMapper authMapper;
  private final CurrentUser currentUser;

  public AdminService(AdminMapper adminMapper, AuthMapper authMapper, CurrentUser currentUser) {
    this.adminMapper = adminMapper;
    this.authMapper = authMapper;
    this.currentUser = currentUser;
  }

  public AdminOverview overview() {
    requireAdmin();
    OverviewRow row = adminMapper.overview();
    if (row == null) {
      return new AdminOverview(0, 0, 0, 0);
    }
    return new AdminOverview(row.userCount, row.topicCount, row.sessionCount, row.pendingFeedbackCount);
  }

  public List<AdminUser> users(String keyword) {
    requireAdmin();
    return adminMapper.listUsers(like(keyword)).stream().map(AdminService::user).toList();
  }

  @Transactional
  public AdminUser updateUser(long id, AdminUserUpdate request) {
    requireAdmin();
    if (request == null) {
      throw new BadRequestException("请求参数不正确。");
    }
    String role = text(request.role());
    if (!ROLES.contains(role)) {
      throw new BadRequestException("请选择学员或管理员。");
    }
    if (id == currentUser.requireId() && !"admin".equals(role)) {
      throw new BadRequestException("不能取消自己的管理权限。");
    }
    if (adminMapper.findUserRole(id) == null) {
      throw new NotFoundException("没有找到这个用户。");
    }
    String nickname = required(request.nickname(), "昵称", 32);
    adminMapper.updateUser(
        id, nickname, optional(request.school(), 64, "学校"), optional(request.major(), 64, "专业"),
        optional(request.bio(), 120, "简介"), role);
    return users(null).stream().filter(item -> item.id() == id).findFirst().orElseThrow();
  }

  @Transactional
  public void deleteUser(long id) {
    requireAdmin();
    if (id == currentUser.requireId()) {
      throw new BadRequestException("不能删除当前登录的管理员。");
    }
    if (adminMapper.deleteUser(id) == 0) {
      throw new NotFoundException("没有找到这个用户。");
    }
  }

  public List<AdminCategory> categories() {
    requireAdmin();
    return adminMapper.listCategories().stream().map(AdminService::category).toList();
  }

  @Transactional
  public AdminCategory createCategory(String name) {
    requireAdmin();
    String title = required(name, "分类名称", 32);
    try {
      adminMapper.insertCategory(title);
    } catch (DuplicateKeyException exception) {
      throw new BadRequestException("这个分类已经存在。");
    }
    return adminMapper.listCategories().stream()
        .filter(item -> title.equals(item.name))
        .map(AdminService::category)
        .findFirst()
        .orElseThrow(() -> new BadRequestException("分类没有保存成功。"));
  }

  public List<AdminTopic> topics() {
    requireAdmin();
    return adminMapper.listTopics().stream().map(AdminService::topic).toList();
  }

  public AdminTopicDetail topic(long id) {
    requireAdmin();
    return detail(requiredTopic(id));
  }

  @Transactional
  public AdminTopicDetail createTopic(AdminTopicRequest request) {
    requireAdmin();
    TopicWrite write = validate(null, request);
    if (write.recommended == 1) {
      adminMapper.clearRecommended();
    }
    adminMapper.insertTopic(write);
    if (write.id == null) {
      throw new BadRequestException("题目没有保存成功。");
    }
    replaceCheckpoints(write.id, request.checkpoints());
    return detail(requiredTopic(write.id));
  }

  @Transactional
  public AdminTopicDetail updateTopic(long id, AdminTopicRequest request) {
    requireAdmin();
    requiredTopic(id);
    TopicWrite write = validate(id, request);
    if (write.recommended == 1) {
      adminMapper.clearRecommended();
    }
    adminMapper.updateTopic(write);
    replaceCheckpoints(id, request.checkpoints());
    return detail(requiredTopic(id));
  }

  @Transactional
  public void deleteTopic(long id) {
    requireAdmin();
    requiredTopic(id);
    if (adminMapper.countSessionsByTopic(id) > 0) {
      throw new BadRequestException("已有训练记录，不能删除这道题。");
    }
    adminMapper.deleteTopic(id);
  }

  public List<AdminSession> sessions(String status) {
    requireAdmin();
    return adminMapper.listSessions(statusFilter(status)).stream().map(AdminService::session).toList();
  }

  public AdminSessionDetail session(long id) {
    requireAdmin();
    return sessionDetail(requiredSession(id));
  }

  public AudioFile audio(long id) {
    requireAdmin();
    requiredSession(id);
    AudioRow row = adminMapper.findAudio(id);
    if (row == null || row.audioData == null || row.audioData.length == 0) {
      throw new NotFoundException("这次训练没有录音。");
    }
    String mime = row.mimeType == null || row.mimeType.isBlank() ? "application/octet-stream" : row.mimeType;
    return new AudioFile(mime, row.audioData);
  }

  @Transactional
  public void deleteSession(long id) {
    requireAdmin();
    if (adminMapper.deleteSession(id) == 0) {
      throw new NotFoundException("没有找到这条训练记录。");
    }
  }

  public List<AdminFeedback> feedbacks(String status) {
    requireAdmin();
    return adminMapper.listFeedbacks(feedbackFilter(status)).stream().map(AdminService::feedback).toList();
  }

  @Transactional
  public AdminFeedback updateFeedback(long id, AdminFeedbackUpdate request) {
    requireAdmin();
    if (request == null || !FEEDBACK_STATUSES.contains(text(request.status()))) {
      throw new BadRequestException("请选择待处理或已回复。");
    }
    String reply = request.reply() == null ? "" : request.reply().trim();
    LocalDateTime repliedAt = null;
    if ("已回复".equals(request.status())) {
      if (reply.isEmpty()) {
        throw new BadRequestException("请填写回复内容。");
      }
      if (reply.length() > 500) {
        throw new BadRequestException("回复不能超过 500 字。");
      }
      repliedAt = LocalDateTime.now();
    } else {
      reply = null;
    }
    if (adminMapper.updateFeedback(id, request.status(), reply, repliedAt) == 0) {
      throw new NotFoundException("没有找到这条反馈。");
    }
    return feedback(adminMapper.findFeedback(id));
  }

  private void requireAdmin() {
    long id = currentUser.requireId();
    if (!"admin".equals(authMapper.findRole(id))) {
      throw new ForbiddenException("没有管理权限。");
    }
  }

  private TopicRow requiredTopic(long id) {
    TopicRow row = adminMapper.findTopic(id);
    if (row == null) {
      throw new NotFoundException("没有找到这道题目。");
    }
    return row;
  }

  private SessionDetailRow requiredSession(long id) {
    SessionDetailRow row = adminMapper.findSession(id);
    if (row == null) {
      throw new NotFoundException("没有找到这条训练记录。");
    }
    return row;
  }

  private TopicWrite validate(Long id, AdminTopicRequest request) {
    if (request == null) {
      throw new BadRequestException("请求参数不正确。");
    }
    if (adminMapper.findCategoryId(request.categoryId()) == null) {
      throw new BadRequestException("请选择题目分类。");
    }
    if (!DIFFICULTIES.contains(text(request.difficulty()))) {
      throw new BadRequestException("难度只能是入门、进阶或挑战。");
    }
    if (request.durationMin() < 1 || request.durationMax() > 30 || request.durationMax() < request.durationMin()) {
      throw new BadRequestException("建议时长要在 1 到 30 分钟之间，且最长不少于最短。");
    }
    if (request.sortOrder() < 0 || request.sortOrder() > 255) {
      throw new BadRequestException("排序需要在 0 到 255 之间。");
    }
    TopicWrite write = new TopicWrite();
    write.id = id;
    write.categoryId = request.categoryId();
    write.title = required(request.title(), "标题", 120);
    write.difficulty = request.difficulty().trim();
    write.durationMin = request.durationMin();
    write.durationMax = request.durationMax();
    write.guide = required(request.guide(), "录音提示", 120);
    write.recommended = request.recommended() ? 1 : 0;
    write.sortOrder = request.sortOrder();
    checkpoints(request.checkpoints());
    return write;
  }

  private List<AdminCheckpoint> checkpoints(List<AdminCheckpoint> raw) {
    if (raw == null || raw.isEmpty()) {
      throw new BadRequestException("请至少填写一个内容要点。");
    }
    if (raw.size() > 8) {
      throw new BadRequestException("一道题最多 8 个内容要点。");
    }
    List<AdminCheckpoint> points = new ArrayList<>();
    Set<String> labels = new LinkedHashSet<>();
    for (AdminCheckpoint point : raw) {
      if (point == null) {
        throw new BadRequestException("内容要点不完整。");
      }
      String label = required(point.label(), "要点名称", 40);
      String keywords = required(point.keywords(), "关键词", 200);
      if (ContentAssessment.keywords(keywords).isEmpty()) {
        throw new BadRequestException("请为要点「" + label + "」填写关键词。");
      }
      if (!labels.add(label)) {
        throw new BadRequestException("要点名称不能重复。");
      }
      points.add(new AdminCheckpoint(label, keywords));
    }
    return points;
  }

  private void replaceCheckpoints(long topicId, List<AdminCheckpoint> raw) {
    List<AdminCheckpoint> points = checkpoints(raw);
    adminMapper.deleteCheckpoints(topicId);
    int order = 1;
    for (AdminCheckpoint point : points) {
      try {
        adminMapper.insertCheckpoint(topicId, point.label(), point.keywords(), order);
      } catch (DuplicateKeyException exception) {
        throw new BadRequestException("要点名称不能重复。");
      }
      order += 1;
    }
  }

  private AdminTopicDetail detail(TopicRow row) {
    List<AdminCheckpoint> points =
        adminMapper.listCheckpoints(row.id).stream()
            .map(item -> new AdminCheckpoint(item.label, item.keywords))
            .toList();
    return new AdminTopicDetail(
        row.id,
        row.categoryId,
        row.categoryName,
        row.title,
        row.difficulty,
        row.durationMin,
        row.durationMax,
        row.guide,
        row.recommended == 1,
        row.sortOrder,
        points);
  }

  private static String statusFilter(String status) {
    String value = text(status);
    if (value.isEmpty()) {
      return "";
    }
    if (!SESSION_STATUSES.contains(value)) {
      throw new BadRequestException("训练状态不正确。");
    }
    return value;
  }

  private static String feedbackFilter(String status) {
    String value = text(status);
    if (value.isEmpty()) {
      return "";
    }
    if (!FEEDBACK_STATUSES.contains(value)) {
      throw new BadRequestException("反馈状态不正确。");
    }
    return value;
  }

  private static String like(String keyword) {
    String text = text(keyword);
    if (text.isEmpty()) {
      return null;
    }
    if (text.length() > 32) {
      throw new BadRequestException("搜索词太长。");
    }
    return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  private static String required(String value, String label, int max) {
    String text = text(value);
    if (text.isEmpty()) {
      throw new BadRequestException("请填写" + label + "。");
    }
    if (text.length() > max) {
      throw new BadRequestException(label + "不能超过 " + max + " 字。");
    }
    return text;
  }

  private static String optional(String value, int max, String label) {
    String text = text(value);
    if (text.isEmpty()) {
      return null;
    }
    if (text.length() > max) {
      throw new BadRequestException(label + "不能超过 " + max + " 字。");
    }
    return text;
  }

  private static String text(String value) {
    return value == null ? "" : value.trim();
  }

  private static String shown(String value) {
    return value == null ? "" : value;
  }

  private static AdminUser user(UserRow row) {
    return new AdminUser(
        row.id,
        row.loginName,
        row.nickname,
        shown(row.school),
        shown(row.major),
        shown(row.bio),
        row.role,
        shown(row.createdAt),
        row.sessionCount);
  }

  private static AdminCategory category(CategoryRow row) {
    return new AdminCategory(row.id, row.name, row.sortOrder);
  }

  private static AdminTopic topic(TopicRow row) {
    return new AdminTopic(
        row.id,
        row.categoryId,
        row.categoryName,
        row.title,
        row.difficulty,
        row.durationMin,
        row.durationMax,
        row.guide,
        row.recommended == 1,
        row.sortOrder,
        row.checkpointCount);
  }

  private static AdminSession session(SessionRow row) {
    return new AdminSession(
        row.id,
        row.loginName,
        row.nickname,
        row.title,
        row.status,
        shown(row.startedAt),
        shown(row.endedAt),
        row.durationSeconds,
        row.reportId,
        row.score);
  }

  private static AdminSessionDetail sessionDetail(SessionDetailRow row) {
    return new AdminSessionDetail(
        row.id,
        row.loginName,
        row.nickname,
        row.topicId,
        row.title,
        row.status,
        shown(row.startedAt),
        shown(row.endedAt),
        row.durationSeconds,
        shown(row.failReason),
        row.reportId,
        row.score,
        row.speechRate,
        row.pauseCount,
        row.fluency,
        row.contentScore,
        shown(row.transcript),
        shown(row.contentSummary),
        shown(row.resultLabel),
        row.hasAudio == 1);
  }

  private static AdminFeedback feedback(FeedbackRow row) {
    return new AdminFeedback(
        row.id,
        row.loginName,
        row.nickname,
        row.category,
        row.content,
        row.status,
        shown(row.createdAt),
        shown(row.reply),
        shown(row.repliedAt));
  }
}
