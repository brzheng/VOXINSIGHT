package com.voxinsight.mapper;

import com.voxinsight.model.AdminRows.CategoryRow;
import com.voxinsight.model.AdminRows.CheckpointRow;
import com.voxinsight.model.AdminRows.FeedbackRow;
import com.voxinsight.model.AdminRows.OverviewRow;
import com.voxinsight.model.AdminRows.SessionDetailRow;
import com.voxinsight.model.AdminRows.SessionRow;
import com.voxinsight.model.AdminRows.TopicRow;
import com.voxinsight.model.AdminRows.TopicWrite;
import com.voxinsight.model.AdminRows.UserRow;
import com.voxinsight.model.Rows.AudioRow;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminMapper {

  OverviewRow overview();

  List<UserRow> listUsers(@Param("keyword") String keyword);

  String findUserRole(@Param("id") long id);

  int updateUser(
      @Param("id") long id,
      @Param("nickname") String nickname,
      @Param("school") String school,
      @Param("major") String major,
      @Param("bio") String bio,
      @Param("role") String role);

  int deleteUser(@Param("id") long id);

  List<CategoryRow> listCategories();

  Long findCategoryId(@Param("id") long id);

  int insertCategory(@Param("name") String name);

  List<TopicRow> listTopics();

  TopicRow findTopic(@Param("id") long id);

  List<CheckpointRow> listCheckpoints(@Param("topicId") long topicId);

  int countSessionsByTopic(@Param("topicId") long topicId);

  int clearRecommended();

  int insertTopic(TopicWrite topic);

  int updateTopic(TopicWrite topic);

  int deleteCheckpoints(@Param("topicId") long topicId);

  int insertCheckpoint(
      @Param("topicId") long topicId,
      @Param("label") String label,
      @Param("keywords") String keywords,
      @Param("sortOrder") int sortOrder);

  int deleteTopic(@Param("id") long id);

  List<SessionRow> listSessions(@Param("status") String status);

  SessionDetailRow findSession(@Param("id") long id);

  AudioRow findAudio(@Param("id") long id);

  int deleteSession(@Param("id") long id);

  List<FeedbackRow> listFeedbacks(@Param("status") String status);

  int updateFeedback(
      @Param("id") long id,
      @Param("status") String status,
      @Param("reply") String reply,
      @Param("repliedAt") LocalDateTime repliedAt);

  FeedbackRow findFeedback(@Param("id") long id);
}
