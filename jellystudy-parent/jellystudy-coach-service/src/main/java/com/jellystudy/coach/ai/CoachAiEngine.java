package com.jellystudy.coach.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jellystudy.coach.config.CoachModelProperties;
import com.jellystudy.coach.document.AiQuiz;
import com.jellystudy.common.entity.SocraticMessageDTO;
import com.jellystudy.common.entity.SocraticReplyDTO;
import com.jellystudy.common.entity.SocraticSummaryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoachAiEngine {

    private final CoachModelProperties modelProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public DiagnosisResult diagnoseWeakPoints(List<String> recentEvaluations, List<String> allowedKnowledge) {
        String scope = scopeBlock(allowedKnowledge, "");
        String prompt = """
            你是 JellyCoach 学习教练。根据评估摘要，从【已学知识点白名单】中选出最需巩固的 3 项作为 weakPoints。
            禁止输出白名单以外的知识点名称。输出 JSON：
            {"weakPoints":["名称1","名称2","名称3"],"diagnosis":"200字以内学情诊断"}
            
            """ + scope + "\n评估摘要：\n" + String.join("\n", recentEvaluations);

        try {
            String response = callModel(prompt);
            JsonNode node = objectMapper.readTree(extractJson(response));
            List<String> weakPoints = parseStringArray(node.get("weakPoints"));
            weakPoints = filterToScope(weakPoints, allowedKnowledge);
            if (weakPoints.isEmpty()) {
                weakPoints = topFromScope(allowedKnowledge, 3);
            }
            String diagnosis = node.has("diagnosis") ? node.get("diagnosis").asText() : "暂无诊断";
            return new DiagnosisResult(weakPoints, diagnosis);
        } catch (Exception e) {
            log.warn("AI 学情诊断失败，使用知识点范围降级", e);
            return new DiagnosisResult(topFromScope(allowedKnowledge, 3),
                    "已根据您的知识点库生成练习建议，请完成 AI 巩固题。");
        }
    }

    public List<AiQuiz> generateQuizzes(String userId, String weakPoint, int count,
                                        List<String> allowedKnowledge, String scopeDetail) {
        String prompt = String.format("""
            你是 JellyCoach。针对已学知识点「%s」出 %d 道巩固练习题。
            【严格要求】题目必须只涉及以下已学范围，禁止出现 Spring Boot、微服务等未列出的技术：
            %s
            
            输出 JSON 数组：[{"question":"题目","hint":"提示"}]
            """, weakPoint, count, scopeBlock(allowedKnowledge, scopeDetail));

        try {
            String response = callModel(prompt);
            JsonNode array = objectMapper.readTree(extractJson(response));
            List<AiQuiz> quizzes = new ArrayList<>();
            if (array.isArray()) {
                for (JsonNode item : array) {
                    quizzes.add(AiQuiz.builder()
                            .id(UUID.randomUUID().toString())
                            .userId(userId)
                            .weakPoint(weakPoint)
                            .question(item.get("question").asText())
                            .hint(item.has("hint") ? item.get("hint").asText() : "")
                            .status("PENDING")
                            .createdAt(new Date())
                            .build());
                }
            }
            if (!quizzes.isEmpty()) {
                return quizzes;
            }
        } catch (Exception e) {
            log.warn("AI 出题失败，使用范围降级", e);
        }
        return List.of(AiQuiz.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .weakPoint(weakPoint)
                .question("请结合你的笔记，简述「" + weakPoint + "」的核心要点。")
                .hint("仅限已学知识点：" + String.join("、", allowedKnowledge))
                .status("PENDING")
                .createdAt(new Date())
                .build());
    }

    public QuizGradeResult gradeQuiz(String question, String userAnswer, String weakPoint) {
        String trimmed = userAnswer != null ? userAnswer.trim() : "";
        if (trimmed.isEmpty()) {
            return new QuizGradeResult(0, "未作答。请根据题目写出你的理解后再提交。");
        }
        if (trimmed.length() < 2) {
            return new QuizGradeResult(8, "答案过短，无法判断掌握程度，请补充说明。");
        }
        if (isObviousNonsense(trimmed)) {
            return new QuizGradeResult(15, "答案与题目无关或缺乏有效内容，请结合知识点重新作答。");
        }

        String prompt = buildGradePrompt(question, trimmed, weakPoint);

        try {
            String response = callModel(prompt, 0.15);
            JsonNode node = objectMapper.readTree(extractJson(response));
            int score = node.has("score") ? node.get("score").asInt(-1) : -1;
            if (score < 0 || score > 100) {
                score = heuristicScore(trimmed, question);
            }
            String feedback = node.has("feedback") ? node.get("feedback").asText() : "已提交";
            String grade = node.has("grade") ? node.get("grade").asText() : toGrade(score);
            score = Math.min(100, Math.max(0, score));
            score = reconcileScore(score, feedback, trimmed, question);
            log.info("千问批改 weakPoint={} score={} grade={} answerLen={}", weakPoint, score, toGrade(score), trimmed.length());
            return new QuizGradeResult(score, formatFeedback(score, toGrade(score), feedback));
        } catch (Exception e) {
            log.warn("AI 批改失败，使用规则降级", e);
            int fallback = heuristicScore(trimmed, question);
            return new QuizGradeResult(fallback, "AI 批改暂不可用，已按答案长度与相关性给出参考分 "
                    + fallback + " 分，请稍后重试获取详细评语。");
        }
    }

    private String buildGradePrompt(String question, String userAnswer, String weakPoint) {
        return """
            你是严谨的阅卷老师，使用阿里云千问对练习题打分。必须根据学生答案的实际质量给出 0-100 的整数分，禁止默认给 85 分或固定分数。

            【评分梯度】
            - 95-100：核心完全正确且切题；选择题/简答题只答关键词也算满分
            - 85-94：基本正确，有少量表述不严谨或次要遗漏
            - 70-84：部分正确，缺少关键要点
            - 50-69：沾边但明显不完整或有明显错误
            - 20-49：大部分错误，仅个别词相关
            - 1-19：几乎无关或胡编
            - 0：空白或完全无关

            【重要规则】
            1. 若答案核心正确，不得因「未展开细节」而低于 90 分
            2. 若答案明显错误，不得高于 40 分
            3. 题目若只问「哪种/哪个/什么状态」，学生答对核心关键词即应 95-100 分，不得因未重复题干而扣分
            4. score 必须是整数，不同质量的答案分数必须有明显差异

            知识点：%s
            题目：%s
            学生答案：%s

            仅输出 JSON，score 填你判定的整数（不要用示例分数）：
            {"score":0,"grade":"A|B|C|D","feedback":"50字以内评语"}
            """.formatted(weakPoint, question, userAnswer);
    }

    private boolean isObviousNonsense(String answer) {
        String lower = answer.toLowerCase().replaceAll("\\s+", "");
        return lower.matches("^(不知道|不会|随便|随便填|aaa+|111+|。。。+|test)+$");
    }

    private int heuristicScore(String answer, String question) {
        int len = answer.length();
        if (len < 5) {
            return 25;
        }
        if (len < 15) {
            return 55;
        }
        if (len < 40) {
            return 72;
        }
        return 80;
    }

    private int reconcileScore(int aiScore, String feedback, String answer, String question) {
        if (feedback != null) {
            boolean wrong = feedback.contains("错误") || feedback.contains("不正确") || feedback.contains("不对")
                    || feedback.contains("未能") || feedback.contains("未正确");
            boolean correct = !wrong && (feedback.contains("正确") || feedback.contains("准确") || feedback.contains("完全符合"));
            if (correct && aiScore < 90) {
                return Math.max(aiScore, 95);
            }
        }
        boolean asksWhich = question.contains("哪种") || question.contains("哪个") || question.contains("什么状态")
                || question.contains("哪一种");
        if (asksWhich && answer.length() <= 12 && aiScore >= 65 && aiScore < 90) {
            String fb = feedback != null ? feedback : "";
            if (!fb.contains("错误") && !fb.contains("不正确")) {
                return 98;
            }
        }
        return aiScore;
    }

    private String toGrade(int score) {
        if (score >= 90) {
            return "A";
        }
        if (score >= 80) {
            return "B";
        }
        if (score >= 60) {
            return "C";
        }
        return "D";
    }

    private String formatFeedback(int score, String grade, String feedback) {
        return String.format("[%s · %d分] %s", grade, score, feedback);
    }

    public String generateWeeklyReport(String userId, int totalPoints, int streakDays,
                                       List<String> weakPoints, List<String> learnedKnowledge) {
        String prompt = String.format("""
            为学习者 %s 写 200 字以内中文学习周报。
            总积分：%d，连续打卡：%d 天
            已学知识点：%s
            建议巩固：%s
            不要提及用户未学过的技术。
            直接输出 Markdown 正文（可用 ## 小标题和 - 列表），不要用 ``` 代码块包裹。
            """, userId, totalPoints, streakDays,
                String.join("、", learnedKnowledge), String.join("、", weakPoints));

        try {
            return callModel(prompt);
        } catch (Exception e) {
            log.warn("AI 周报失败，使用降级", e);
            return "## 学习周报\n已学：" + String.join("、", learnedKnowledge)
                    + "\n建议巩固：" + String.join("、", weakPoints);
        }
    }

    public SocraticReplyDTO socraticGuide(String topic, String userMessage,
                                         List<SocraticMessageDTO> history,
                                         List<String> allowedKnowledge, String scopeDetail) {
        int priorTurns = history != null ? (int) history.stream()
                .filter(m -> "user".equals(m.getRole())).count() : 0;
        int turnCount = priorTurns + 1;

        String conversation = formatConversation(history, userMessage);
        String prompt = String.format("""
            你是软考学习场景的苏格拉底式助教，正在进行【多轮连续对话】。
            话题：%s | 第 %d 轮

            【对话历史】
            %s

            已学知识点白名单（不得超范围）：
            %s

            【核心规则】
            1. 默认不直接给完整答案，用追问引导
            2. 若学生回答有明显错误，禁止说「你错了」；改用反向追问让其自己发现矛盾（如：「如果 X 成立，那为什么还需要 Y？」）
            3. 同时预生成三级提示（供用户卡壳时逐级查看）：
               - hintLevel1：仅关键词（5字内）
               - hintLevel2：更具体的思考方向（20字内）
               - hintLevel3：核心要点总结（40字内，仍不是完整标准答案）
            4. 承接上文，禁止重复已问过的问题

            仅输出 JSON：
            {"reply":"本轮引导或反向追问","misconceptionDetected":false,"hint":"思考方向","hintLevel1":"关键词","hintLevel2":"具体引导","hintLevel3":"要点总结"}
            """, topic, turnCount, conversation, scopeBlock(allowedKnowledge, scopeDetail));

        try {
            String response = callModel(prompt, 0.35);
            JsonNode node = objectMapper.readTree(extractJson(response));
            return SocraticReplyDTO.builder()
                    .topic(topic)
                    .reply(node.path("reply").asText("你能再深入一步吗？"))
                    .hint(node.path("hint").asText("回顾已学知识点"))
                    .turnCount(turnCount)
                    .misconceptionDetected(node.path("misconceptionDetected").asBoolean(false))
                    .hintLevel1(node.path("hintLevel1").asText(""))
                    .hintLevel2(node.path("hintLevel2").asText(""))
                    .hintLevel3(node.path("hintLevel3").asText(""))
                    .build();
        } catch (Exception e) {
            log.warn("苏格拉底对话失败", e);
            return SocraticReplyDTO.builder()
                    .topic(topic)
                    .reply(buildFallbackReply(userMessage, turnCount))
                    .hint("参考：" + String.join("、", allowedKnowledge))
                    .turnCount(turnCount)
                    .hintLevel1("核心概念")
                    .hintLevel2("联系已学章节思考")
                    .hintLevel3("回顾课件中的定义")
                    .build();
        }
    }

    public SocraticSummaryDTO socraticSummarize(String topic, List<SocraticMessageDTO> history,
                                                List<String> allowedKnowledge) {
        String conversation = formatHistoryOnly(history);
        String prompt = String.format("""
            分析以下苏格拉底式对话，输出学情评估（结合智能评估思路，软考场景）。
            话题：%s
            已学范围：%s

            【完整对话】
            %s

            输出 JSON：
            {
              "keyPoints":["要点1","要点2","要点3"],
              "masteredAspects":["掌握好的1","掌握好的2"],
              "misconceptions":["误区1"],
              "logicChainComment":"思考逻辑链是否完整的评语",
              "recommendedPractice":["建议练习的知识点或方向"],
              "summaryMarkdown":"## 知识点总结卡片\\n- 要点..."
            }
            """, topic, String.join("、", allowedKnowledge), conversation);

        try {
            String response = callModel(prompt, 0.3);
            JsonNode node = objectMapper.readTree(extractJson(response));
            return SocraticSummaryDTO.builder()
                    .topic(topic)
                    .keyPoints(parseStringList(node.get("keyPoints")))
                    .masteredAspects(parseStringList(node.get("masteredAspects")))
                    .misconceptions(parseStringList(node.get("misconceptions")))
                    .logicChainComment(node.path("logicChainComment").asText(""))
                    .recommendedPractice(parseStringList(node.get("recommendedPractice")))
                    .summaryMarkdown(node.path("summaryMarkdown").asText(""))
                    .build();
        } catch (Exception e) {
            log.warn("苏格拉底总结失败", e);
            return SocraticSummaryDTO.builder()
                    .topic(topic)
                    .keyPoints(List.of("请回顾本次讨论的核心概念"))
                    .masteredAspects(List.of("积极参与追问"))
                    .misconceptions(List.of())
                    .logicChainComment("AI 总结暂不可用")
                    .recommendedPractice(topFromScope(allowedKnowledge, 2))
                    .summaryMarkdown("## " + topic + " 讨论回顾\n请稍后重试生成详细总结。")
                    .build();
        }
    }

    private List<String> parseStringList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(n -> list.add(n.asText()));
        }
        return list;
    }

    private String formatHistoryOnly(List<SocraticMessageDTO> history) {
        StringBuilder sb = new StringBuilder();
        if (history != null) {
            for (SocraticMessageDTO m : history) {
                if (m.getContent() == null || m.getContent().isBlank()) {
                    continue;
                }
                sb.append("user".equals(m.getRole()) ? "学生：" : "助教：")
                        .append(m.getContent().trim()).append("\n");
            }
        }
        return sb.isEmpty() ? "（暂无对话）" : sb.toString();
    }

    private String formatConversation(List<SocraticMessageDTO> history, String currentMessage) {
        StringBuilder sb = new StringBuilder();
        if (history != null) {
            for (SocraticMessageDTO m : history) {
                if (m.getContent() == null || m.getContent().isBlank()) {
                    continue;
                }
                sb.append("user".equals(m.getRole()) ? "学生：" : "助教：")
                        .append(m.getContent().trim()).append("\n");
            }
        }
        sb.append("学生：").append(currentMessage.trim()).append("\n");
        return sb.toString();
    }

    private String buildFallbackReply(String userMessage, int turn) {
        if (turn <= 1) {
            return "关于「" + userMessage + "」，你目前的理解是什么？试着用自己的话说说看。";
        }
        return "很好，我们在第 " + turn + " 轮讨论中了。你能把刚才的想法和「阻塞态/就绪态」联系起来吗？";
    }

    private String scopeBlock(List<String> allowedKnowledge, String detail) {
        String names = allowedKnowledge.isEmpty() ? "（暂无）" : String.join("、", allowedKnowledge);
        return "【已学知识点白名单】" + names + (detail != null && !detail.isBlank() ? "\n" + detail : "");
    }

    private List<String> filterToScope(List<String> points, List<String> scope) {
        if (scope.isEmpty()) {
            return points;
        }
        return points.stream()
                .filter(p -> scope.stream().anyMatch(s -> s.equals(p) || s.contains(p) || p.contains(s)))
                .collect(Collectors.toList());
    }

    private List<String> topFromScope(List<String> scope, int n) {
        if (scope.isEmpty()) {
            return List.of("请先添加知识点");
        }
        return scope.subList(0, Math.min(n, scope.size()));
    }

    private List<String> parseStringArray(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(p -> list.add(p.asText()));
        }
        return list;
    }

    private String callModel(String prompt) throws Exception {
        return callModel(prompt, 0.4);
    }

    private String callModel(String prompt, double temperature) throws Exception {
        String apiKey = modelProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("未配置 DASHSCOPE_API_KEY");
        }

        String jsonBody = String.format("""
            {
              "model": "%s",
              "input": {"messages":[{"role":"user","content":"%s"}]},
              "parameters": {"temperature":%.2f,"max_tokens":2048}
            }
            """, modelProperties.getModelName(), escapeJson(prompt), temperature);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(java.time.Duration.ofMillis(modelProperties.getTimeout()))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("DashScope HTTP " + response.statusCode());
        }
        JsonNode root = objectMapper.readTree(response.body());
        return root.path("output").path("text").asText();
    }

    private String extractJson(String text) {
        int start = text.indexOf('{');
        int arrStart = text.indexOf('[');
        if (arrStart >= 0 && (start < 0 || arrStart < start)) {
            int end = text.lastIndexOf(']');
            return end > arrStart ? text.substring(arrStart, end + 1) : text;
        }
        if (start >= 0) {
            int end = text.lastIndexOf('}');
            return end > start ? text.substring(start, end + 1) : text;
        }
        return text;
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    public record DiagnosisResult(List<String> weakPoints, String diagnosis) {}
    public record QuizGradeResult(int score, String feedback) {}
}
