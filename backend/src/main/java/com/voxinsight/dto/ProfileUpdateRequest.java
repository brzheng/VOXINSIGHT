package com.voxinsight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
    @NotBlank @Size(max = 32) String nickname,
    @Size(max = 64) String school,
    @Size(max = 64) String major,
    @Size(max = 120) String bio) {}
