package com.voxinsight.web;

import com.voxinsight.service.CurrentUser;
import com.voxinsight.service.NoticeService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NoticeController {

  private final NoticeService noticeService;
  private final CurrentUser currentUser;

  public NoticeController(NoticeService noticeService, CurrentUser currentUser) {
    this.noticeService = noticeService;
    this.currentUser = currentUser;
  }

  @PostMapping("/api/notices/{id}/read")
  public void read(@PathVariable("id") long id) {
    noticeService.markRead(currentUser.requireId(), id);
  }
}
