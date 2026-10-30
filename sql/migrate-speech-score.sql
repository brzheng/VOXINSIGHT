-- 让已有库可以保存语音识别评分。不会删除训练记录。
-- 重新导入同目录的 voxinsight.sql 会清空全库，不必再跑本文件。

USE voxinsight;

SET @has_transcript := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'voxinsight'
    AND TABLE_NAME = 'analysis_reports'
    AND COLUMN_NAME = 'transcript'
);
SET @add_transcript := IF(
  @has_transcript = 0,
  'ALTER TABLE analysis_reports ADD COLUMN transcript TEXT NULL COMMENT ''语音识别文本，旧的模拟报告为空'' AFTER result_label',
  'SELECT 1'
);
PREPARE stmt FROM @add_transcript;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE analysis_reports DROP CHECK chk_report_rate;
ALTER TABLE analysis_reports ADD CONSTRAINT chk_report_rate CHECK (speech_rate <= 2000);
ALTER TABLE analysis_reports DROP CHECK chk_report_pause;
ALTER TABLE analysis_reports ADD CONSTRAINT chk_report_pause CHECK (pause_count <= 6000);
ALTER TABLE analysis_reports ALTER COLUMN result_label SET DEFAULT '语音识别结果';

UPDATE help_articles
SET content = '在首页点击「开始训练」进入题库，选择题目后允许使用麦克风。结束训练时，录音会保存到数据库，并按语音识别结果评分。'
WHERE article_id = 1;

UPDATE help_articles
SET content = '语速按识别出的汉字数和录音时长计算，停顿按波形中的静音段计算。分数只反映这一次说得是否连贯，不能用来判断性格、心理状态或面试能否通过。较早的演示记录仍标为模拟分析结果。'
WHERE article_id = 2;

UPDATE app_meta
SET about_text = '声析 VOXINSIGHT 是面向大学生和求职者的表达训练原型。录音会保存在数据库里，并由语音识别计算语速、停顿和流畅度。分数用来观察自己的变化，不判断性格、心理状态或面试结果。'
WHERE meta_id = 1;
