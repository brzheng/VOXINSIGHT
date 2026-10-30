package com.voxinsight.mapper;

import com.voxinsight.model.Rows.AvatarRow;
import com.voxinsight.model.Rows.FeedbackRowItem;
import com.voxinsight.model.Rows.SettingsRow;
import com.voxinsight.model.Rows.SummaryRow;
import com.voxinsight.model.Rows.UserRow;
import java.time.LocalTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProfileMapper {

  UserRow findUser(@Param("userId") long userId);

  SettingsRow findSettings(@Param("userId") long userId);

  int updateGoal(@Param("userId") long userId, @Param("targetCount") int targetCount);

  int updateNotification(
      @Param("userId") long userId,
      @Param("reminderOn") int reminderOn,
      @Param("reportNoticeOn") int reportNoticeOn,
      @Param("goalNoticeOn") int goalNoticeOn,
      @Param("reminderTime") LocalTime reminderTime);

  int updatePrivacy(
      @Param("userId") long userId,
      @Param("keepHistory") int keepHistory,
      @Param("anonymousStats") int anonymousStats);

  int findWeeklyTarget(@Param("userId") long userId);

  int updateProfile(
      @Param("userId") long userId,
      @Param("nickname") String nickname,
      @Param("school") String school,
      @Param("major") String major,
      @Param("bio") String bio);

  int updateAvatar(
      @Param("userId") long userId, @Param("mimeType") String mimeType, @Param("avatarData") byte[] avatarData);

  AvatarRow findAvatar(@Param("userId") long userId);

  SummaryRow anonymousSummary();

  int insertFeedback(
      @Param("userId") long userId, @Param("category") String category, @Param("content") String content);

  List<FeedbackRowItem> listFeedback(@Param("userId") long userId);
}
