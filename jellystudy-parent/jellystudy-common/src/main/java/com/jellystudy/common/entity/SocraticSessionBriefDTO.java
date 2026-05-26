package com.jellystudy.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocraticSessionBriefDTO implements Serializable {
    private String id;
    private String topic;
    private List<String> keyPoints;
    private List<String> misconceptions;
    private Integer evaluateScore;
    private String evaluateGrade;
    private String createdAt;
}
