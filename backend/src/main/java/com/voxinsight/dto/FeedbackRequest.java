package com.voxinsight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FeedbackRequest(
    @NotBlank @Size(max = 16) String category, @NotBlank @Size(max = 500) String content) {}
