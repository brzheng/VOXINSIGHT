package com.voxinsight.service;

import java.time.LocalDateTime;

public final class DisplayTime {

  private DisplayTime() {}

  public static String dateLabel(LocalDateTime time) {
    return time.getMonthValue() + "月" + time.getDayOfMonth() + "日";
  }

  public static String shortDate(LocalDateTime time) {
    return time.getMonthValue() + "/" + time.getDayOfMonth();
  }

  public static String durationLabel(int seconds) {
    int minutes = seconds / 60;
    int remain = seconds % 60;
    if (minutes == 0) {
      return remain + "秒";
    }
    return minutes + "分" + String.format("%02d", remain) + "秒";
  }
}
