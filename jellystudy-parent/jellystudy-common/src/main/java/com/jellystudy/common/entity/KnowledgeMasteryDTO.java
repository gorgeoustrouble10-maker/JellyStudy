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
public class KnowledgeMasteryDTO implements Serializable {

    private String name;
    /** 0-100，来自 AI 练习 / 苏格拉底 / 问答评估聚合 */
    private int percent;
    private String status;
    /** quiz | socratic | qa | mixed | none */
    private String source;
}
