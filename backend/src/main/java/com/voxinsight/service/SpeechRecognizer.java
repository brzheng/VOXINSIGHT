package com.voxinsight.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voxinsight.config.AppProperties;
import com.voxinsight.web.BadRequestException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.springframework.stereotype.Component;
import org.vosk.Model;
import org.vosk.Recognizer;

@Component
public class SpeechRecognizer {

  private final ObjectMapper json = new ObjectMapper();
  private final Path modelPath;
  private Model model;

  public SpeechRecognizer(AppProperties properties) {
    Path path = Path.of(properties.getVoskModel());
    this.modelPath = path.isAbsolute() ? path : Path.of("").toAbsolutePath().resolve(path);
  }

  public String transcribe(byte[] pcm) {
    try (Recognizer recognizer = new Recognizer(model(), PauseDetector.SAMPLE_RATE)) {
      recognizer.setWords(true);
      int offset = 0;
      while (offset + 1 < pcm.length) {
        int length = Math.min(8000, pcm.length - offset);
        if ((length & 1) == 1) {
          length--;
        }
        byte[] chunk = Arrays.copyOfRange(pcm, offset, offset + length);
        recognizer.acceptWaveForm(chunk, chunk.length);
        offset += length;
      }
      JsonNode node = json.readTree(recognizer.getFinalResult());
      return normalize(node.path("text").asText(""));
    } catch (IOException exception) {
      throw new BadRequestException("语音识别失败，请再录一次。");
    }
  }

  private synchronized Model model() {
    if (model != null) {
      return model;
    }
    if (!Files.isDirectory(modelPath)) {
      throw new BadRequestException("语音识别模型还没有安装。");
    }
    try {
      model = new Model(modelPath.toString());
    } catch (IOException exception) {
      throw new BadRequestException("语音识别模型无法加载。");
    }
    return model;
  }

  static String normalize(String text) {
    return text.replaceAll("(?<=\\p{IsHan})\\s+(?=\\p{IsHan})", "").trim();
  }
}
