package com.jellystudy.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocraticReplyDTO implements Serializable {
    private String topic;
    private String reply;
    private String hint;
    private int turnCount;
    /** 是否检测到理解误区（回复采用反向追问而非直接否定） */
    private boolean misconceptionDetected;
    /** 学生已纠正误区，本轮进入递进新问题 */
    private boolean stepAdvanced;
    /** 卡壳时的入门拆解（紧扣上一问，含通俗科普） */
    private boolean scaffoldMode;
    /** 千问 API 不可用，使用本地上下文兜底 */
    private boolean degradedFallback;
    /** 产出来源：QWEN / LOCAL_GUARD / LOCAL_SCAFFOLD / LOCAL_FALLBACK / LOCAL_POPULARIZE */
    private String aiSource;
    /** 本轮为纯科普模式生成 */
    private boolean popularizeMode;
    private String hintLevel1;
    private String hintLevel2;
    private String hintLevel3;
}
