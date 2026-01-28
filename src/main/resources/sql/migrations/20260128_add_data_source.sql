-- 添加 data_source 字段到生成记录表，用于持久化数据来源
ALTER TABLE report_generation
ADD COLUMN IF NOT EXISTS data_source VARCHAR(20);

COMMENT ON COLUMN report_generation.data_source IS '数据来源：API 或 MANUAL';
