package com.voxinsight.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record DurationRequest(@Min(0) @Max(1800) int durationSeconds) {}
