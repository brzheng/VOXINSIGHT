package com.voxinsight.service;

import com.voxinsight.web.BadRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class AudioDecoder {

  public byte[] toPcm(byte[] audio, String mimeType) {
    Path input = null;
    try {
      input = Files.createTempFile("voxinsight-", extension(mimeType));
      Files.write(input, audio);
      Process process =
          new ProcessBuilder(
                  "ffmpeg",
                  "-hide_banner",
                  "-loglevel",
                  "error",
                  "-i",
                  input.toString(),
                  "-t",
                  "1800",
                  "-ac",
                  "1",
                  "-ar",
                  Integer.toString(PauseDetector.SAMPLE_RATE),
                  "-f",
                  "s16le",
                  "-acodec",
                  "pcm_s16le",
                  "pipe:1")
              .redirectError(ProcessBuilder.Redirect.DISCARD)
              .start();
      byte[] pcm;
      try (InputStream stream = process.getInputStream()) {
        pcm = stream.readAllBytes();
      }
      if (!process.waitFor(45, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        throw new BadRequestException("录音解码超时。");
      }
      if (process.exitValue() != 0 || pcm.length < PauseDetector.SAMPLE_RATE) {
        throw new BadRequestException("无法解码这段录音。");
      }
      return pcm;
    } catch (IOException exception) {
      throw new BadRequestException("无法解码这段录音。");
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new BadRequestException("录音解码被中断。");
    } finally {
      if (input != null) {
        try {
          Files.deleteIfExists(input);
        } catch (IOException ignored) {
          // 临时文件留在系统临时目录，不影响评分结果。
        }
      }
    }
  }

  private static String extension(String mimeType) {
    if (mimeType == null) {
      return ".audio";
    }
    if (mimeType.contains("wav")) {
      return ".wav";
    }
    if (mimeType.contains("ogg")) {
      return ".ogg";
    }
    if (mimeType.contains("mp4")) {
      return ".m4a";
    }
    if (mimeType.contains("mpeg")) {
      return ".mp3";
    }
    return ".webm";
  }
}
