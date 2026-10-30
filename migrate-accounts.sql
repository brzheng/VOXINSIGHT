-- 给已有库补上账号、头像、站内通知，并把「今天」改成服务器当天。
-- 不会删除训练记录。重新导入 voxinsight.sql 会清空全库，不必再跑本文件。

USE voxinsight;

SET @has_login := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'voxinsight' AND TABLE_NAME = 'users' AND COLUMN_NAME = 'login_name'
);
SET @sql := IF(
  @has_login = 0,
  'ALTER TABLE users ADD COLUMN login_name VARCHAR(32) NULL COMMENT ''登录账号'' AFTER user_id',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_password := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'voxinsight' AND TABLE_NAME = 'users' AND COLUMN_NAME = 'password_hash'
);
SET @sql := IF(
  @has_password = 0,
  'ALTER TABLE users ADD COLUMN password_hash VARCHAR(255) NULL COMMENT ''密码散列'' AFTER login_name',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_mime := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'voxinsight' AND TABLE_NAME = 'users' AND COLUMN_NAME = 'avatar_mime'
);
SET @sql := IF(
  @has_mime = 0,
  'ALTER TABLE users ADD COLUMN avatar_mime VARCHAR(64) NULL COMMENT ''头像类型'' AFTER avatar_url',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_avatar := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'voxinsight' AND TABLE_NAME = 'users' AND COLUMN_NAME = 'avatar_data'
);
SET @sql := IF(
  @has_avatar = 0,
  'ALTER TABLE users ADD COLUMN avatar_data MEDIUMBLOB NULL COMMENT ''头像文件'' AFTER avatar_mime',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE users
SET login_name = 'linxiao',
    password_hash = 'obLD1OX2BxgpOktcbX6PkA==:KrBcaQjQfH3fBwnuWTokuOFkwhs3JDMbI2Gz6Yh243A='
WHERE user_id = 1 AND login_name IS NULL;

UPDATE users
SET login_name = 'zhouning',
    password_hash = 'obLD1OX2BxgpOktcbX6PkA==:KrBcaQjQfH3fBwnuWTokuOFkwhs3JDMbI2Gz6Yh243A='
WHERE user_id = 2 AND login_name IS NULL;

UPDATE users
SET password_hash = 'obLD1OX2BxgpOktcbX6PkA==:KrBcaQjQfH3fBwnuWTokuOFkwhs3JDMbI2Gz6Yh243A='
WHERE user_id IN (1, 2) AND (password_hash IS NULL OR password_hash = '');

SET @has_login_key := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = 'voxinsight' AND TABLE_NAME = 'users' AND INDEX_NAME = 'uk_login_name'
);
SET @sql := IF(
  @has_login_key = 0,
  'ALTER TABLE users ADD UNIQUE KEY uk_login_name (login_name)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS auth_tokens (
  token       CHAR(64)        NOT NULL COMMENT '登录令牌',
  user_id     BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
  expires_at  DATETIME        NOT NULL COMMENT '过期时间',
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '签发时间',
  PRIMARY KEY (token),
  KEY idx_token_user (user_id),
  CONSTRAINT fk_token_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录令牌';

CREATE TABLE IF NOT EXISTS user_notices (
  notice_id    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '通知 ID',
  user_id      BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
  notice_type  ENUM('practice','report','goal') NOT NULL COMMENT '通知类型',
  content      VARCHAR(200)    NOT NULL COMMENT '通知内容',
  created_at   DATETIME        NOT NULL COMMENT '产生时间',
  read_at      DATETIME        NULL COMMENT '已读时间',
  PRIMARY KEY (notice_id),
  KEY idx_notice_user (user_id, read_at, created_at),
  CONSTRAINT fk_user_notice_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内通知';

CREATE OR REPLACE VIEW v_user_dashboard AS
SELECT
  u.user_id,
  u.nickname,
  u.avatar_url,
  CASE WHEN u.avatar_data IS NULL THEN 0 ELSE 1 END AS has_avatar,
  m.brand_name_zh,
  m.brand_name_en,
  m.slogan,
  m.as_of_date,
  g.target_count AS weekly_target_count,
  (
    SELECT COUNT(*)
    FROM training_sessions s
    WHERE s.user_id = u.user_id
      AND s.status = 'completed'
      AND DATE(s.ended_at) = CURDATE()
      AND EXISTS (
        SELECT 1 FROM privacy_settings p
        WHERE p.user_id = u.user_id AND p.keep_training_history = 1
      )
  ) AS today_completed_count,
  (
    SELECT COUNT(*)
    FROM training_sessions s
    WHERE s.user_id = u.user_id
      AND s.status = 'completed'
      AND s.ended_at >= DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY)
      AND s.ended_at < DATE_ADD(CURDATE(), INTERVAL 1 DAY)
      AND EXISTS (
        SELECT 1 FROM privacy_settings p
        WHERE p.user_id = u.user_id AND p.keep_training_history = 1
      )
  ) AS weekly_completed_count,
  (
    SELECT r.total_score
    FROM training_sessions s
    INNER JOIN analysis_reports r ON r.session_id = s.session_id
    WHERE s.user_id = u.user_id
      AND s.status = 'completed'
      AND EXISTS (
        SELECT 1 FROM privacy_settings p
        WHERE p.user_id = u.user_id AND p.keep_training_history = 1
      )
    ORDER BY s.ended_at DESC, s.session_id DESC
    LIMIT 1
  ) AS latest_score,
  (
    SELECT s.session_id
    FROM training_sessions s
    WHERE s.user_id = u.user_id
      AND s.status = 'completed'
      AND EXISTS (
        SELECT 1 FROM privacy_settings p
        WHERE p.user_id = u.user_id AND p.keep_training_history = 1
      )
    ORDER BY s.ended_at DESC, s.session_id DESC
    LIMIT 1
  ) AS latest_session_id,
  (
    SELECT r.report_id
    FROM training_sessions s
    INNER JOIN analysis_reports r ON r.session_id = s.session_id
    WHERE s.user_id = u.user_id
      AND s.status = 'completed'
      AND EXISTS (
        SELECT 1 FROM privacy_settings p
        WHERE p.user_id = u.user_id AND p.keep_training_history = 1
      )
    ORDER BY s.ended_at DESC, s.session_id DESC
    LIMIT 1
  ) AS latest_report_id
FROM users u
CROSS JOIN app_meta m
LEFT JOIN training_goals g ON g.user_id = u.user_id;

CREATE OR REPLACE VIEW v_user_training_stats AS
SELECT
  u.user_id,
  COUNT(s.session_id) AS total_session_count,
  COALESCE(SUM(s.duration_seconds), 0) AS total_duration_seconds,
  ROUND(AVG(r.total_score), 1) AS average_score
FROM users u
LEFT JOIN training_sessions s
  ON s.user_id = u.user_id
 AND s.status = 'completed'
 AND EXISTS (
   SELECT 1 FROM privacy_settings p
   WHERE p.user_id = u.user_id AND p.keep_training_history = 1
 )
LEFT JOIN analysis_reports r ON r.session_id = s.session_id
GROUP BY u.user_id;

CREATE OR REPLACE VIEW v_training_history AS
SELECT
  s.user_id,
  s.session_id,
  r.report_id,
  t.topic_id,
  t.title AS topic_title,
  c.category_name,
  t.difficulty,
  s.ended_at AS trained_at,
  s.duration_seconds,
  r.total_score,
  r.speech_rate,
  r.pause_count,
  r.fluency_score,
  r.result_label
FROM training_sessions s
INNER JOIN topics t ON t.topic_id = s.topic_id
INNER JOIN categories c ON c.category_id = t.category_id
INNER JOIN analysis_reports r ON r.session_id = s.session_id
WHERE s.status = 'completed'
  AND EXISTS (
    SELECT 1 FROM privacy_settings p
    WHERE p.user_id = s.user_id AND p.keep_training_history = 1
  );

ALTER TABLE users
  MODIFY login_name VARCHAR(32) NOT NULL COMMENT '登录账号',
  MODIFY password_hash VARCHAR(255) NOT NULL COMMENT '密码散列';
