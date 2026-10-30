package com.voxinsight.service;

public final class PauseDetector {

  public static final int SAMPLE_RATE = 16_000;
  private static final double PAUSE_SECONDS = 0.4;

  private PauseDetector() {}

  public static double durationSeconds(byte[] pcm) {
    if (pcm == null || pcm.length < 2) {
      return 0;
    }
    return (pcm.length / 2) / (double) SAMPLE_RATE;
  }

  public static int countPauses(byte[] pcm) {
    int frameSamples = SAMPLE_RATE / 50;
    int sampleCount = pcm == null ? 0 : pcm.length / 2;
    int frames = sampleCount / frameSamples;
    if (frames < 10) {
      return 0;
    }
    double[] loudness = new double[frames];
    for (int frame = 0; frame < frames; frame++) {
      long sum = 0;
      int base = frame * frameSamples;
      for (int i = 0; i < frameSamples; i++) {
        int index = (base + i) * 2;
        int sample = (short) ((pcm[index + 1] << 8) | (pcm[index] & 0xff));
        sum += (long) sample * sample;
      }
      loudness[frame] = Math.sqrt(sum / (double) frameSamples);
    }
    double[] sorted = loudness.clone();
    java.util.Arrays.sort(sorted);
    double noise = sorted[frames / 5];
    double threshold = Math.max(350, noise * 3.5);
    boolean[] speech = new boolean[frames];
    for (int frame = 0; frame < frames; frame++) {
      speech[frame] = loudness[frame] >= threshold;
    }

    int pauseFrames = (int) Math.ceil(PAUSE_SECONDS * 50);
    int pauses = 0;
    int cursor = 0;
    boolean heardSpeech = false;
    while (cursor < frames) {
      if (speech[cursor]) {
        heardSpeech = true;
        while (cursor < frames && speech[cursor]) {
          cursor++;
        }
        continue;
      }
      int start = cursor;
      while (cursor < frames && !speech[cursor]) {
        cursor++;
      }
      if (heardSpeech && cursor < frames && speech[cursor] && cursor - start >= pauseFrames) {
        pauses++;
      }
    }
    return pauses;
  }
}
