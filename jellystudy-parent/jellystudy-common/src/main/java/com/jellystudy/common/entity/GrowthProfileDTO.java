package com.jellystudy.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrowthProfileDTO implements Serializable {

    private String userId;
    private int totalPoints;
    private int streakDays;
    /** 今日是否已完成打卡（每日首次有效学习行为） */
    private boolean checkedInToday;
    /** 最近7天打卡记录，末位=今天 */
    private List<Boolean> recent7DayCheckIns;
    /** 最近30天打卡记录，末位=今天 */
    private List<Boolean> recent30DayCheckIns;
    private String streakNote;
    private List<String> weakPoints;
    private String lastDiagnosis;
    /** 来自知识点服务的已学范围 */
    private List<String> learnedKnowledgePoints;
    private List<KnowledgeMasteryDTO> knowledgeMastery;
    private String quizScopeSource;
    private Date updatedAt;
}
