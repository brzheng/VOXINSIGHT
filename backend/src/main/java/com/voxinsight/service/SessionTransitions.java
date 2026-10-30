package com.voxinsight.service;

import com.voxinsight.web.BadRequestException;

public final class SessionTransitions {

  private SessionTransitions() {}

  public static void pause(String status) {
    require("recording".equals(status), "只有录制中的训练可以暂停。");
  }

  public static void resume(String status) {
    require("paused".equals(status), "只有已暂停的训练可以继续。");
  }

  public static void analyze(String status) {
    require(
        "recording".equals(status)
            || "paused".equals(status)
            || "failed".equals(status)
            || "analyzing".equals(status),
        "这次训练不能再生成报告。");
  }

  public static void discard(String status) {
    require(status != null && !"completed".equals(status), "已完成的训练不能放弃。");
  }

  private static void require(boolean allowed, String message) {
    if (!allowed) {
      throw new BadRequestException(message);
    }
  }
}
