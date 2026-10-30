-- 已有 voxinsight 库升级到管理后台。全新导入 voxinsight.sql 时不必执行。
-- 每条语句只需成功一次。若提示列已存在，说明这一步已经做过，跳过该句后继续即可。

USE voxinsight;

ALTER TABLE users
  ADD COLUMN role ENUM('user','admin') NOT NULL DEFAULT 'user' COMMENT 'user 学员 / admin 管理员' AFTER login_name;

ALTER TABLE user_feedbacks
  ADD COLUMN reply_content VARCHAR(500) NULL COMMENT '管理员回复' AFTER status,
  ADD COLUMN replied_at DATETIME NULL COMMENT '回复时间' AFTER reply_content;

INSERT INTO users (login_name, role, password_hash, nickname, avatar_url, bio)
SELECT 'admin', 'admin', 'obLD1OX2BxgpOktcbX6PkA==:KrBcaQjQfH3fBwnuWTokuOFkwhs3JDMbI2Gz6Yh243A=', '管理员', '', '声析管理账号，也可以登录学员端。'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE login_name = 'admin');

INSERT INTO training_goals (user_id, period_type, target_count)
SELECT u.user_id, 'weekly', 3
FROM users u
WHERE u.login_name = 'admin'
  AND NOT EXISTS (SELECT 1 FROM training_goals g WHERE g.user_id = u.user_id);

INSERT INTO notification_settings (
  user_id, practice_reminder_enabled, reminder_time, report_notice_enabled, goal_notice_enabled
)
SELECT u.user_id, 1, '20:00:00', 1, 1
FROM users u
WHERE u.login_name = 'admin'
  AND NOT EXISTS (SELECT 1 FROM notification_settings n WHERE n.user_id = u.user_id);

INSERT INTO privacy_settings (user_id, keep_training_history, allow_anonymous_stats)
SELECT u.user_id, 1, 0
FROM users u
WHERE u.login_name = 'admin'
  AND NOT EXISTS (SELECT 1 FROM privacy_settings p WHERE p.user_id = u.user_id);
