package com.jellystudy.common.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 问题DTO - 用于Dubbo传输
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private String id;
    
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;
    
    @NotBlank(message = "内容不能为空")
    @Size(max = 5000, message = "内容长度不能超过5000个字符")
    private String content;
    
    private String knowledgePointId;
    private String author;
    private Date createdAt;
    private Date updatedAt;
    private int viewCount;
    private int likeCount;
    private int answerCount;
}
