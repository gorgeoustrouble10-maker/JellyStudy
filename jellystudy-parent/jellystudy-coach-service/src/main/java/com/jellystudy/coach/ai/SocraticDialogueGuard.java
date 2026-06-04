package com.jellystudy.coach.ai;

import com.jellystudy.common.entity.SocraticMessageDTO;
import com.jellystudy.common.entity.SocraticReplyDTO;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 苏格拉底对话：答对检测、重复拦截、卡壳拆解、API 失败时的上下文兜底。
 */
public final class SocraticDialogueGuard {

    private static final Pattern BLOCKED_READY_CORRECT = Pattern.compile(
            "(不冲突|没有冲突|阻塞.*(资源|I/O|io|等待资源)|就绪.*(CPU|cpu|调度)|资源等待.*就绪|等待CPU)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern LAST_WAS_BLOCKED_READY_TRAP = Pattern.compile(
            "(阻塞.*调度|等待调度.*阻塞|就绪.*冲突|阻塞态.*就绪态)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern NEEDS_SCAFFOLD = Pattern.compile(
            "(不清楚|不太懂|不太明白|不会|不知道怎么|组织语言|怎么说|看不懂|零基础|没学过|能科普|解释一下|讲一讲|帮我理解|两个频道)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SCHEDULING_QUESTION = Pattern.compile(
            "(短作业|先来先服务|FCFS|SJF|平均等待|调度算法)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern BLOCKED_READY_QUESTION = Pattern.compile(
            "(阻塞态|就绪态|运行态|状态转换)",
            Pattern.CASE_INSENSITIVE);

    private SocraticDialogueGuard() {
    }

    public static boolean isPopularizeMode(String teachMode) {
        return teachMode != null && "popularize".equalsIgnoreCase(teachMode.trim());
    }

    public static boolean misconceptionResolved(String userMessage, List<SocraticMessageDTO> history) {
        if (userMessage == null || userMessage.isBlank()) {
            return false;
        }
        String lastAssistant = lastAssistantText(history);
        if (lastAssistant == null) {
            return false;
        }
        if (!LAST_WAS_BLOCKED_READY_TRAP.matcher(lastAssistant).find()) {
            return false;
        }
        return BLOCKED_READY_CORRECT.matcher(userMessage).find();
    }

    public static boolean needsScaffoldHelp(String userMessage) {
        return userMessage != null && NEEDS_SCAFFOLD.matcher(userMessage).find();
    }

    public static boolean isNearDuplicateReply(String reply, List<SocraticMessageDTO> history) {
        if (reply == null || reply.isBlank() || history == null) {
            return false;
        }
        String norm = normalize(reply);
        if (norm.length() < 12) {
            return false;
        }
        for (SocraticMessageDTO m : history) {
            if (!"assistant".equals(m.getRole()) || m.getContent() == null) {
                continue;
            }
            String prev = normalize(m.getContent());
            if (prev.length() < 12) {
                continue;
            }
            if (norm.equals(prev) || norm.contains(prev) || prev.contains(norm)) {
                return true;
            }
            if (longestCommonRatio(norm, prev) >= 0.72) {
                return true;
            }
        }
        return false;
    }

    /** 学生答对误区后进入下一问 */
    public static SocraticReplyDTO buildAdvanceReply(String topic, int turnCount, String userMessage,
                                                     List<SocraticMessageDTO> history) {
        SocraticReplyDTO dto = buildProgressiveReply(topic, history, turnCount, userMessage, true);
        dto.setStepAdvanced(true);
        return dto;
    }

    /** 卡壳 / 重复 / 降级时：紧扣上一问辅导，不跳频道 */
    public static SocraticReplyDTO buildProgressiveReply(String topic, List<SocraticMessageDTO> history,
                                                         int turnCount, String userMessage, boolean afterCorrect) {
        if (needsScaffoldHelp(userMessage) || (!afterCorrect && isWeakAttempt(userMessage))) {
            SocraticReplyDTO scaffold = buildScaffoldReply(topic, history, turnCount);
            scaffold.setScaffoldMode(true);
            return scaffold;
        }

        String last = lastAssistantText(history);
        if (last != null && SCHEDULING_QUESTION.matcher(last).find()) {
            return replyOf(topic, turnCount, false, false,
                    "你的直觉对了一半：短作业优先确实常让「平均等待」更短，但关键是比较同一批作业在 FCFS 与 SJF 下谁排队更久。"
                            + "请用「若三个作业到达时间相同、运行时间分别是 1/3/5，则…」这种句式试写一句对比。",
                    "调度对比", "到达时间", "比较 FCFS 与 SJF 平均等待");
        }
        if (last != null && BLOCKED_READY_QUESTION.matcher(last).find()) {
            return replyOf(topic, turnCount, afterCorrect, false,
                    afterCorrect
                            ? "阻塞与就绪你已分清。接着想：进程从运行态因 I/O 阻塞时，CPU 会立刻去运行另一个就绪进程吗？"
                            : "仍围绕状态：阻塞态在等什么？就绪态又在等什么？各用半句话回答即可。",
                    "状态", "等资源", "等 CPU");
        }

        if (topic != null && topic.contains("操作系统")) {
            String reply = switch (Math.min(turnCount, 4)) {
                case 1 -> "理解正确：阻塞态等待的是 I/O、锁等资源，就绪态才是等待 CPU 调度。"
                        + "那进程从运行态因读磁盘而阻塞时，CPU 会立刻调度别的就绪进程吗？为什么？";
                case 2 -> "很好。接着想：同一就绪队列里，短作业优先和先来先服务对平均等待时间会有什么不同？"
                        + "各举一个直觉例子。";
                case 3 -> "不错。如果两个进程互相持有对方需要的资源，会进入什么状态？"
                        + "这和单个进程的阻塞态有什么联系？";
                default -> "本轮核心点你已经理顺。可以点「结束并生成总结」沉淀成卡片，"
                        + "或换一个新话题继续追问。";
            };
            return replyOf(topic, turnCount, afterCorrect, false, reply,
                    "状态转换", "调度与资源", "结合运行/就绪/阻塞");
        }

        return replyOf(topic, turnCount, afterCorrect, false,
                "我们仍停留在「" + topic + "」。请用一句话说出你此刻最大的疑问，我先帮你拆开再追问。",
                "定义", "推导", "联系已学章节");
    }

    /** API 不可用时的上下文兜底（禁止固定跳回阻塞/就绪） */
    public static SocraticReplyDTO buildContextualFallback(String topic, List<SocraticMessageDTO> history,
                                                           int turnCount, String userMessage) {
        SocraticReplyDTO dto = buildProgressiveReply(topic, history, turnCount, userMessage, false);
        dto.setDegradedFallback(true);
        if (!dto.isScaffoldMode() && dto.getReply() != null && !dto.getReply().contains("上一问")) {
            String last = lastAssistantText(history);
            if (last != null && last.length() > 20) {
                String snippet = last.length() > 80 ? last.substring(0, 80) + "…" : last;
                dto.setReply("（千问暂不可用，本地辅导）我们仍围绕上一问：「" + snippet + "」。"
                        + "你可以先写一句不完整的理解，我帮你补全，不必追求标准答案。");
            }
        }
        return dto;
    }

    /** 纯科普模式：本地或 API 失败时的深度讲解 */
    public static SocraticReplyDTO buildPopularizeReply(String topic, List<SocraticMessageDTO> history,
                                                         String userMessage, int turnCount) {
        SocraticReplyDTO deep = buildScaffoldReply(topic, history, turnCount);
        String extra = userMessage != null && !userMessage.isBlank()
                ? " 你刚提到：「" + (userMessage.length() > 40 ? userMessage.substring(0, 40) + "…" : userMessage) + "」。"
                : " ";
        deep.setReply("【科普模式】" + deep.getReply() + extra
                + " 有不懂的词直接问我，理解后也可切回「追问模式」练一练。");
        deep.setHint("纯科普 · 软考入门");
        deep.setPopularizeMode(true);
        deep.setScaffoldMode(true);
        deep.setTurnCount(turnCount);
        return deep;
    }

    public static SocraticReplyDTO buildScaffoldReply(String topic, List<SocraticMessageDTO> history, int turnCount) {
        String last = lastAssistantText(history);
        if (last != null && SCHEDULING_QUESTION.matcher(last).find()) {
            return replyOf(topic, turnCount, false, true,
                    "没关系，先搞懂「平均等待时间」：从作业到达到被调度完成，排队越久等待越久。"
                            + "FCFS 按到达先后，短作业也可能排在长作业后面；SJF 优先选运行时间短的，平均等待往往更短。"
                            + "你可以填空：「三个作业同时到达、运行时间 1/3/5，FCFS 先跑___，SJF 先跑___，所以平均等待___更短。」",
                    "平均等待", "FCFS 先来先服务", "SJF 短作业优先");
        }
        if (last != null && BLOCKED_READY_QUESTION.matcher(last).find()) {
            return replyOf(topic, turnCount, false, true,
                    "先用生活类比：阻塞态 = 等外卖（等资源），就绪态 = 饭好了在桌上等你吃（等 CPU）。"
                            + "填空：「阻塞态等___，就绪态等___。」填完后再想：为何不能混为一谈？",
                    "阻塞", "就绪", "等资源 vs 等CPU");
        }
        if (topic != null && topic.contains("操作系统")) {
            return replyOf(topic, turnCount, false, true,
                    "零基础也没关系。操作系统可以先记三态：运行（占 CPU）、就绪（排队等 CPU）、阻塞（等 I/O/锁）。"
                            + "你先选一个最不懂的词，我用一句话解释它，再继续追问。",
                    "三态", "CPU", "I/O");
        }
        return replyOf(topic, turnCount, false, true,
                "我先帮你拆「" + topic + "」：请说出你最不懂的一个词，我用通俗话解释，再一起推到上一问。",
                "关键词", "通俗理解", "再追问");
    }

    private static boolean isWeakAttempt(String userMessage) {
        if (userMessage == null) {
            return false;
        }
        String t = userMessage.trim();
        return t.length() < 12 && !t.contains("?");
    }

    private static SocraticReplyDTO replyOf(String topic, int turnCount, boolean stepAdvanced, boolean scaffold,
                                            String reply, String h1, String h2, String h3) {
        return SocraticReplyDTO.builder()
                .topic(topic)
                .reply(reply)
                .hint(scaffold ? "入门拆解" : "递进追问")
                .turnCount(turnCount)
                .misconceptionDetected(false)
                .stepAdvanced(stepAdvanced)
                .scaffoldMode(scaffold)
                .hintLevel1(h1)
                .hintLevel2(h2)
                .hintLevel3(h3)
                .build();
    }

    static String lastAssistantText(List<SocraticMessageDTO> history) {
        if (history == null) {
            return null;
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            SocraticMessageDTO m = history.get(i);
            if ("assistant".equals(m.getRole()) && m.getContent() != null && !m.getContent().isBlank()) {
                return m.getContent();
            }
        }
        return null;
    }

    static String normalize(String text) {
        return text.replaceAll("\\s+", "")
                .replaceAll("[，。！？、；：\"'\\-—…]", "")
                .toLowerCase(Locale.ROOT);
    }

    static double longestCommonRatio(String a, String b) {
        int max = 0;
        int limit = Math.min(a.length(), b.length());
        for (int len = limit; len >= 8; len--) {
            for (int i = 0; i + len <= a.length(); i++) {
                String sub = a.substring(i, i + len);
                if (b.contains(sub)) {
                    max = Math.max(max, len);
                    break;
                }
            }
            if (max > 0) {
                break;
            }
        }
        return max / (double) Math.max(a.length(), b.length());
    }
}
