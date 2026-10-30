package com.voxinsight.web;

import com.voxinsight.dto.AdminCategory;
import com.voxinsight.dto.AdminCategoryRequest;
import com.voxinsight.dto.AdminFeedback;
import com.voxinsight.dto.AdminFeedbackUpdate;
import com.voxinsight.dto.AdminOverview;
import com.voxinsight.dto.AdminSession;
import com.voxinsight.dto.AdminSessionDetail;
import com.voxinsight.dto.AdminTopic;
import com.voxinsight.dto.AdminTopicDetail;
import com.voxinsight.dto.AdminTopicRequest;
import com.voxinsight.dto.AdminUser;
import com.voxinsight.dto.AdminUserUpdate;
import com.voxinsight.dto.AudioFile;
import com.voxinsight.service.AdminService;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {

  private final AdminService adminService;

  public AdminController(AdminService adminService) {
    this.adminService = adminService;
  }

  @GetMapping("/api/admin/overview")
  public AdminOverview overview() {
    return adminService.overview();
  }

  @GetMapping("/api/admin/users")
  public List<AdminUser> users(@RequestParam(required = false) String keyword) {
    return adminService.users(keyword);
  }

  @PutMapping("/api/admin/users/{id}")
  public AdminUser updateUser(@PathVariable("id") long id, @RequestBody AdminUserUpdate request) {
    return adminService.updateUser(id, request);
  }

  @DeleteMapping("/api/admin/users/{id}")
  public void deleteUser(@PathVariable("id") long id) {
    adminService.deleteUser(id);
  }

  @GetMapping("/api/admin/categories")
  public List<AdminCategory> categories() {
    return adminService.categories();
  }

  @PostMapping("/api/admin/categories")
  public AdminCategory createCategory(@RequestBody AdminCategoryRequest request) {
    return adminService.createCategory(request == null ? null : request.name());
  }

  @GetMapping("/api/admin/topics")
  public List<AdminTopic> topics() {
    return adminService.topics();
  }

  @GetMapping("/api/admin/topics/{id}")
  public AdminTopicDetail topic(@PathVariable("id") long id) {
    return adminService.topic(id);
  }

  @PostMapping("/api/admin/topics")
  public AdminTopicDetail createTopic(@RequestBody AdminTopicRequest request) {
    return adminService.createTopic(request);
  }

  @PutMapping("/api/admin/topics/{id}")
  public AdminTopicDetail updateTopic(@PathVariable("id") long id, @RequestBody AdminTopicRequest request) {
    return adminService.updateTopic(id, request);
  }

  @DeleteMapping("/api/admin/topics/{id}")
  public void deleteTopic(@PathVariable("id") long id) {
    adminService.deleteTopic(id);
  }

  @GetMapping("/api/admin/sessions")
  public List<AdminSession> sessions(@RequestParam(required = false) String status) {
    return adminService.sessions(status);
  }

  @GetMapping("/api/admin/sessions/{id}")
  public AdminSessionDetail session(@PathVariable("id") long id) {
    return adminService.session(id);
  }

  @GetMapping("/api/admin/sessions/{id}/audio")
  public ResponseEntity<byte[]> audio(@PathVariable("id") long id) {
    AudioFile file = adminService.audio(id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.mimeType()))
        .contentLength(file.bytes().length)
        .body(file.bytes());
  }

  @DeleteMapping("/api/admin/sessions/{id}")
  public void deleteSession(@PathVariable("id") long id) {
    adminService.deleteSession(id);
  }

  @GetMapping("/api/admin/feedbacks")
  public List<AdminFeedback> feedbacks(@RequestParam(required = false) String status) {
    return adminService.feedbacks(status);
  }

  @PutMapping("/api/admin/feedbacks/{id}")
  public AdminFeedback updateFeedback(
      @PathVariable("id") long id, @RequestBody AdminFeedbackUpdate request) {
    return adminService.updateFeedback(id, request);
  }
}
