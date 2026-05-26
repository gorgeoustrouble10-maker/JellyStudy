-- 演示数据清理：每类界面仅保留 2 条（干净中文）
SET NAMES utf8mb4;

USE jellystudy;
DELETE FROM answer;
DELETE FROM question;
DELETE FROM knowledge_point;

INSERT INTO knowledge_point (id, name, description, parent_id, path, created_at, updated_at) VALUES
('kp-demo-001', '1+1=?', '小学数学与逻辑思维入门', NULL, '/1+1', NOW(), NOW()),
('kp-demo-002', 'Redis与缓存', '第十一周实验：排行榜与读缓存', NULL, '/redis', NOW(), NOW());

INSERT INTO question (id, title, content, knowledge_point_id, author, view_count, like_count, answer_count, created_at, updated_at) VALUES
('q-demo-001', '请问1+1等于几？为什么？', '请从数学定义和直观例子两方面简要说明。', 'kp-demo-001', '演示用户', 12, 3, 1, NOW(), NOW()),
('q-demo-002', 'Redis ZSET 适合做什么排行榜？', '结合 JellyStudy 热门问题榜说明 score 如何设计。', 'kp-demo-002', '演示用户', 8, 2, 1, NOW(), NOW());

INSERT INTO answer (id, question_id, content, author, like_count, comments_json, created_at, updated_at) VALUES
('a-demo-001', 'q-demo-001', '1+1=2。因为 1 个单位再加 1 个单位，合起来是 2 个单位（皮亚诺公理/加法定义）。', '演示用户', 5, '[]', NOW(), NOW()),
('a-demo-002', 'q-demo-002', 'ZSET 适合按分数排序的榜，例如本系统热门榜：score=点赞×3+回答×2+浏览×0.1，用 ZREVRANGE 取 TopN。', '演示用户', 4, '[]', NOW(), NOW());

USE jellystudy_evaluate;
DELETE FROM answer_evaluation;
DELETE FROM question_evaluation;

INSERT INTO question_evaluation (id, question_id, question_title, question_content, knowledge_points, difficulty, difficulty_description, evaluation_details, created_at) VALUES
('qe-demo-001', 'q-demo-001', '请问1+1等于几？为什么？', '请从数学定义和直观例子两方面简要说明。', '["数学基础","加法"]', 'EASY', '简单：考察基础概念', '【演示】问题清晰，适合入门学习者。', NOW()),
('qe-demo-002', 'q-demo-002', 'Redis ZSET 适合做什么排行榜？', '结合 JellyStudy 热门问题榜说明 score 如何设计。', '["Redis","ZSET","排行榜"]', 'MEDIUM', '中等：需要理解数据结构', '【演示】与课程 Redis 实验高度相关。', NOW());

INSERT INTO answer_evaluation (id, answer_id, question_id, answer_content, score, grade, evaluation_details, strengths, suggestions, reference_answer, created_at) VALUES
('ae-demo-001', 'a-demo-001', 'q-demo-001', '1+1=2。因为 1 个单位再加 1 个单位，合起来是 2 个单位。', 92, 'A', '回答正确且给出理由。', '["结论正确","有简要解释"]', '["可补充更多例子"]', '1+1=2，可用数轴或实物举例。', NOW()),
('ae-demo-002', 'a-demo-002', 'q-demo-002', 'ZSET 适合按分数排序的榜...', 88, 'B', '结合项目场景，较好。', '["联系本项目实现"]', '["可补充复杂度说明"]', 'ZSET 支持 O(logN) 更新与范围查询。', NOW());
