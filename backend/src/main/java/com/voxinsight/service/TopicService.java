package com.voxinsight.service;

import com.voxinsight.service.CurrentUser;
import com.voxinsight.dto.FavoriteResponse;
import com.voxinsight.dto.TopicCard;
import com.voxinsight.dto.TopicListResponse;
import com.voxinsight.mapper.TopicMapper;
import com.voxinsight.model.Rows.TopicRow;
import com.voxinsight.web.NotFoundException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TopicService {

  private final TopicMapper topicMapper;
  private final CurrentUser currentUser;

  public TopicService(TopicMapper topicMapper, CurrentUser currentUser) {
    this.topicMapper = topicMapper;
    this.currentUser = currentUser;
  }

  public TopicListResponse list(String category, String keyword) {
    List<String> categories = new ArrayList<>();
    categories.add("全部");
    categories.addAll(topicMapper.listCategories());
    List<TopicCard> topics =
        topicMapper.listTopics(currentUser.requireId(), category, keyword).stream()
            .map(TopicService::toCard)
            .toList();
    return new TopicListResponse(categories, topics);
  }

  @Transactional
  public FavoriteResponse toggleFavorite(long topicId) {
    long userId = currentUser.requireId();
    if (topicMapper.findTopic(topicId) == null) {
      throw new NotFoundException("没有找到这道题目。");
    }
    boolean favorite = topicMapper.countFavorite(userId, topicId) > 0;
    if (favorite) {
      topicMapper.deleteFavorite(userId, topicId);
    } else {
      topicMapper.insertFavorite(userId, topicId);
    }
    return new FavoriteResponse(topicId, !favorite);
  }

  static TopicCard toCard(TopicRow row) {
    return new TopicCard(
        row.id,
        row.category,
        row.title,
        row.difficulty,
        row.durationMin,
        row.durationMax,
        row.guide,
        row.favorite != null && row.favorite != 0);
  }
}
