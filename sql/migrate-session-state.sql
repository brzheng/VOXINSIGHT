-- 给已有库补上训练状态可恢复，以及按题目要点计算的内容分数。
-- 不会删除训练记录。重新导入同目录的 voxinsight.sql 会清空全库，不必再跑本文件。

USE voxinsight;

CREATE TABLE IF NOT EXISTS topic_checkpoints (
  checkpoint_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '要点 ID',
  topic_id      BIGINT UNSIGNED NOT NULL COMMENT '题目 ID',
  label         VARCHAR(40)     NOT NULL COMMENT '要点名称',
  keywords      VARCHAR(200)    NOT NULL COMMENT '命中关键词，逗号分隔',
  sort_order    TINYINT UNSIGNED NOT NULL COMMENT '排序',
  PRIMARY KEY (checkpoint_id),
  UNIQUE KEY uk_topic_checkpoint (topic_id, label),
  CONSTRAINT fk_checkpoint_topic FOREIGN KEY (topic_id) REFERENCES topics (topic_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目内容要点';

INSERT INTO topic_checkpoints (topic_id, label, keywords, sort_order)
SELECT seed.topic_id, seed.label, seed.keywords, seed.sort_order
FROM (
  SELECT 1 AS topic_id, '身份' AS label, '我是,我叫,名叫' AS keywords, 1 AS sort_order
  UNION ALL SELECT 1, '学校或专业', '大学,专业,学院,学校', 2
  UNION ALL SELECT 1, '经历', '项目,实习,负责,经历', 3
  UNION ALL SELECT 1, '意向', '希望,应聘,求职,岗位', 4
  UNION ALL SELECT 2, '优势', '优势,擅长,长处', 1
  UNION ALL SELECT 2, '例子', '例如,比如,曾经,项目', 2
  UNION ALL SELECT 2, '结果', '所以,因此,完成,学会', 3
  UNION ALL SELECT 3, '背景', '问题,困难,当时,遇到', 1
  UNION ALL SELECT 3, '行动', '解决,沟通,分析,尝试', 2
  UNION ALL SELECT 3, '结果', '最后,结果,完成', 3
  UNION ALL SELECT 4, '分歧', '意见,分歧,不一致,冲突', 1
  UNION ALL SELECT 4, '处理', '沟通,倾听,讨论,协调', 2
  UNION ALL SELECT 4, '结果', '达成,一致,方案', 3
  UNION ALL SELECT 5, '主题', '关于,话题,介绍', 1
  UNION ALL SELECT 5, '细节', '因为,比如,例如,首先', 2
  UNION ALL SELECT 5, '收束', '所以,总之,总结', 3
  UNION ALL SELECT 6, '时间', '早上,上午,中午,下午,晚上', 1
  UNION ALL SELECT 6, '活动', '上课,吃饭,学习,工作,运动,睡觉', 2
  UNION ALL SELECT 6, '感受', '觉得,感觉,开心,顺利', 3
) AS seed
WHERE NOT EXISTS (
  SELECT 1 FROM topic_checkpoints c
  WHERE c.topic_id = seed.topic_id AND c.label = seed.label
);

SET @has_content := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'voxinsight' AND TABLE_NAME = 'analysis_reports' AND COLUMN_NAME = 'content_score'
);
SET @sql := IF(
  @has_content = 0,
  'ALTER TABLE analysis_reports ADD COLUMN content_score TINYINT UNSIGNED NULL COMMENT ''内容分数，0–100'' AFTER transcript, ADD COLUMN content_summary VARCHAR(500) NULL COMMENT ''内容要点覆盖说明'' AFTER content_score',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_check := (
  SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
  WHERE TABLE_SCHEMA = 'voxinsight' AND TABLE_NAME = 'analysis_reports' AND CONSTRAINT_NAME = 'chk_report_content'
);
SET @sql := IF(
  @has_check = 0,
  'ALTER TABLE analysis_reports ADD CONSTRAINT chk_report_content CHECK (content_score IS NULL OR content_score <= 100)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE help_articles
SET content = '表达分数按识别出的汉字数、语速和停顿计算。内容分数看识别文本有没有覆盖这道题的要点。两份分数分开显示，不能用来判断性格、心理状态或面试能否通过。较早的演示记录没有内容分数。'
WHERE article_id = 2;
