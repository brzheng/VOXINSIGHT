package com.voxinsight.dto;

public record SettingsResponse(
    boolean reminderOn,
    boolean reportNoticeOn,
    boolean goalNoticeOn,
    String reminderTime,
    boolean keepHistory,
    boolean anonymousStats) {}
