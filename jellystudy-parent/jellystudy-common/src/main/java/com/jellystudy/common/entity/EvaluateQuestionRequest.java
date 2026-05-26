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
public class EvaluateQuestionRequest implements Serializable {

    @NotBlank(message = "问题ID不能为空")
    private String questionId;

    @NotBlank(message = "问题标题不能为空")
    private String questionTitle;

    @NotBlank(message = "问题内容不能为空")
    private String questionContent;
}
