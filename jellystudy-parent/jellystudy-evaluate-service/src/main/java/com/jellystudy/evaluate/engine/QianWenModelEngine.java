package com.jellystudy.evaluate.engine;

import com.jellystudy.common.entity.AnswerEvaluationDTO;
import com.jellystudy.common.entity.QuestionEvaluationDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jellystudy.evaluate.config.EvaluateModelProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

/**
 * 千问大模型评估引擎（REST API版本）
 * 基于阿里DashScope REST API实现真实大模型评估功能
 * 支持知识点提取、难度分级、答案评分
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QianWenModelEngine implements ModelEngine {

    private final EvaluateModelProperties modelProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * 评估问题
     * - 调用千问大模型提取知识点
     * - 进行难度分级（EASY/MEDIUM/HARD）
     */
    @Override
    public QuestionEvaluationDTO evaluateQuestion(String questionId, String title, String content) {
        log.info("使用千问大模型评估问题: {}", title);
        
        String prompt = buildQuestionEvaluationPrompt(title, content);
        
        try {
            String response = callQianWenModel(prompt);
            return parseQuestionEvaluationResponse(questionId, title, content, response);
        } catch (Exception e) {
            log.error("千问大模型调用失败，使用降级策略", e);
            return createFallbackQuestionEvaluation(questionId, title, content);
        }
    }

    /**
     * 评估答案
     * - 调用千问大模型对答案进行评分
     * - 提供评分详情和改进建议
     */
    @Override
    public AnswerEvaluationDTO evaluateAnswer(String answerId, String questionId, 
                                              String questionContent, String answerContent) {
        log.info("使用千问大模型评估答案, answerId: {}", answerId);
        
        String prompt = buildAnswerEvaluationPrompt(questionContent, answerContent);
        
        try {
            String response = callQianWenModel(prompt);
            return parseAnswerEvaluationResponse(answerId, questionId, answerContent, response);
        } catch (Exception e) {
            log.error("千问大模型调用失败，使用降级策略", e);
            return createFallbackAnswerEvaluation(answerId, questionId, answerContent);
        }
    }

    /**
     * 构建问题评估Prompt
     */
    private String buildQuestionEvaluationPrompt(String title, String content) {
        return String.format("""
            请对以下技术问题进行评估分析：
            
            问题标题：%s
            问题内容：%s
            
            请按照以下JSON格式输出评估结果：
            {
                "knowledgePoints": ["知识点1", "知识点2", "知识点3"],
                "difficulty": "EASY|MEDIUM|HARD",
                "difficultyDescription": "难度描述",
                "evaluationDetails": "详细评估报告"
            }
            
            要求：
            1. knowledgePoints：提取问题涉及的3-5个核心技术知识点
            2. difficulty：根据问题复杂度判断难度等级
            3. difficultyDescription：简要说明难度判断依据
            4. evaluationDetails：详细的评估分析报告
            
            注意：请只输出JSON格式，不要包含其他内容。
            """, title, content);
    }

    /**
     * 构建答案评估Prompt
     */
    private String buildAnswerEvaluationPrompt(String questionContent, String answerContent) {
        return String.format("""
            请对以下答案进行评分评估：
            
            问题：%s
            
            答案：%s
            
            请按照以下JSON格式输出评估结果（score 填你判定的 0-100 整数，禁止固定 85）：
            {
                "score": 0,
                "grade": "A|B|C|D",
                "evaluationDetails": "评估详情",
                "strengths": ["优点1", "优点2"],
                "suggestions": ["建议1", "建议2"],
                "referenceAnswer": "参考答案"
            }
            
            评分标准（100分制）：
            - 90-100分：A（优秀）- 答案准确、完整、深入
            - 80-89分：B（良好）- 答案较准确、较完整
            - 60-79分：C（合格）- 答案基本符合要求
            - 0-59分：D（不合格）- 答案不准确或不完整
            
            要求：
            1. score：根据实际质量独立判定，不同答案分数须有梯度
            2. grade：根据分数给出等级
            3. evaluationDetails：详细的评分理由
            4. strengths：答案的优点
            5. suggestions：改进建议
            6. referenceAnswer：针对该问题的理想回答
            
            注意：请只输出JSON格式，不要包含其他内容。
            """, questionContent, answerContent);
    }

    /**
     * 调用千问大模型（REST API方式）
     * 基于阿里云百炼大模型服务平台
     */
    private String callQianWenModel(String prompt) throws IOException, InterruptedException {
        String apiKey = modelProperties.getApiKey();
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("your-api-key-here")) {
            log.warn("使用Mock模式：未配置有效的百炼API Key");
            throw new RuntimeException("请配置有效的百炼API Key（前往阿里云大模型服务平台百炼获取）");
        }

        String jsonBody = String.format("""
            {
                "model": "%s",
                "input": {
                    "messages": [
                        {
                            "role": "user",
                            "content": "%s"
                        }
                    ]
                },
                "parameters": {
                    "temperature": 0.3,
                    "max_tokens": 2048
                }
            }
            """, modelProperties.getModelName(), escapeJson(prompt));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(java.time.Duration.ofMillis(modelProperties.getTimeout()))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("调用千问大模型失败，状态码: " + response.statusCode() + ", 响应: " + response.body());
        }

        // 解析响应
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode output = root.get("output");
        if (output != null && output.has("text")) {
            return output.get("text").asText();
        }
        
        throw new RuntimeException("千问大模型返回格式异常: " + response.body());
    }

    /**
     * JSON转义
     */
    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r");
    }

    /**
     * 解析问题评估响应
     */
    private QuestionEvaluationDTO parseQuestionEvaluationResponse(String questionId, String title, 
                                                                  String content, String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            
            List<String> knowledgePoints = new ArrayList<>();
            JsonNode pointsNode = jsonNode.get("knowledgePoints");
            if (pointsNode != null && pointsNode.isArray()) {
                for (JsonNode point : pointsNode) {
                    knowledgePoints.add(point.asText());
                }
            }
            
            String difficulty = jsonNode.has("difficulty") ? jsonNode.get("difficulty").asText() : "MEDIUM";
            String difficultyDesc = jsonNode.has("difficultyDescription") ? 
                    jsonNode.get("difficultyDescription").asText() : "中等难度";
            String details = jsonNode.has("evaluationDetails") ? 
                    jsonNode.get("evaluationDetails").asText() : "评估完成";
            
            // 验证难度值
            if (!difficulty.equals("EASY") && !difficulty.equals("MEDIUM") && !difficulty.equals("HARD")) {
                difficulty = "MEDIUM";
            }
            
            return QuestionEvaluationDTO.builder()
                    .id(UUID.randomUUID().toString())
                    .questionId(questionId)
                    .questionTitle(title)
                    .questionContent(content)
                    .knowledgePoints(knowledgePoints.isEmpty() ? List.of("未知知识点") : knowledgePoints)
                    .difficulty(difficulty)
                    .difficultyDescription(difficultyDesc)
                    .evaluationDetails(details)
                    .createdAt(new Date())
                    .build();
                    
        } catch (JsonProcessingException e) {
            log.error("解析问题评估响应失败，使用降级策略", e);
            return createFallbackQuestionEvaluation(questionId, title, content);
        }
    }

    /**
     * 解析答案评估响应
     */
    private AnswerEvaluationDTO parseAnswerEvaluationResponse(String answerId, String questionId, 
                                                              String answerContent, String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            
            int score = jsonNode.has("score") ? jsonNode.get("score").asInt(60) : 60;
            String grade = jsonNode.has("grade") ? jsonNode.get("grade").asText() : "C";
            String details = jsonNode.has("evaluationDetails") ? 
                    jsonNode.get("evaluationDetails").asText() : "评估完成";
            
            List<String> strengths = new ArrayList<>();
            JsonNode strengthsNode = jsonNode.get("strengths");
            if (strengthsNode != null && strengthsNode.isArray()) {
                for (JsonNode strength : strengthsNode) {
                    strengths.add(strength.asText());
                }
            }
            
            List<String> suggestions = new ArrayList<>();
            JsonNode suggestionsNode = jsonNode.get("suggestions");
            if (suggestionsNode != null && suggestionsNode.isArray()) {
                for (JsonNode suggestion : suggestionsNode) {
                    suggestions.add(suggestion.asText());
                }
            }
            
            String referenceAnswer = jsonNode.has("referenceAnswer") ? 
                    jsonNode.get("referenceAnswer").asText() : "请参考相关文档";
            
            // 验证分数范围
            score = Math.min(100, Math.max(0, score));
            
            // 验证等级
            if (!grade.matches("[ABCD]")) {
                grade = score >= 90 ? "A" : score >= 80 ? "B" : score >= 60 ? "C" : "D";
            }
            
            return AnswerEvaluationDTO.builder()
                    .id(UUID.randomUUID().toString())
                    .answerId(answerId)
                    .questionId(questionId)
                    .answerContent(answerContent)
                    .score(score)
                    .grade(grade)
                    .evaluationDetails(details)
                    .strengths(strengths.isEmpty() ? List.of("回答符合基本要求") : strengths)
                    .suggestions(suggestions.isEmpty() ? List.of("继续努力") : suggestions)
                    .referenceAnswer(referenceAnswer)
                    .createdAt(new Date())
                    .build();
                    
        } catch (JsonProcessingException e) {
            log.error("解析答案评估响应失败，使用降级策略", e);
            return createFallbackAnswerEvaluation(answerId, questionId, answerContent);
        }
    }

    /**
     * 创建降级问题评估结果
     */
    private QuestionEvaluationDTO createFallbackQuestionEvaluation(String questionId, 
                                                                   String questionTitle, 
                                                                   String questionContent) {
        log.warn("使用降级策略处理问题评估, questionId: {}", questionId);
        return QuestionEvaluationDTO.builder()
                .id(UUID.randomUUID().toString())
                .questionId(questionId)
                .questionTitle(questionTitle)
                .questionContent(questionContent)
                .knowledgePoints(List.of("未知知识点"))
                .difficulty("MEDIUM")
                .difficultyDescription("中等：需要一定的知识积累和分析能力")
                .evaluationDetails("【问题评估报告】\n评估服务暂时不可用，使用默认评估结果")
                .createdAt(new Date())
                .build();
    }

    /**
     * 创建降级答案评估结果
     */
    private AnswerEvaluationDTO createFallbackAnswerEvaluation(String answerId, 
                                                               String questionId, 
                                                               String answerContent) {
        log.warn("使用降级策略处理答案评估, answerId: {}", answerId);
        return AnswerEvaluationDTO.builder()
                .id(UUID.randomUUID().toString())
                .answerId(answerId)
                .questionId(questionId)
                .answerContent(answerContent)
                .score(60)
                .grade("C")
                .evaluationDetails("【答案评估报告】\n评估服务暂时不可用，使用默认评分")
                .strengths(List.of("回答符合基本要求"))
                .suggestions(List.of("建议在评估服务恢复后重新评估"))
                .referenceAnswer("评估服务暂时不可用，无法生成参考答案")
                .createdAt(new Date())
                .build();
    }
}