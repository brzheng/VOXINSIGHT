package com.voxinsight.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

class AudioJoinerTest {

  @Test
  void appendsTheSecondClipAfterTheFirst() {
    byte[] joined = new AudioJoiner().join(tone(1.0, 440), "audio/wav", tone(1.0, 880), "audio/wav");
    byte[] pcm = new AudioDecoder().toPcm(joined, "audio/webm");

    assertTrue(joined.length > 0);
    assertEquals(2.0, PauseDetector.durationSeconds(pcm), 0.15);
  }

  private static byte[] tone(double seconds, double frequency) {
    int samples = (int) Math.round(seconds * PauseDetector.SAMPLE_RATE);
    byte[] pcm = new byte[samples * 2];
    ByteBuffer buffer = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN);
    for (int index = 0; index < samples; index++) {
      short sample =
          (short) (Math.sin(2 * Math.PI * frequency * index / PauseDetector.SAMPLE_RATE) * 8000);
      buffer.putShort(sample);
    }
    ByteBuffer wav = ByteBuffer.allocate(44 + pcm.length).order(ByteOrder.LITTLE_ENDIAN);
    wav.put("RIFF".getBytes());
    wav.putInt(36 + pcm.length);
    wav.put("WAVE".getBytes());
    wav.put("fmt ".getBytes());
    wav.putInt(16);
    wav.putShort((short) 1);
    wav.putShort((short) 1);
    wav.putInt(PauseDetector.SAMPLE_RATE);
    wav.putInt(PauseDetector.SAMPLE_RATE * 2);
    wav.putShort((short) 2);
    wav.putShort((short) 16);
    wav.put("data".getBytes());
    wav.putInt(pcm.length);
    wav.put(pcm);
    return wav.array();
  }
}
