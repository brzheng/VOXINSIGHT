package com.voxinsight.web;

import com.voxinsight.dto.FavoriteResponse;
import com.voxinsight.dto.TopicListResponse;
import com.voxinsight.service.TopicService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TopicController {

  private final TopicService topicService;

  public TopicController(TopicService topicService) {
    this.topicService = topicService;
  }

  @GetMapping("/api/topics")
  public TopicListResponse topics(
      @RequestParam(required = false) String category, @RequestParam(required = false) String keyword) {
    return topicService.list(category, keyword);
  }

  @PostMapping("/api/topics/{id}/favorite")
  public FavoriteResponse favorite(@PathVariable("id") long id) {
    return topicService.toggleFavorite(id);
  }
}
