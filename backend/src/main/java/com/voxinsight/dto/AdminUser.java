package com.voxinsight.dto;

public record AdminUser(
    long id,
    String loginName,
    String nickname,
    String school,
    String major,
    String bio,
    String role,
    String createdAt,
    int sessionCount) {}
