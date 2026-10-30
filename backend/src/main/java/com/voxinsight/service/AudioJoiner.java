package com.voxinsight.service;

import com.voxinsight.web.BadRequestException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class AudioJoiner {

  public byte[] join(byte[] first, String firstMime, byte[] second, String secondMime) {
    Path left = null;
    Path right = null;
    Path output = null;
    try {
      left = temp(first, firstMime);
      right = temp(second, secondMime);
      output = Files.createTempFile("voxinsight-join-", ".webm");
      Process process =
          new ProcessBuilder(
                  "ffmpeg",
                  "-hide_banner",
                  "-loglevel",
                  "error",
                  "-y",
                  "-i",
                  left.toString(),
                  "-i",
                  right.toString(),
                  "-filter_complex",
                  "[0:a][1:a]concat=n=2:v=0:a=1",
                  "-t",
                  "1800",
                  "-c:a",
                  "libopus",
                  "-b:a",
                  "24k",
                  "-f",
                  "webm",
                  output.toString())
              .redirectError(ProcessBuilder.Redirect.DISCARD)
              .start();
      if (!process.waitFor(45, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        throw new BadRequestException("录音拼接超时。");
      }
      if (process.exitValue() != 0) {
        throw new BadRequestException("无法把两段录音接在一起。");
      }
      byte[] joined = Files.readAllBytes(output);
      if (joined.length == 0 || joined.length > AudioUpload.MAX_BYTES) {
        throw new BadRequestException("接好的录音过大，请控制在 8MB 以内。");
      }
      return joined;
    } catch (IOException exception) {
      throw new BadRequestException("无法把两段录音接在一起。");
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new BadRequestException("录音拼接被中断。");
    } finally {
      delete(left);
      delete(right);
      delete(output);
    }
  }

  private static Path temp(byte[] bytes, String mimeType) throws IOException {
    Path path = Files.createTempFile("voxinsight-", extension(mimeType));
    Files.write(path, bytes);
    return path;
  }

  private static void delete(Path path) {
    if (path == null) {
      return;
    }
    try {
      Files.deleteIfExists(path);
    } catch (IOException ignored) {
      // 临时文件留在系统临时目录，不影响已保存的录音。
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
