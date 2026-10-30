package com.voxinsight.dto;

import jakarta.validation.constraints.Min;

public record StartSessionRequest(@Min(1) long topicId) {}
