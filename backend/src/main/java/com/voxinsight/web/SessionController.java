package com.voxinsight.web;

import com.voxinsight.dto.CreatedSession;
import com.voxinsight.dto.DurationRequest;
import com.voxinsight.dto.SessionState;
import com.voxinsight.dto.StartSessionRequest;
import com.voxinsight.model.Rows.AudioRow;
import com.voxinsight.service.SessionService;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class SessionController {

  private final SessionService sessionService;

  public SessionController(SessionService sessionService) {
    this.sessionService = sessionService;
  }

  @PostMapping("/api/sessions/start")
  public SessionState start(@Valid @RequestBody StartSessionRequest request) {
    return sessionService.start(request.topicId());
  }

  @GetMapping("/api/sessions/active")
  public ResponseEntity<SessionState> active(@RequestParam("topicId") long topicId) {
    SessionState state = sessionService.findActive(topicId);
    if (state == null) {
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.ok(state);
  }

  @PostMapping("/api/sessions/{id}/pause")
  public SessionState pause(@PathVariable("id") long id, @Valid @RequestBody DurationRequest request) {
    return sessionService.pause(id, request.durationSeconds());
  }

  @PostMapping("/api/sessions/{id}/resume")
  public SessionState resume(@PathVariable("id") long id) {
    return sessionService.resume(id);
  }

  @PostMapping(value = "/api/sessions/{id}/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public void audio(
      @PathVariable("id") long id,
      @RequestParam("durationSeconds") int durationSeconds,
      @RequestPart(value = "prefix", required = false) MultipartFile prefix,
      @RequestPart("audio") MultipartFile audio)
      throws IOException {
    sessionService.saveDraft(
        id,
        durationSeconds,
        audio.getBytes(),
        audio.getContentType(),
        prefix == null || prefix.isEmpty() ? null : prefix.getBytes(),
        prefix == null ? null : prefix.getContentType());
  }

  @GetMapping("/api/sessions/{id}/audio")
  public ResponseEntity<byte[]> readAudio(@PathVariable("id") long id) {
    AudioRow audio = sessionService.audio(id);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .contentType(MediaType.parseMediaType(audio.mimeType))
        .body(audio.audioData);
  }

  @PostMapping(value = "/api/sessions/{id}/finish", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public CreatedSession finish(
      @PathVariable("id") long id,
      @RequestParam(value = "durationSeconds", required = false) Integer durationSeconds,
      @RequestPart(value = "prefix", required = false) MultipartFile prefix,
      @RequestPart(value = "audio", required = false) MultipartFile audio)
      throws IOException {
    byte[] bytes = audio == null || audio.isEmpty() ? null : audio.getBytes();
    String mime = audio == null ? null : audio.getContentType();
    byte[] prefixBytes = prefix == null || prefix.isEmpty() ? null : prefix.getBytes();
    String prefixMime = prefix == null ? null : prefix.getContentType();
    return sessionService.finish(id, durationSeconds, bytes, mime, prefixBytes, prefixMime);
  }

  @PostMapping("/api/sessions/{id}/retry")
  public CreatedSession retry(@PathVariable("id") long id) {
    return sessionService.finish(id, null, null, null);
  }

  @DeleteMapping("/api/sessions/{id}")
  public void discard(@PathVariable("id") long id) {
    sessionService.discard(id);
  }
}
