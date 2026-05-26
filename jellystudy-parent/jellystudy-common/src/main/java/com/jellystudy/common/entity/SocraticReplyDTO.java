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
    private String hintLevel1;
    private String hintLevel2;
    private String hintLevel3;
}
