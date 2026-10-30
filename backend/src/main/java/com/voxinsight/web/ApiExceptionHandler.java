package com.voxinsight.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ApiError> notFound(NotFoundException exception) {
    return ResponseEntity.status(404).body(new ApiError(exception.getMessage()));
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ApiError> badRequest(BadRequestException exception) {
    return ResponseEntity.badRequest().body(new ApiError(exception.getMessage()));
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ApiError> unauthorized(UnauthorizedException exception) {
    return ResponseEntity.status(401).body(new ApiError(exception.getMessage()));
  }

  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<ApiError> forbidden(ForbiddenException exception) {
    return ResponseEntity.status(403).body(new ApiError(exception.getMessage()));
  }

  @ExceptionHandler({
    MissingServletRequestParameterException.class,
    MissingServletRequestPartException.class
  })
  public ResponseEntity<ApiError> missingPart(Exception exception) {
    return ResponseEntity.badRequest().body(new ApiError("没有收到录音文件。"));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiError> tooLarge(MaxUploadSizeExceededException exception) {
    return ResponseEntity.badRequest().body(new ApiError("录音文件过大，请控制在 8MB 以内。"));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> invalid(MethodArgumentNotValidException exception) {
    return ResponseEntity.badRequest().body(new ApiError("请求参数不正确。"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> unavailable(Exception exception) {
    log.error("request failed", exception);
    return ResponseEntity.internalServerError().body(new ApiError("服务暂时不可用。"));
  }
}
