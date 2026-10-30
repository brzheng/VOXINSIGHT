package com.voxinsight.service;

import com.voxinsight.web.AuthContext;
import com.voxinsight.web.UnauthorizedException;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

  public long requireId() {
    Long id = AuthContext.id();
    if (id == null) {
      throw new UnauthorizedException("请先登录。");
    }
    return id;
  }
}
