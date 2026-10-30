package com.voxinsight.web;

import com.voxinsight.dto.AudioFile;
import com.voxinsight.dto.ReportResponse;
import com.voxinsight.service.ReportService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReportController {

  private final ReportService reportService;

  public ReportController(ReportService reportService) {
    this.reportService = reportService;
  }

  @GetMapping("/api/reports/{id}")
  public ReportResponse report(@PathVariable("id") long id) {
    return reportService.load(id);
  }

  @GetMapping("/api/reports/{id}/audio")
  public ResponseEntity<byte[]> audio(@PathVariable("id") long id) {
    AudioFile file = reportService.loadAudio(id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.mimeType()))
        .contentLength(file.bytes().length)
        .body(file.bytes());
  }
}
