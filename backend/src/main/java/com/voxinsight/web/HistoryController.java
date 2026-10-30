package com.voxinsight.web;

import com.voxinsight.dto.HistoryResponse;
import com.voxinsight.service.HistoryService;
import com.voxinsight.service.ProfileService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HistoryController {

  private final HistoryService historyService;
  private final ProfileService profileService;

  public HistoryController(HistoryService historyService, ProfileService profileService) {
    this.historyService = historyService;
    this.profileService = profileService;
  }

  @GetMapping("/api/history")
  public HistoryResponse history() {
    return historyService.load();
  }

  @DeleteMapping("/api/history")
  public void clear() {
    profileService.clearHistory();
  }
}
