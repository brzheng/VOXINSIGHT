package com.voxinsight.dto;

import java.util.List;

public record ProfileResponse(
    ProfileUser user, TrainingStats stats, int weeklyDone, int weeklyTarget) {

  public record ProfileUser(
      String nickname, String loginName, String initial, String school, String major, String bio, boolean hasAvatar) {}
}
