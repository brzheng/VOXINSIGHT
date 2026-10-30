package com.voxinsight.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.voxinsight.service.AudioUpload.Accepted;
import com.voxinsight.web.BadRequestException;
import org.junit.jupiter.api.Test;

class AudioUploadTest {

  @Test
  void keepsBrowserCodecSuffixOutOfTheStoredMimeType() {
    byte[] bytes = {1, 2, 3, 4};
    Accepted accepted = AudioUpload.accept(bytes, "audio/webm;codecs=opus");

    assertEquals("audio/webm", accepted.mimeType());
    assertArrayEquals(bytes, accepted.bytes());
  }

  @Test
  void rejectsEmptyUnsupportedAndOversizedAudio() {
    assertThrows(BadRequestException.class, () -> AudioUpload.accept(new byte[0], "audio/webm"));
    assertThrows(BadRequestException.class, () -> AudioUpload.accept(new byte[] {1}, "image/png"));
    assertThrows(BadRequestException.class, () -> AudioUpload.accept(new byte[] {1}, null));
    assertThrows(
        BadRequestException.class, () -> AudioUpload.accept(new byte[AudioUpload.MAX_BYTES + 1], "audio/wav"));
  }
}
