package com.voxinsight.web;

import com.voxinsight.dto.HelpResponse;
import com.voxinsight.service.HelpService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelpController {

  private final HelpService helpService;

  public HelpController(HelpService helpService) {
    this.helpService = helpService;
  }

  @GetMapping("/api/help")
  public HelpResponse help() {
    return helpService.load();
  }
}
