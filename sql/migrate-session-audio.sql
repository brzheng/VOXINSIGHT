-- 给已经导入过的 voxinsight 库补上录音表。
-- 不会删除现有训练记录。重新执行同目录的 voxinsight.sql 会清空全库，不必再跑本文件。

USE voxinsight;

CREATE TABLE IF NOT EXISTS session_audios (
  session_id  BIGINT UNSIGNED NOT NULL COMMENT '训练记录 ID',
  mime_type   VARCHAR(64)     NOT NULL COMMENT '音频 MIME，不含 codecs 参数',
  byte_size   INT UNSIGNED    NOT NULL COMMENT '音频字节数',
  audio_data  MEDIUMBLOB      NOT NULL COMMENT '音频内容',
  PRIMARY KEY (session_id),
  CONSTRAINT fk_audio_session FOREIGN KEY (session_id) REFERENCES training_sessions (session_id) ON DELETE CASCADE,
  CONSTRAINT chk_audio_size CHECK (byte_size >= 1 AND byte_size <= 8388608)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='训练录音';

UPDATE help_articles
SET content = '在首页点击「开始训练」进入题库，选择题目后允许使用麦克风。结束训练时，录音会保存到数据库，并按语音识别结果评分。'
WHERE article_id = 1;

UPDATE app_meta
SET about_text = '声析 VOXINSIGHT 是面向大学生和求职者的表达训练原型。录音会保存在数据库里，并由语音识别计算语速、停顿和流畅度。分数用来观察自己的变化，不判断性格、心理状态或面试结果。'
WHERE meta_id = 1;
