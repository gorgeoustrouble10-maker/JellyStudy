package com.jellystudy.common.api;

/**
 * Coach / 评估侧 AI 产出来源，供前端展示与运维观测。
 */
public enum AiSource {
    /** 阿里云 DashScope 千问 */
    QWEN,
    /** 对话守卫：答对递进、重复拦截等本地逻辑 */
    LOCAL_GUARD,
    /** 卡壳拆解 / 入门科普模板 */
    LOCAL_SCAFFOLD,
    /** 千问不可用或调用失败后的上下文兜底 */
    LOCAL_FALLBACK,
    /** 纯科普模式本地讲解 */
    LOCAL_POPULARIZE,
    /** 诊断/出题/周报等规则或模板降级 */
    LOCAL_RULES
}
