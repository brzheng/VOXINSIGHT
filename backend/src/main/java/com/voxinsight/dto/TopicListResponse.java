package com.voxinsight.dto;

import java.util.List;

public record TopicListResponse(List<String> categories, List<TopicCard> topics) {}
