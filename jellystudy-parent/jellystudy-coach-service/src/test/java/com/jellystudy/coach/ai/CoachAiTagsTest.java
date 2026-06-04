package com.jellystudy.coach.ai;

import com.jellystudy.common.api.AiSource;
import com.jellystudy.common.entity.SocraticReplyDTO;
import com.jellystudy.common.entity.SocraticSummaryDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoachAiTagsTest {

    @Test
    void qwenClearsDegradedFlag() {
        SocraticReplyDTO dto = SocraticReplyDTO.builder().degradedFallback(true).build();
        CoachAiTags.apply(dto, AiSource.QWEN);
        assertEquals("QWEN", dto.getAiSource());
        assertFalse(dto.isDegradedFallback());
    }

    @Test
    void localFallbackMarksDegraded() {
        SocraticReplyDTO dto = new SocraticReplyDTO();
        CoachAiTags.apply(dto, AiSource.LOCAL_FALLBACK);
        assertEquals("LOCAL_FALLBACK", dto.getAiSource());
        assertTrue(dto.isDegradedFallback());
    }

    @Test
    void summaryFallbackFlag() {
        SocraticSummaryDTO dto = new SocraticSummaryDTO();
        CoachAiTags.markSummaryFallback(dto);
        assertTrue(dto.isSummaryDegraded());
        assertEquals("LOCAL_RULES", dto.getAiSource());
    }
}
