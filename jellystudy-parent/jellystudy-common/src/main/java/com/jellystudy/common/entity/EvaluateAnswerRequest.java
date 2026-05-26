package com.jellystudy.common.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluateAnswerRequest implements Serializable {

    @NotBlank(message = "答案ID不能为空")
    private String answerId;

    @NotBlank(message = "问题ID不能为空")
    private String questionId;

    @NotBlank(message = "问题内容不能为空")
    private String questionContent;

    @NotBlank(message = "答案内容不能为空")
    private String answerContent;

    /** 回答作者（学号/用户名），用于 Coach MQ 积分归因 */
    private String userId;
}
