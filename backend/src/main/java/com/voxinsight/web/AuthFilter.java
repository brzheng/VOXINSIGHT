package com.voxinsight.web;

import com.voxinsight.mapper.AuthMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthFilter extends OncePerRequestFilter {

  private final AuthMapper authMapper;

  public AuthFilter(AuthMapper authMapper) {
    this.authMapper = authMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (!request.getRequestURI().startsWith("/api/") || isPublic(request)) {
      chain.doFilter(request, response);
      return;
    }
    String header = request.getHeader("Authorization");
    String token = header != null && header.startsWith("Bearer ") ? header.substring(7).trim() : "";
    Long userId = token.isEmpty() ? null : authMapper.findUserIdByToken(token, LocalDateTime.now());
    if (userId == null) {
      response.setStatus(401);
      response.setContentType("application/json;charset=UTF-8");
      response.getWriter().write("{\"message\":\"请先登录。\"}");
      return;
    }
    if (request.getRequestURI().startsWith("/api/admin/")
        && !"admin".equals(authMapper.findRole(userId))) {
      response.setStatus(403);
      response.setContentType("application/json;charset=UTF-8");
      response.getWriter().write("{\"message\":\"没有管理权限。\"}");
      return;
    }
    AuthContext.set(userId);
    try {
      chain.doFilter(request, response);
    } finally {
      AuthContext.clear();
    }
  }

  private static boolean isPublic(HttpServletRequest request) {
    String path = request.getRequestURI();
    String method = request.getMethod();
    if ("OPTIONS".equals(method)) {
      return true;
    }
    return ("GET".equals(method) && "/api/health".equals(path))
        || ("POST".equals(method)
            && ("/api/auth/login".equals(path) || "/api/auth/register".equals(path)));
  }
}
