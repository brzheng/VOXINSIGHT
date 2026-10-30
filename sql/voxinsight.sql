-- =============================================================================
-- 声析 VOXINSIGHT 数据库脚本
-- 课程：软件开发实践 · 数据库周
-- 适用：MySQL 8.0+（InnoDB，utf8mb4）
--
-- 结束训练时，浏览器麦克风录到的音频写入 session_audios。
-- 新产生的报告由语音识别和波形停顿计算。种子数据里的两份历史报告仍是模拟结果。
-- 库中不保存性格、心理状态或面试成败类字段。
--
-- 本文件位于 sql/。重新执行会删除并重建 voxinsight 库。
-- 同目录的 migrate-*.sql 只用于旧库原地升级；全新导入本文件后不必再执行。
-- 首页、历史和新训练都按服务器当天统计，周从周一开始。
-- app_meta.as_of_date 只保留原先的演示日期，不再决定进度或新记录的时间。
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP DATABASE IF EXISTS voxinsight;
CREATE DATABASE voxinsight
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE voxinsight;

-- -----------------------------------------------------------------------------
-- 页面与表的对应关系
-- M01 首页       users + training_goals + topics + v_user_dashboard
-- M02 题库       categories + topics + user_favorites
-- M03 录音训练   training_sessions（pending / recording / paused）
-- M04 分析报告   analysis_reports + report_feedbacks
-- M05 训练历史   v_training_history（只含已完成且已出报告的记录）
-- M06 个人中心   users + v_user_training_stats + 各设置表 + help_articles
-- -----------------------------------------------------------------------------

-- -----------------------------------------------------------------------------
-- 应用信息（关于声析，同时冻结原型演示日期）
-- -----------------------------------------------------------------------------
CREATE TABLE app_meta (
  meta_id       TINYINT UNSIGNED NOT NULL COMMENT '固定为 1',
  brand_name_zh VARCHAR(16)      NOT NULL COMMENT '中文品牌名',
  brand_name_en VARCHAR(32)      NOT NULL COMMENT '英文品牌名',
  slogan        VARCHAR(64)      NOT NULL COMMENT '产品 Slogan',
  as_of_date    DATE             NOT NULL COMMENT '保留的演示日期，不参与统计',
  version_name  VARCHAR(16)      NOT NULL COMMENT '原型版本',
  about_text    VARCHAR(500)     NOT NULL COMMENT '关于声析',
  PRIMARY KEY (meta_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用信息与演示日期';

-- -----------------------------------------------------------------------------
-- 用户与个人资料
-- -----------------------------------------------------------------------------
CREATE TABLE users (
  user_id       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
  login_name    VARCHAR(32)     NOT NULL COMMENT '登录账号',
  role          ENUM('user','admin') NOT NULL DEFAULT 'user' COMMENT 'user 学员 / admin 管理员',
  password_hash VARCHAR(255)    NOT NULL COMMENT '密码散列',
  nickname      VARCHAR(32)     NOT NULL COMMENT '昵称',
  avatar_url    VARCHAR(255)    NOT NULL COMMENT '头像路径',
  avatar_mime   VARCHAR(64)     NULL     COMMENT '头像类型',
  avatar_data   MEDIUMBLOB      NULL     COMMENT '头像文件',
  school        VARCHAR(64)     NULL     COMMENT '学校',
  major         VARCHAR(64)     NULL     COMMENT '专业',
  bio           VARCHAR(120)    NULL     COMMENT '个人简介',
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '资料更新时间',
  PRIMARY KEY (user_id),
  UNIQUE KEY uk_login_name (login_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户';

-- -----------------------------------------------------------------------------
-- 训练目标：个人中心「每周练习 3 次」，首页进度的分母
-- -----------------------------------------------------------------------------
CREATE TABLE training_goals (
  user_id      BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
  period_type  ENUM('weekly')  NOT NULL DEFAULT 'weekly' COMMENT '目标周期，原型仅为每周',
  target_count TINYINT UNSIGNED NOT NULL DEFAULT 3 COMMENT '目标次数',
  updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (user_id),
  CONSTRAINT fk_goal_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
  CONSTRAINT chk_goal_target CHECK (target_count BETWEEN 1 AND 14)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='训练目标';

-- -----------------------------------------------------------------------------
-- 通知设置
-- -----------------------------------------------------------------------------
CREATE TABLE notification_settings (
  user_id                    BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
  practice_reminder_enabled  TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '练习提醒',
  reminder_time              TIME            NOT NULL DEFAULT '20:00:00' COMMENT '提醒时间',
  report_notice_enabled      TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '报告生成通知',
  goal_notice_enabled        TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '周目标提醒',
  updated_at                 DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (user_id),
  CONSTRAINT fk_notice_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知设置';

-- -----------------------------------------------------------------------------
-- 隐私与数据管理
-- 只管理训练记录是否保留在本机演示库，不涉及性格或心理画像
-- -----------------------------------------------------------------------------
CREATE TABLE privacy_settings (
  user_id                  BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
  keep_training_history    TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '保留训练历史',
  allow_anonymous_stats    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '允许汇总匿名使用统计',
  updated_at               DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (user_id),
  CONSTRAINT fk_privacy_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='隐私与数据管理';

-- -----------------------------------------------------------------------------
-- 题目分类。原型中的「全部」是筛选条件，不入库
-- -----------------------------------------------------------------------------
CREATE TABLE categories (
  category_id   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分类 ID',
  category_name VARCHAR(32)     NOT NULL COMMENT '分类名称',
  sort_order    TINYINT UNSIGNED NOT NULL COMMENT '排序',
  PRIMARY KEY (category_id),
  UNIQUE KEY uk_category_name (category_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目分类';

-- -----------------------------------------------------------------------------
-- 训练题目
-- -----------------------------------------------------------------------------
CREATE TABLE topics (
  topic_id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '题目 ID',
  category_id       BIGINT UNSIGNED NOT NULL COMMENT '分类 ID',
  title             VARCHAR(120)    NOT NULL COMMENT '题目标题',
  difficulty        ENUM('入门','进阶','挑战') NOT NULL COMMENT '难度',
  duration_min      TINYINT UNSIGNED NOT NULL COMMENT '建议最短分钟数',
  duration_max      TINYINT UNSIGNED NOT NULL COMMENT '建议最长分钟数',
  guide_text        VARCHAR(120)    NOT NULL COMMENT '录音页提示',
  is_recommended    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否为首页推荐题',
  sort_order        TINYINT UNSIGNED NOT NULL COMMENT '列表排序',
  created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (topic_id),
  KEY idx_topic_category (category_id),
  KEY idx_topic_title (title),
  CONSTRAINT fk_topic_category FOREIGN KEY (category_id) REFERENCES categories (category_id),
  CONSTRAINT chk_topic_duration CHECK (duration_min >= 1 AND duration_max >= duration_min)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='训练题目';

CREATE TABLE topic_checkpoints (
  checkpoint_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '要点 ID',
  topic_id      BIGINT UNSIGNED NOT NULL COMMENT '题目 ID',
  label         VARCHAR(40)     NOT NULL COMMENT '要点名称',
  keywords      VARCHAR(200)    NOT NULL COMMENT '命中关键词，逗号分隔',
  sort_order    TINYINT UNSIGNED NOT NULL COMMENT '排序',
  PRIMARY KEY (checkpoint_id),
  UNIQUE KEY uk_topic_checkpoint (topic_id, label),
  CONSTRAINT fk_checkpoint_topic FOREIGN KEY (topic_id) REFERENCES topics (topic_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目内容要点';

-- -----------------------------------------------------------------------------
-- 收藏。联合主键保证同一用户对同一题目只收藏一次
-- -----------------------------------------------------------------------------
CREATE TABLE user_favorites (
  user_id    BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
  topic_id   BIGINT UNSIGNED NOT NULL COMMENT '题目 ID',
  created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (user_id, topic_id),
  KEY idx_favorite_topic (topic_id),
  CONSTRAINT fk_fav_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
  CONSTRAINT fk_fav_topic FOREIGN KEY (topic_id) REFERENCES topics (topic_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目收藏';

-- -----------------------------------------------------------------------------
-- 一次录音训练
-- status：pending 待开始 / recording 录制中 / paused 已暂停
--         analyzing 分析中 / completed 已完成 / failed 分析失败
-- 音频不放在本表，避免历史和首页查询带出二进制内容
-- -----------------------------------------------------------------------------
CREATE TABLE training_sessions (
  session_id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '训练记录 ID',
  user_id           BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
  topic_id          BIGINT UNSIGNED NOT NULL COMMENT '题目 ID',
  status            ENUM('pending','recording','paused','analyzing','completed','failed') NOT NULL COMMENT '训练状态',
  started_at        DATETIME        NULL COMMENT '开始录音时间',
  ended_at          DATETIME        NULL COMMENT '结束时间',
  duration_seconds  INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '有效录音秒数',
  fail_reason       VARCHAR(120)    NULL COMMENT '分析失败原因，仅 failed 时填写',
  created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (session_id),
  KEY idx_session_user_time (user_id, ended_at),
  KEY idx_session_topic (topic_id),
  CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
  CONSTRAINT fk_session_topic FOREIGN KEY (topic_id) REFERENCES topics (topic_id),
  CONSTRAINT chk_session_duration CHECK (duration_seconds <= 1800)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='训练记录';

-- -----------------------------------------------------------------------------
-- 一次已完成训练对应的麦克风录音。种子数据里的历史记录没有音频。
-- -----------------------------------------------------------------------------
CREATE TABLE session_audios (
  session_id  BIGINT UNSIGNED NOT NULL COMMENT '训练记录 ID',
  mime_type   VARCHAR(64)     NOT NULL COMMENT '音频 MIME，不含 codecs 参数',
  byte_size   INT UNSIGNED    NOT NULL COMMENT '音频字节数',
  audio_data  MEDIUMBLOB      NOT NULL COMMENT '音频内容',
  PRIMARY KEY (session_id),
  CONSTRAINT fk_audio_session FOREIGN KEY (session_id) REFERENCES training_sessions (session_id) ON DELETE CASCADE,
  CONSTRAINT chk_audio_size CHECK (byte_size >= 1 AND byte_size <= 8388608)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='训练录音';

-- -----------------------------------------------------------------------------
-- 分析报告。一次已完成训练对应一份报告；暂停和失败没有报告
-- -----------------------------------------------------------------------------
CREATE TABLE analysis_reports (
  report_id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '报告 ID',
  session_id     BIGINT UNSIGNED NOT NULL COMMENT '训练记录 ID',
  total_score    TINYINT UNSIGNED NOT NULL COMMENT '总分，0–100',
  speech_rate    SMALLINT UNSIGNED NOT NULL COMMENT '语速，字/分钟',
  pause_count    SMALLINT UNSIGNED NOT NULL COMMENT '停顿次数',
  fluency_score  TINYINT UNSIGNED NOT NULL COMMENT '表达流畅度，0–100',
  result_label    VARCHAR(32)     NOT NULL DEFAULT '语音识别结果' COMMENT '结果性质标注',
  transcript      TEXT            NULL COMMENT '语音识别文本，旧的模拟报告为空',
  content_score   TINYINT UNSIGNED NULL COMMENT '内容分数，0–100',
  content_summary VARCHAR(500)    NULL COMMENT '内容要点覆盖说明',
  analyzed_at     DATETIME        NOT NULL COMMENT '报告生成时间',
  PRIMARY KEY (report_id),
  UNIQUE KEY uk_report_session (session_id),
  CONSTRAINT fk_report_session FOREIGN KEY (session_id) REFERENCES training_sessions (session_id) ON DELETE CASCADE,
  CONSTRAINT chk_report_score CHECK (total_score <= 100 AND fluency_score <= 100),
  CONSTRAINT chk_report_rate CHECK (speech_rate <= 2000),
  CONSTRAINT chk_report_pause CHECK (pause_count <= 6000),
  CONSTRAINT chk_report_content CHECK (content_score IS NULL OR content_score <= 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='训练分析报告';

-- -----------------------------------------------------------------------------
-- 报告中的表现与建议。一条建议一行，避免把多条文本塞进同一字段
-- strength 表现较好 / improvement 可改进 / suggestion 改进建议
-- -----------------------------------------------------------------------------
CREATE TABLE report_feedbacks (
  feedback_id   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '反馈 ID',
  report_id     BIGINT UNSIGNED NOT NULL COMMENT '报告 ID',
  feedback_type ENUM('strength','improvement','suggestion') NOT NULL COMMENT '反馈类型',
  content       VARCHAR(255)    NOT NULL COMMENT '反馈内容',
  sort_order    TINYINT UNSIGNED NOT NULL COMMENT '同一报告内的排序',
  PRIMARY KEY (feedback_id),
  KEY idx_feedback_report (report_id, sort_order),
  CONSTRAINT fk_feedback_report FOREIGN KEY (report_id) REFERENCES analysis_reports (report_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报告表现与改进建议';

-- -----------------------------------------------------------------------------
-- 帮助与反馈
-- -----------------------------------------------------------------------------
CREATE TABLE help_articles (
  article_id  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '文章 ID',
  title       VARCHAR(80)     NOT NULL COMMENT '标题',
  content     VARCHAR(500)    NOT NULL COMMENT '正文',
  sort_order  TINYINT UNSIGNED NOT NULL COMMENT '排序',
  PRIMARY KEY (article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帮助文章';

CREATE TABLE user_feedbacks (
  feedback_id  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '反馈 ID',
  user_id      BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
  category     ENUM('功能建议','问题反馈','其他') NOT NULL COMMENT '反馈类别',
  content      VARCHAR(500)    NOT NULL COMMENT '反馈内容',
  status        ENUM('待处理','已回复') NOT NULL DEFAULT '待处理' COMMENT '处理状态',
  reply_content VARCHAR(500)    NULL COMMENT '管理员回复',
  replied_at    DATETIME        NULL COMMENT '回复时间',
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  PRIMARY KEY (feedback_id),
  KEY idx_user_feedback (user_id, created_at),
  CONSTRAINT fk_user_feedback_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户反馈';

CREATE TABLE auth_tokens (
  token       CHAR(64)        NOT NULL COMMENT '登录令牌',
  user_id     BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
  expires_at  DATETIME        NOT NULL COMMENT '过期时间',
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '签发时间',
  PRIMARY KEY (token),
  KEY idx_token_user (user_id),
  CONSTRAINT fk_token_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录令牌';

CREATE TABLE user_notices (
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

-- -----------------------------------------------------------------------------
-- 首页工作台按服务器当天统计。关闭「保留训练历史」时，次数和最近成绩显示为空。
-- -----------------------------------------------------------------------------
CREATE VIEW v_user_dashboard AS
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

-- -----------------------------------------------------------------------------
-- 个人中心累计统计。只统计已完成训练；关闭历史保留时这些数字为 0
-- -----------------------------------------------------------------------------
CREATE VIEW v_user_training_stats AS
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

-- -----------------------------------------------------------------------------
-- 训练历史与成绩趋势。按 trained_at 倒序即为历史列表，正序即为趋势
-- -----------------------------------------------------------------------------
CREATE VIEW v_training_history AS
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

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 演示数据
--
-- 主用户林晓的已完成记录：
--   2026-10-10  自我介绍  72 分  语速 142  停顿 8 次  流畅度 76
--   2026-10-12  自我介绍  81 分  语速 168  停顿 4 次  流畅度 85
-- 第二次停顿更少、语速更接近自然交谈、流畅度和总分更高，建议文案与指标一致。
-- 首页「最近一次成绩」= 81；72 分保留为上一次报告，可从历史进入。
-- 种子训练日期是 2026-10-10 和 2026-10-12。
-- 首页本周进度只统计当前自然周，导入日不在那一周时，进度显示为 0。
-- 另有一条已暂停、一条分析失败，不进入历史、平均分和进度。
-- 用户周宁没有任何训练，用于空历史状态。
-- 演示登录：linxiao / zhouning，密码都是 demo1234。
-- =============================================================================

INSERT INTO app_meta (
  meta_id, brand_name_zh, brand_name_en, slogan, as_of_date, version_name, about_text
) VALUES (
  1,
  '声析',
  'VOXINSIGHT',
  '让每一次表达，都有迹可循。',
  '2026-10-12',
  '0.1.0-demo',
  '声析 VOXINSIGHT 是面向大学生和求职者的表达训练原型。录音会保存在数据库里，并由语音识别计算语速、停顿和流畅度。分数用来观察自己的变化，不判断性格、心理状态或面试结果。'
);

INSERT INTO users (user_id, login_name, role, password_hash, nickname, avatar_url, school, major, bio, created_at) VALUES
  (1, 'linxiao', 'user', 'obLD1OX2BxgpOktcbX6PkA==:KrBcaQjQfH3fBwnuWTokuOFkwhs3JDMbI2Gz6Yh243A=', '林晓', 'assets/avatars/linxiao.png', '明德大学', '软件工程', '正在准备秋招面试，想把自我介绍说得更顺。', '2026-10-01 10:00:00'),
  (2, 'zhouning', 'user', 'obLD1OX2BxgpOktcbX6PkA==:KrBcaQjQfH3fBwnuWTokuOFkwhs3JDMbI2Gz6Yh243A=', '周宁', 'assets/avatars/zhouning.png', '明德大学', '汉语言文学', '刚注册，还没有训练记录。', '2026-10-12 09:00:00'),
  (3, 'admin', 'admin', 'obLD1OX2BxgpOktcbX6PkA==:KrBcaQjQfH3fBwnuWTokuOFkwhs3JDMbI2Gz6Yh243A=', '管理员', '', NULL, NULL, '声析管理账号，也可以登录学员端。', '2026-10-01 09:00:00');

INSERT INTO training_goals (user_id, period_type, target_count) VALUES
  (1, 'weekly', 3),
  (2, 'weekly', 3),
  (3, 'weekly', 3);

INSERT INTO notification_settings (user_id, practice_reminder_enabled, reminder_time, report_notice_enabled, goal_notice_enabled) VALUES
  (1, 1, '20:00:00', 1, 1),
  (2, 1, '20:00:00', 1, 1),
  (3, 1, '20:00:00', 1, 1);

INSERT INTO privacy_settings (user_id, keep_training_history, allow_anonymous_stats) VALUES
  (1, 1, 0),
  (2, 1, 0),
  (3, 1, 0);

INSERT INTO categories (category_id, category_name, sort_order) VALUES
  (1, '面试表达', 1),
  (2, '日常口语', 2),
  (3, '演讲训练', 3);

INSERT INTO topics (
  topic_id, category_id, title, difficulty, duration_min, duration_max, guide_text, is_recommended, sort_order
) VALUES
  (1, 1, '请用 1 分钟做一个自我介绍。', '入门', 1, 2, '尽量自然表达，注意语速与停顿。', 1, 1),
  (2, 1, '你最大的优势是什么？', '入门', 1, 2, '尽量自然表达，注意语速与停顿。', 0, 2),
  (3, 1, '请介绍一次你解决问题的经历。', '进阶', 2, 3, '尽量自然表达，注意语速与停顿。', 0, 3),
  (4, 1, '如果遇到团队意见不一致，你会怎么做？', '进阶', 2, 3, '尽量自然表达，注意语速与停顿。', 0, 4),
  (5, 3, '请围绕一个熟悉的话题进行 2 分钟表达。', '入门', 2, 2, '尽量自然表达，注意语速与停顿。', 0, 5),
  (6, 2, '请用日常对话的方式介绍你的一天。', '入门', 1, 2, '尽量自然表达，注意语速与停顿。', 0, 6);

INSERT INTO topic_checkpoints (topic_id, label, keywords, sort_order) VALUES
  (1, '身份', '我是,我叫,名叫', 1),
  (1, '学校或专业', '大学,专业,学院,学校', 2),
  (1, '经历', '项目,实习,负责,经历', 3),
  (1, '意向', '希望,应聘,求职,岗位', 4),
  (2, '优势', '优势,擅长,长处', 1),
  (2, '例子', '例如,比如,曾经,项目', 2),
  (2, '结果', '所以,因此,完成,学会', 3),
  (3, '背景', '问题,困难,当时,遇到', 1),
  (3, '行动', '解决,沟通,分析,尝试', 2),
  (3, '结果', '最后,结果,完成', 3),
  (4, '分歧', '意见,分歧,不一致,冲突', 1),
  (4, '处理', '沟通,倾听,讨论,协调', 2),
  (4, '结果', '达成,一致,方案', 3),
  (5, '主题', '关于,话题,介绍', 1),
  (5, '细节', '因为,比如,例如,首先', 2),
  (5, '收束', '所以,总之,总结', 3),
  (6, '时间', '早上,上午,中午,下午,晚上', 1),
  (6, '活动', '上课,吃饭,学习,工作,运动,睡觉', 2),
  (6, '感受', '觉得,感觉,开心,顺利', 3);

INSERT INTO user_favorites (user_id, topic_id, created_at) VALUES
  (1, 1, '2026-10-10 19:20:00'),
  (1, 3, '2026-10-11 21:05:00');

-- 已完成两次自我介绍；暂停和失败只用于补充状态，不生成报告
INSERT INTO training_sessions (
  session_id, user_id, topic_id, status, started_at, ended_at, duration_seconds, fail_reason, created_at
) VALUES
  (1, 1, 1, 'completed', '2026-10-10 19:30:00', '2026-10-10 19:31:08', 68, NULL, '2026-10-10 19:30:00'),
  (2, 1, 1, 'completed', '2026-10-12 08:40:00', '2026-10-12 08:41:15', 75, NULL, '2026-10-12 08:40:00'),
  (3, 1, 2, 'paused',    '2026-10-11 21:10:00', NULL,                   26, NULL, '2026-10-11 21:10:00'),
  (4, 1, 4, 'failed',    '2026-10-09 16:00:00', '2026-10-09 16:01:02', 62, '演示：分析未能生成，本次不计入成绩。', '2026-10-09 16:00:00');

INSERT INTO analysis_reports (
  report_id, session_id, total_score, speech_rate, pause_count, fluency_score, result_label, analyzed_at
) VALUES
  (1, 1, 72, 142, 8, 76, '模拟分析结果', '2026-10-10 19:31:12'),
  (2, 2, 81, 168, 4, 85, '模拟分析结果', '2026-10-12 08:41:20');

INSERT INTO report_feedbacks (report_id, feedback_type, content, sort_order) VALUES
  (1, 'strength',     '自我介绍覆盖了身份和求职意向，信息基本完整。', 1),
  (1, 'improvement',  '句子中间的长停顿偏多，表达被频繁打断。', 2),
  (1, 'suggestion',   '可以适当减少句子中间的长停顿，让表达更连贯。', 3),
  (1, 'suggestion',   '建议按照「背景—行动—结果」组织面试回答。', 4),
  (2, 'strength',     '语速更接近自然交谈，停顿从 8 次降到 4 次，表达更连贯。', 1),
  (2, 'improvement',  '结果部分仍偏概括，还可以补一个具体例子。', 2),
  (2, 'suggestion',   '连贯性已有提升，接下来把句中停顿保持在短停顿即可。', 3),
  (2, 'suggestion',   '回答已有背景和行动，建议在结果处补上具体收获。', 4);

INSERT INTO help_articles (article_id, title, content, sort_order) VALUES
  (1, '如何开始一次训练', '在首页点击「开始训练」进入题库，选择题目后允许使用麦克风。结束训练时，录音会保存到数据库，并按语音识别结果评分。', 1),
  (2, '报告里的分数代表什么', '表达分数按识别出的汉字数、语速和停顿计算。内容分数看识别文本有没有覆盖这道题的要点。两份分数分开显示，不能用来判断性格、心理状态或面试能否通过。较早的演示记录没有内容分数。', 2),
  (3, '如何查看上一次练习', '打开训练历史，按时间倒序查看记录。点进某条记录即可看到那一次自己的报告。', 3);

INSERT INTO user_feedbacks (feedback_id, user_id, category, content, status, created_at) VALUES
  (1, 1, '功能建议', '希望历史页能直接对比同一次题目的前后两次报告。', '待处理', '2026-10-12 09:10:00');

-- =============================================================================
-- 导入后可自行核对的查询（默认不执行）
--
-- 首页：本周 1/3，最近成绩 81，最近报告为 report_id = 2
-- SELECT * FROM v_user_dashboard WHERE user_id = 1;
--
-- 个人中心：累计 2 次，时长 143 秒，平均分 76.5
-- SELECT * FROM v_user_training_stats WHERE user_id = 1;
--
-- 历史：10 月 12 日 81 分在前，10 月 10 日 72 分在后
-- SELECT topic_title, trained_at, total_score, speech_rate, pause_count, fluency_score
-- FROM v_training_history
-- WHERE user_id = 1
-- ORDER BY trained_at DESC;
--
-- 空历史
-- SELECT * FROM v_training_history WHERE user_id = 2;
-- =============================================================================
