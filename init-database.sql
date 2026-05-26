-- JellyStudy 全库初始化（Docker MySQL 3307 / 容器内 3306）
-- root / 123456

CREATE DATABASE IF NOT EXISTS jellystudy DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS jellystudy_evaluate DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE jellystudy;

CREATE TABLE IF NOT EXISTS knowledge_point (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    parent_id VARCHAR(36),
    path VARCHAR(500),
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_kp_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS question (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    content TEXT,
    knowledge_point_id VARCHAR(36),
    author VARCHAR(100),
    view_count INT NOT NULL DEFAULT 0,
    like_count INT NOT NULL DEFAULT 0,
    answer_count INT NOT NULL DEFAULT 0,
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_q_kp (knowledge_point_id),
    INDEX idx_q_like (like_count),
    INDEX idx_q_view (view_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS answer (
    id VARCHAR(36) PRIMARY KEY,
    question_id VARCHAR(36) NOT NULL,
    content TEXT,
    author VARCHAR(100),
    like_count INT NOT NULL DEFAULT 0,
    comments_json TEXT,
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_a_question (question_id),
    INDEX idx_a_like (like_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE jellystudy_evaluate;

CREATE TABLE IF NOT EXISTS question_evaluation (
    id VARCHAR(36) PRIMARY KEY,
    question_id VARCHAR(36) NOT NULL,
    question_title VARCHAR(500),
    question_content TEXT,
    knowledge_points TEXT,
    difficulty VARCHAR(20),
    difficulty_description VARCHAR(500),
    evaluation_details TEXT,
    created_at DATETIME,
    INDEX idx_question_id (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS answer_evaluation (
    id VARCHAR(36) PRIMARY KEY,
    answer_id VARCHAR(36) NOT NULL,
    question_id VARCHAR(36) NOT NULL,
    answer_content TEXT,
    score INT NOT NULL,
    grade VARCHAR(10),
    evaluation_details TEXT,
    strengths TEXT,
    suggestions TEXT,
    reference_answer TEXT,
    created_at DATETIME,
    INDEX idx_answer_id (answer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
