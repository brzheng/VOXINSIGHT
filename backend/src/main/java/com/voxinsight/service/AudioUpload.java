package com.voxinsight.service;

import com.voxinsight.web.BadRequestException;
import java.util.Set;

public final class AudioUpload {

  public static final int MAX_BYTES = 8 * 1024 * 1024;

  private static final Set<String> TYPES =
      Set.of("audio/webm", "audio/ogg", "audio/mp4", "audio/mpeg", "audio/wav", "audio/x-wav", "audio/wave");

  private AudioUpload() {}

  public record Accepted(byte[] bytes, String mimeType) {}

  public static Accepted accept(byte[] bytes, String contentType) {
    if (bytes == null || bytes.length == 0) {
      throw new BadRequestException("没有收到录音文件。");
    }
    if (bytes.length > MAX_BYTES) {
      throw new BadRequestException("录音文件过大，请控制在 8MB 以内。");
    }
    String mimeType = normalize(contentType);
    if (!TYPES.contains(mimeType)) {
      throw new BadRequestException("录音格式不受支持。");
    }
    return new Accepted(bytes, mimeType);
  }

  static String normalize(String contentType) {
    if (contentType == null) {
      return "";
    }
    int separator = contentType.indexOf(';');
    String mimeType = separator < 0 ? contentType : contentType.substring(0, separator);
    return mimeType.trim().toLowerCase();
  }
}
