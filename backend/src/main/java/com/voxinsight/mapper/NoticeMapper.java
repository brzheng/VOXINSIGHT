package com.voxinsight.mapper;

import com.voxinsight.model.Rows.NoticeRow;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NoticeMapper {

  int countTodayPractice(@Param("userId") long userId);

  int countThisWeek(@Param("userId") long userId, @Param("weekStart") LocalDateTime weekStart);

  int countNoticeSince(
      @Param("userId") long userId,
      @Param("noticeType") String noticeType,
      @Param("since") LocalDateTime since);

  int insertNotice(
      @Param("userId") long userId,
      @Param("noticeType") String noticeType,
      @Param("content") String content,
      @Param("createdAt") LocalDateTime createdAt);

  List<NoticeRow> listUnread(@Param("userId") long userId);

  int markRead(@Param("userId") long userId, @Param("noticeId") long noticeId);
}
