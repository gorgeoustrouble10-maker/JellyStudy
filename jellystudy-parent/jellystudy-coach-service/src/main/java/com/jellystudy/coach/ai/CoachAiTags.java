package com.jellystudy.coach.ai;

import com.jellystudy.common.api.AiSource;
import com.jellystudy.common.entity.SocraticReplyDTO;
import com.jellystudy.common.entity.SocraticSummaryDTO;

/**
 * 统一标记 AI 回复来源与是否降级，便于日志与前端展示。
 */
public final class CoachAiTags {

    private CoachAiTags() {
    }

    public static void apply(SocraticReplyDTO dto, AiSource source) {
        if (dto == null || source == null) {
            return;
        }
        dto.setAiSource(source.name());
        boolean degraded = source != AiSource.QWEN && source != AiSource.LOCAL_GUARD;
        if (source == AiSource.LOCAL_FALLBACK) {
            dto.setDegradedFallback(true);
        } else if (source == AiSource.QWEN) {
            dto.setDegradedFallback(false);
        } else if (degraded && !dto.isDegradedFallback()) {
            dto.setDegradedFallback(false);
        }
    }

    public static void markSummaryFallback(SocraticSummaryDTO dto) {
        if (dto != null) {
            dto.setSummaryDegraded(true);
            dto.setAiSource(AiSource.LOCAL_RULES.name());
        }
    }

    public static void markSummaryQwen(SocraticSummaryDTO dto) {
        if (dto != null) {
            dto.setSummaryDegraded(false);
            dto.setAiSource(AiSource.QWEN.name());
        }
    }
}
