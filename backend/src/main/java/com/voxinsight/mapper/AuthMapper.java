package com.voxinsight.mapper;

import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthMapper {

  Long findUserIdByLogin(@Param("loginName") String loginName);

  String findPasswordHash(@Param("loginName") String loginName);

  String findNickname(@Param("userId") long userId);

  String findRole(@Param("userId") long userId);

  int insertUser(
      @Param("loginName") String loginName,
      @Param("passwordHash") String passwordHash,
      @Param("nickname") String nickname);

  Long findNewUserId(@Param("loginName") String loginName);

  int insertGoal(@Param("userId") long userId);

  int insertNotification(@Param("userId") long userId);

  int insertPrivacy(@Param("userId") long userId);

  int insertToken(
      @Param("token") String token,
      @Param("userId") long userId,
      @Param("expiresAt") LocalDateTime expiresAt);

  Long findUserIdByToken(@Param("token") String token, @Param("now") LocalDateTime now);

  int deleteToken(@Param("token") String token);
}
