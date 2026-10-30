package com.voxinsight.web;

import com.voxinsight.dto.HomeResponse;
import com.voxinsight.service.HomeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

  private final HomeService homeService;

  public HomeController(HomeService homeService) {
    this.homeService = homeService;
  }

  @GetMapping("/api/home")
  public HomeResponse home() {
    return homeService.load();
  }
}
