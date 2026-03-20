-- 为 task_sample 表添加创建时间字段
ALTER TABLE task_sample
    ADD COLUMN created_at DATETIME(6) NULL;

-- 为已有数据设置创建时间（使用当前时间作为默认值）
UPDATE task_sample
SET created_at = NOW()
WHERE created_at IS NULL;

-- 修改字段为非空并添加索引
ALTER TABLE task_sample
    MODIFY created_at DATETIME(6) NOT NULL;

-- 添加索引以优化按时间排序的查询
CREATE INDEX idx_task_sample_created_at ON task_sample(created_at DESC);
