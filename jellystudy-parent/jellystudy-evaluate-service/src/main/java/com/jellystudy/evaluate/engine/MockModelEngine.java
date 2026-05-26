package com.jellystudy.evaluate.engine;

import com.jellystudy.common.entity.AnswerEvaluationDTO;
import com.jellystudy.common.entity.QuestionEvaluationDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 模拟大模型评估引擎
 * 实现知识点提取、难度分级、答案评分功能
 */
@Slf4j
@Component
public class MockModelEngine implements ModelEngine {

    @Value("${evaluate.model.timeout:60000}")
    private long timeout;

    @Value("${evaluate.model.retry-count:3}")
    private int retryCount;

    /**
     * 预定义知识点库
     */
    private static final List<String> KNOWLEDGE_POINTS = Arrays.asList(
            "Java基础", "数据结构", "算法", "数据库", "计算机网络",
            "操作系统", "面向对象", "设计模式", "Spring Boot", "Dubbo",
            "微服务", "分布式", "并发编程", "JVM", "SQL优化"
    );

    /**
     * 评估问题
     */
    public QuestionEvaluationDTO evaluateQuestion(String questionId, String title, String content) {
        log.info("开始评估问题: {}", title);
        
        // 模拟大模型调用延迟
        simulateDelay(500, 1500);
        
        // 提取知识点（基于关键词匹配）
        List<String> extractedPoints = extractKnowledgePoints(title, content);
        
        // 难度分级
        String difficulty = analyzeDifficulty(title, content, extractedPoints);
        String difficultyDesc = getDifficultyDescription(difficulty);
        
        // 生成评估详情
        String details = generateQuestionEvaluationDetails(title, extractedPoints, difficulty);
        
        return QuestionEvaluationDTO.builder()
                .id(UUID.randomUUID().toString())
                .questionId(questionId)
                .questionTitle(title)
                .questionContent(content)
                .knowledgePoints(extractedPoints)
                .difficulty(difficulty)
                .difficultyDescription(difficultyDesc)
                .evaluationDetails(details)
                .createdAt(new Date())
                .build();
    }

    /**
     * 评估答案
     */
    public AnswerEvaluationDTO evaluateAnswer(String answerId, String questionId, 
                                              String questionContent, String answerContent) {
        log.info("开始评估答案, answerId: {}", answerId);
        
        // 模拟大模型调用延迟
        simulateDelay(800, 2000);
        
        // 计算得分（基于答案长度、关键词匹配等因素）
        int score = calculateScore(questionContent, answerContent);
        String grade = getGrade(score);
        
        // 生成评估详情
        List<String> strengths = generateStrengths(answerContent);
        List<String> suggestions = generateSuggestions(answerContent);
        String details = generateAnswerEvaluationDetails(score, grade, strengths, suggestions);
        
        // 生成参考答案
        String referenceAnswer = generateReferenceAnswer(questionContent);
        
        return AnswerEvaluationDTO.builder()
                .id(UUID.randomUUID().toString())
                .answerId(answerId)
                .questionId(questionId)
                .answerContent(answerContent)
                .score(score)
                .grade(grade)
                .evaluationDetails(details)
                .strengths(strengths)
                .suggestions(suggestions)
                .referenceAnswer(referenceAnswer)
                .createdAt(new Date())
                .build();
    }

    /**
     * 提取知识点
     */
    private List<String> extractKnowledgePoints(String title, String content) {
        List<String> points = new ArrayList<>();
        String text = (title + " " + content).toLowerCase();
        
        for (String point : KNOWLEDGE_POINTS) {
            if (text.contains(point.toLowerCase())) {
                points.add(point);
            }
        }
        
        // 如果没有匹配到，随机返回一些知识点
        if (points.isEmpty()) {
            Random random = new Random();
            int count = random.nextInt(3) + 1;
            for (int i = 0; i < count; i++) {
                points.add(KNOWLEDGE_POINTS.get(random.nextInt(KNOWLEDGE_POINTS.size())));
            }
        }
        
        return points;
    }

    /**
     * 分析难度
     */
    private String analyzeDifficulty(String title, String content, List<String> knowledgePoints) {
        int complexity = 0;
        
        // 根据内容长度判断
        int length = (title + content).length();
        if (length > 500) complexity += 2;
        else if (length > 200) complexity += 1;
        
        // 根据知识点数量判断
        if (knowledgePoints.size() > 3) complexity += 2;
        else if (knowledgePoints.size() > 1) complexity += 1;
        
        // 根据关键词判断
        String text = (title + content).toLowerCase();
        if (text.contains("原理") || text.contains("底层") || text.contains("实现")) {
            complexity += 2;
        }
        if (text.contains("简单") || text.contains("基础")) {
            complexity -= 1;
        }
        if (text.contains("设计") || text.contains("架构")) {
            complexity += 1;
        }
        
        // 判断难度等级
        if (complexity >= 4) return "HARD";
        if (complexity >= 2) return "MEDIUM";
        return "EASY";
    }

    /**
     * 获取难度描述
     */
    private String getDifficultyDescription(String difficulty) {
        return switch (difficulty) {
            case "EASY" -> "简单：适合入门级学习者，考察基础知识";
            case "MEDIUM" -> "中等：需要一定的知识积累和分析能力";
            case "HARD" -> "困难：需要深入理解和综合运用知识";
            default -> "未知";
        };
    }

    /**
     * 计算答案得分
     */
    private int calculateScore(String questionContent, String answerContent) {
        int score = 60; // 基础分
        
        // 答案长度评分
        int answerLength = answerContent.length();
        if (answerLength > 500) score += 15;
        else if (answerLength > 200) score += 10;
        else if (answerLength > 50) score += 5;
        
        // 关键词匹配评分
        String questionLower = questionContent.toLowerCase();
        String answerLower = answerContent.toLowerCase();
        String[] keywords = {"定义", "原理", "实现", "特点", "优缺点", "举例", "步骤", "方法"};
        for (String keyword : keywords) {
            if (questionLower.contains(keyword) && answerLower.contains(keyword)) {
                score += 3;
            }
        }
        
        // 格式评分（包含列表、分段等）
        if (answerContent.contains("\n") && answerContent.lines().count() > 3) {
            score += 5;
        }
        if (answerContent.contains("1.") || answerContent.contains("- ")) {
            score += 5;
        }
        
        // 随机波动（模拟大模型评分的不确定性）
        Random random = new Random();
        score += random.nextInt(11) - 5; // -5 到 +5
        
        return Math.min(100, Math.max(0, score));
    }

    /**
     * 获取评分等级
     */
    private String getGrade(int score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 60) return "C";
        return "D";
    }

    /**
     * 生成优点列表
     */
    private List<String> generateStrengths(String answerContent) {
        List<String> strengths = new ArrayList<>();
        
        if (answerContent.length() > 100) {
            strengths.add("回答内容详实，信息量充足");
        }
        if (answerContent.contains("\n")) {
            strengths.add("回答结构清晰，层次分明");
        }
        if (answerContent.contains("首先") || answerContent.contains("其次") || 
            answerContent.contains("1.") || answerContent.contains("2.")) {
            strengths.add("逻辑条理清晰，论证充分");
        }
        if (answerContent.length() > 200) {
            strengths.add("分析深入，体现了对知识的理解");
        }
        
        if (strengths.isEmpty()) {
            strengths.add("回答符合基本要求");
        }
        
        return strengths;
    }

    /**
     * 生成改进建议
     */
    private List<String> generateSuggestions(String answerContent) {
        List<String> suggestions = new ArrayList<>();
        
        if (answerContent.length() < 100) {
            suggestions.add("建议增加更多细节和示例");
        }
        if (!answerContent.contains("\n") || answerContent.lines().count() < 3) {
            suggestions.add("建议分点阐述，提高可读性");
        }
        if (!answerContent.contains("例如") && !answerContent.contains("举例")) {
            suggestions.add("建议结合实际例子说明");
        }
        
        if (suggestions.isEmpty()) {
            suggestions.add("回答质量较高，继续保持");
        }
        
        return suggestions;
    }

    /**
     * 生成问题评估详情
     */
    private String generateQuestionEvaluationDetails(String title, 
                                                      List<String> points, 
                                                      String difficulty) {
        return String.format("【问题评估报告】\n" +
                "问题标题：%s\n" +
                "提取知识点：%s\n" +
                "难度等级：%s\n" +
                "评估时间：%s", 
                title, String.join("、", points), difficulty, new Date());
    }

    /**
     * 生成答案评估详情
     */
    private String generateAnswerEvaluationDetails(int score, String grade, 
                                                   List<String> strengths, 
                                                   List<String> suggestions) {
        return String.format("【答案评估报告】\n" +
                "得分：%d分\n" +
                "等级：%s\n" +
                "优点：%s\n" +
                "建议：%s\n" +
                "评估时间：%s", 
                score, grade, 
                String.join("；", strengths), 
                String.join("；", suggestions), 
                new Date());
    }

    /**
     * 生成参考答案
     */
    private String generateReferenceAnswer(String questionContent) {
        return String.format("【参考答案】\n\n" +
                "针对问题\"%s\"，建议从以下几个方面进行回答：\n" +
                "1. 明确核心概念和定义\n" +
                "2. 分析相关原理和机制\n" +
                "3. 结合实际应用场景\n" +
                "4. 对比优缺点和适用范围\n" +
                "5. 给出具体示例和代码（如适用）", 
                questionContent.length() > 50 ? questionContent.substring(0, 50) + "..." : questionContent);
    }

    /**
     * 模拟网络延迟
     */
    private void simulateDelay(long minMs, long maxMs) {
        try {
            Thread.sleep(minMs + (long) (Math.random() * (maxMs - minMs)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
