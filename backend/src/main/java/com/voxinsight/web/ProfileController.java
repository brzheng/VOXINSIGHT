package com.voxinsight.web;

import com.voxinsight.dto.AnonymousSummary;
import com.voxinsight.dto.FeedbackItem;
import com.voxinsight.dto.FeedbackRequest;
import com.voxinsight.dto.GoalRequest;
import com.voxinsight.dto.GoalResponse;
import com.voxinsight.dto.ProfileResponse;
import com.voxinsight.dto.ProfileUpdateRequest;
import com.voxinsight.dto.SettingsResponse;
import com.voxinsight.model.Rows.AvatarRow;
import com.voxinsight.service.ProfileService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ProfileController {

  private final ProfileService profileService;

  public ProfileController(ProfileService profileService) {
    this.profileService = profileService;
  }

  @GetMapping("/api/profile")
  public ProfileResponse profile() {
    return profileService.load();
  }

  @PutMapping("/api/profile")
  public ProfileResponse update(@Valid @RequestBody ProfileUpdateRequest request) {
    return profileService.update(request);
  }

  @PostMapping(value = "/api/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public void uploadAvatar(@RequestPart("avatar") MultipartFile avatar) throws IOException {
    profileService.updateAvatar(avatar.getBytes(), avatar.getContentType());
  }

  @GetMapping("/api/profile/avatar")
  public ResponseEntity<byte[]> avatar() {
    AvatarRow row = profileService.avatar();
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .contentType(MediaType.parseMediaType(row.mimeType))
        .body(row.data);
  }

  @PutMapping("/api/profile/goal")
  public GoalResponse goal(@Valid @RequestBody GoalRequest request) {
    return profileService.updateGoal(request.targetCount());
  }

  @GetMapping("/api/profile/settings")
  public SettingsResponse settings() {
    return profileService.settings();
  }

  @PutMapping("/api/profile/settings")
  public SettingsResponse saveSettings(@RequestBody SettingsResponse request) {
    return profileService.updateSettings(request);
  }

  @GetMapping("/api/profile/summary")
  public AnonymousSummary summary() {
    return profileService.anonymousSummary();
  }

  @GetMapping("/api/feedback")
  public List<FeedbackItem> feedbacks() {
    return profileService.feedbacks();
  }

  @PostMapping("/api/feedback")
  public void feedback(@Valid @RequestBody FeedbackRequest request) {
    profileService.submitFeedback(request.category(), request.content());
  }
}
