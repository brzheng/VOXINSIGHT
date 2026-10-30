package com.voxinsight.web;

import com.voxinsight.mapper.HealthMapper;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

  private final HealthMapper healthMapper;

  public HealthController(HealthMapper healthMapper) {
    this.healthMapper = healthMapper;
  }

  @GetMapping("/api/health")
  public Map<String, String> health() {
    healthMapper.countUsers();
    return Map.of("status", "ok", "database", "ok");
  }
}
