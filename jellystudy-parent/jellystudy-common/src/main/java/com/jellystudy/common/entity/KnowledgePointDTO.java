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
 * 知识点DTO - 用于Dubbo传输
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgePointDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private String id;
    
    @NotBlank(message = "知识点名称不能为空")
    @Size(max = 100, message = "知识点名称长度不能超过100个字符")
    private String name;
    
    @Size(max = 500, message = "知识点描述长度不能超过500个字符")
    private String description;
    
    private String parentId;
    private String path;
    private Date createdAt;
    private Date updatedAt;

    /** 关联该知识点的问题数量（只读统计） */
    private Integer questionCount;
}
