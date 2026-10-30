package com.voxinsight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(@NotBlank @Size(max = 32) String loginName, @NotBlank @Size(max = 32) String password) {}
