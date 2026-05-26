package com.jellystudy.common.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 回答DTO - 用于Dubbo传输
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private String id;
    
    @NotBlank(message = "问题ID不能为空")
    private String questionId;
    
    @NotBlank(message = "回答内容不能为空")
    @Size(max = 10000, message = "回答内容长度不能超过10000个字符")
    private String content;
    
    private String author;
    private Date createdAt;
    private Date updatedAt;
    private int likeCount;
    private List<CommentDTO> comments;
}
