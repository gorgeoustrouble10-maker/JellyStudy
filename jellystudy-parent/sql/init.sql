-- ==============================================
-- JellyStudy 评估服务数据库初始化脚本
-- ==============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS jellystudy_evaluate 
  DEFAULT CHARACTER SET utf8mb4 
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE jellystudy_evaluate;

-- ==============================================
-- 问题评估记录表
-- ==============================================
CREATE TABLE IF NOT EXISTS question_evaluation (
    id VARCHAR(36) PRIMARY KEY COMMENT '评估记录ID',
    question_id VARCHAR(36) NOT NULL COMMENT '问题ID',
    question_title VARCHAR(500) COMMENT '问题标题',
    question_content TEXT COMMENT '问题内容',
    knowledge_points TEXT COMMENT '提取的知识点（JSON数组格式）',
    difficulty VARCHAR(20) COMMENT '难度等级：EASY/MEDIUM/HARD',
    difficulty_description VARCHAR(500) COMMENT '难度描述',
    evaluation_details TEXT COMMENT '评估详情',
    created_at DATETIME COMMENT '创建时间',
    INDEX idx_question_id (question_id),
    INDEX idx_difficulty (difficulty)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问题评估记录表';

-- ==============================================
-- 答案评估记录表
-- ==============================================
CREATE TABLE IF NOT EXISTS answer_evaluation (
    id VARCHAR(36) PRIMARY KEY COMMENT '评估记录ID',
    answer_id VARCHAR(36) NOT NULL COMMENT '答案ID',
    question_id VARCHAR(36) COMMENT '问题ID',
    answer_content TEXT COMMENT '用户答案内容',
    score INT NOT NULL COMMENT '得分（0-100）',
    grade VARCHAR(10) COMMENT '评分等级：A/B/C/D',
    evaluation_details TEXT COMMENT '评估详情',
    strengths TEXT COMMENT '优点（JSON数组格式）',
    suggestions TEXT COMMENT '改进建议（JSON数组格式）',
    reference_answer TEXT COMMENT '参考答案',
    created_at DATETIME COMMENT '创建时间',
    INDEX idx_answer_id (answer_id),
    INDEX idx_question_id (question_id),
    INDEX idx_grade (grade),
    INDEX idx_score (score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答案评估记录表';

-- ==============================================
-- 初始化数据（可选）
-- ==============================================
INSERT INTO question_evaluation (id, question_id, question_title, question_content, knowledge_points, difficulty, difficulty_description, evaluation_details, created_at)
VALUES 
('eval_q_001', 'q_001', '什么是JVM？', '请简述Java虚拟机的定义和作用', '["JVM","Java基础"]', 'EASY', '简单：适合入门级学习者，考察基础知识', '【问题评估报告】\n问题标题：什么是JVM？\n提取知识点：JVM、Java基础\n难度等级：EASY\n评估时间：2024-01-01 10:00:00', NOW()),
('eval_q_002', 'q_002', 'Spring Boot自动配置原理', '请详细说明Spring Boot自动配置的实现原理', '["Spring Boot","微服务"]', 'HARD', '困难：需要深入理解和综合运用知识', '【问题评估报告】\n问题标题：Spring Boot自动配置原理\n提取知识点：Spring Boot、微服务\n难度等级：HARD\n评估时间：2024-01-01 10:00:01', NOW());

INSERT INTO answer_evaluation (id, answer_id, question_id, answer_content, score, grade, evaluation_details, strengths, suggestions, reference_answer, created_at)
VALUES 
('eval_a_001', 'a_001', 'q_001', 'JVM是Java虚拟机，负责运行Java程序。它将字节码转换为机器码执行。', 85, 'B', '【答案评估报告】\n得分：85分\n等级：B\n优点：回答准确；结构清晰\n建议：可以增加更多细节\n评估时间：2024-01-01 10:01:00', '["回答准确","结构清晰"]', '["可以增加更多细节","建议结合实际例子"]', '【参考答案】\nJVM（Java Virtual Machine）是Java虚拟机，是运行Java程序的核心组件...', NOW());

-- ==============================================
-- 脚本执行完成
-- ==============================================
SELECT '数据库初始化完成' AS message;
