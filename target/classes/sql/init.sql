-- ============================================================================
-- KingBase数据库初始化脚本
-- ============================================================================
-- 说明：
--   1. 此脚本用于初始化Word生成工具系统的数据库表结构
--   2. 包含模板管理表和生成记录表
--   3. 注意：此脚本需要在已连接到KingBase数据库的情况下执行
--
-- 使用方法：
--   ksql -U SYSTEM -d report_system -f init.sql
--
-- 数据库：report_system
-- 用户名：SYSTEM
-- 密码：password（根据实际配置修改）
-- ============================================================================


-- ============================================================================
-- 表1: report_template (报表模板表)
-- 功能：存储Word模板文件信息及相关配置
-- ============================================================================

-- 删除已存在的表（避免重复创建错误）
DROP TABLE IF EXISTS report_template CASCADE;

-- 创建模板表
CREATE TABLE report_template (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT,
    file_type VARCHAR(50),
    file_path VARCHAR(500),
    api_url VARCHAR(500),
    groovy_script TEXT,
    status INTEGER DEFAULT 1,
    created_by VARCHAR(100),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

COMMENT ON COLUMN report_template.id IS '模板ID，主键，自增';
COMMENT ON COLUMN report_template.name IS '模板名称';
COMMENT ON COLUMN report_template.description IS '模板描述说明';
COMMENT ON COLUMN report_template.file_name IS '原始文件名';
COMMENT ON COLUMN report_template.file_size IS '文件大小（字节）';
COMMENT ON COLUMN report_template.file_type IS '文件类型（如docx）';
COMMENT ON COLUMN report_template.file_path IS '文件在MinIO中的存储路径';
COMMENT ON COLUMN report_template.api_url IS '外部数据接口URL（可选），用于获取数据';
COMMENT ON COLUMN report_template.groovy_script IS 'Groovy脚本内容（用于存储脚本文本）';
COMMENT ON COLUMN report_template.status IS '状态：1-启用，0-禁用';
COMMENT ON COLUMN report_template.created_by IS '创建人';
COMMENT ON COLUMN report_template.create_time IS '创建时间';
COMMENT ON COLUMN report_template.update_time IS '更新时间';
COMMENT ON COLUMN report_template.deleted IS '逻辑删除标记：0-未删除，1-已删除';
COMMENT ON TABLE report_template IS '报表模板表：存储Word模板文件信息及相关配置';


-- ============================================================================
-- 表2: report_generation (报表生成记录表)
-- 功能：存储Word生成历史记录
-- ============================================================================

-- 删除已存在的表（避免重复创建错误）
DROP TABLE IF EXISTS report_generation CASCADE;

-- 创建生成记录表
CREATE TABLE report_generation (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT,
    template_name VARCHAR(255),
    request_data TEXT,
    response_data TEXT,
    data_source VARCHAR(20),
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT,
    file_path VARCHAR(500),
    file_url VARCHAR(500),
    status INTEGER DEFAULT 0,
    error_message TEXT,
    execution_log TEXT,
    created_by VARCHAR(100),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

COMMENT ON COLUMN report_generation.id IS '记录ID，主键，自增';
COMMENT ON COLUMN report_generation.template_id IS '关联的模板ID';
COMMENT ON COLUMN report_generation.template_name IS '模板名称（冗余字段，便于查询）';
COMMENT ON COLUMN report_generation.request_data IS '请求数据（JSON格式）';
COMMENT ON COLUMN report_generation.response_data IS '响应数据';
COMMENT ON COLUMN report_generation.data_source IS '数据来源：API 或 MANUAL';
COMMENT ON COLUMN report_generation.file_name IS '生成的文件名';
COMMENT ON COLUMN report_generation.file_size IS '文件大小（字节）';
COMMENT ON COLUMN report_generation.file_path IS '文件在MinIO中的存储路径';
COMMENT ON COLUMN report_generation.file_url IS '文件访问URL';
COMMENT ON COLUMN report_generation.status IS '状态：0-生成中，1-成功，2-失败';
COMMENT ON COLUMN report_generation.error_message IS '错误信息（失败时记录，已包含在 execution_log 中）';
COMMENT ON COLUMN report_generation.execution_log IS 'Groovy脚本执行日志（包含INFO/WARN/ERROR级别日志和异常堆栈）';
COMMENT ON COLUMN report_generation.created_by IS '创建人';
COMMENT ON COLUMN report_generation.create_time IS '创建时间';
COMMENT ON COLUMN report_generation.update_time IS '更新时间';
COMMENT ON COLUMN report_generation.deleted IS '逻辑删除标记：0-未删除，1-已删除';
COMMENT ON TABLE report_generation IS '报表生成记录表：存储Word生成历史记录';


-- ============================================================================
-- 索引创建
-- 功能：提高查询性能
-- ============================================================================

-- 删除已存在的索引
DROP INDEX IF EXISTS idx_template_name;
DROP INDEX IF EXISTS idx_template_deleted;
DROP INDEX IF EXISTS idx_generation_template_id;
DROP INDEX IF EXISTS idx_generation_deleted;

-- 模板表索引
CREATE INDEX idx_template_name ON report_template(name);           -- 按模板名称查询
CREATE INDEX idx_template_deleted ON report_template(deleted);       -- 按删除标记查询

-- 生成记录表索引
CREATE INDEX idx_generation_template_id ON report_generation(template_id);  -- 按模板ID查询
CREATE INDEX idx_generation_deleted ON report_generation(deleted);        -- 按删除标记查询


-- ============================================================================
-- 测试数据插入
-- 功能：插入示例数据，方便测试系统功能
-- ============================================================================

-- 插入销售报表模板示例
INSERT INTO report_template (
    name,
    description,
    file_name,
    file_size,
    file_type,
    file_path,
    api_url,
    api_params,
    created_by
)
VALUES (
    '销售报表模板',
    '月度销售数据报表模板',
    'sales_report_template.docx',
    102400,
    'docx',
    'templates/sales_report_template.docx',
    'http://api.example.com/sales/data',
    '{"month":"string","year":"string"}',
    'admin'
);

-- 插入财务报表模板示例
INSERT INTO report_template (
    name,
    description,
    file_name,
    file_size,
    file_type,
    file_path,
    api_url,
    api_params,
    created_by
)
VALUES (
    '财务报表模板',
    '季度财务分析报表模板',
    'finance_report_template.docx',
    153600,
    'docx',
    'templates/finance_report_template.docx',
    'http://api.example.com/finance/data',
    '{"quarter":"string","year":"string"}',
    'admin'
);


-- ============================================================================
-- 执行完成提示
-- ============================================================================
-- 说明：表结构和测试数据已创建完成
-- 下一步：启动Spring Boot应用，访问 http://localhost:8080/report-system/
-- ============================================================================
