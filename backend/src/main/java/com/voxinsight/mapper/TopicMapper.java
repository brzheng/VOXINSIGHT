package com.voxinsight.mapper;

import com.voxinsight.model.Rows.TopicRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TopicMapper {

  List<String> listCategories();

  List<TopicRow> listTopics(
      @Param("userId") long userId,
      @Param("category") String category,
      @Param("keyword") String keyword);

  TopicRow findTopic(@Param("topicId") long topicId);

  int countFavorite(@Param("userId") long userId, @Param("topicId") long topicId);

  int insertFavorite(@Param("userId") long userId, @Param("topicId") long topicId);

  int deleteFavorite(@Param("userId") long userId, @Param("topicId") long topicId);
}
