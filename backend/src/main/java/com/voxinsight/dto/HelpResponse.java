package com.voxinsight.dto;

import java.util.List;

public record HelpResponse(List<Article> articles, About about) {

  public record Article(String title, String content) {}

  public record About(String zh, String en, String slogan, String about, String version) {}
}
