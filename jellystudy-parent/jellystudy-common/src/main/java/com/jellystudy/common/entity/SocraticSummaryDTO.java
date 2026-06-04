package com.jellystudy.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocraticSummaryDTO implements Serializable {

    private String topic;
    /** 核心要点卡片 */
    private List<String> keyPoints;
    /** 掌握较好的方面 */
    private List<String> masteredAspects;
    /** 理解误区 */
    private List<String> misconceptions;
    /** 思考逻辑链评价 */
    private String logicChainComment;
    /** 推荐巩固的知识点/练习 */
    private List<String> recommendedPractice;
    private String summaryMarkdown;
    private Integer evaluateScore;
    private String evaluateGrade;
    /** 总结是否因千问失败而降级 */
    private boolean summaryDegraded;
    private String aiSource;
}
