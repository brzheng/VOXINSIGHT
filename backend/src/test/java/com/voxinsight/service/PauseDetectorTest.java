package com.voxinsight.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PauseDetectorTest {

  @Test
  void countsASilenceBetweenTwoSpokenParts() {
    byte[] pcm = new byte[0];
    pcm = append(pcm, silence(0.5));
    pcm = append(pcm, tone(0.3));
    pcm = append(pcm, silence(0.5));
    pcm = append(pcm, tone(0.3));
    pcm = append(pcm, silence(0.2));

    assertEquals(1, PauseDetector.countPauses(pcm));
  }

  @Test
  void ignoresSilenceWhenNobodySpoke() {
    assertEquals(0, PauseDetector.countPauses(silence(2)));
  }

  private static byte[] silence(double seconds) {
    return new byte[samples(seconds) * 2];
  }

  private static byte[] tone(double seconds) {
    int samples = samples(seconds);
    byte[] pcm = new byte[samples * 2];
    for (int i = 0; i < samples; i++) {
      short sample = (short) (8000 * Math.sin(2 * Math.PI * 220 * i / PauseDetector.SAMPLE_RATE));
      pcm[i * 2] = (byte) (sample & 0xff);
      pcm[i * 2 + 1] = (byte) ((sample >> 8) & 0xff);
    }
    return pcm;
  }

  private static int samples(double seconds) {
    return (int) Math.round(seconds * PauseDetector.SAMPLE_RATE);
  }

  private static byte[] append(byte[] left, byte[] right) {
    byte[] joined = new byte[left.length + right.length];
    System.arraycopy(left, 0, joined, 0, left.length);
    System.arraycopy(right, 0, joined, left.length, right.length);
    return joined;
  }
}
