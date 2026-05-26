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
 * 评论DTO - 用于Dubbo传输
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private String id;
    
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容长度不能超过1000个字符")
    private String content;
    
    private String author;
    private Date createdAt;
}
