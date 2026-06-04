package com.jellystudy.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "knowledge.list")
public class KnowledgeListProperties {

    /** 列表最大返回条数；0 表示不限制 */
    private int maxListSize = 0;
}
