package com.voxinsight.dto;

public record AuthResponse(String token, String loginName, String nickname, String role) {}
