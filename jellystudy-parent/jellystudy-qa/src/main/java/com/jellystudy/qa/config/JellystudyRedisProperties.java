package com.jellystudy.qa.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "jellystudy.redis")
public class JellystudyRedisProperties {

    private String hotKey = "jelly:hot:questions";
    private String viewRankKey = "jelly:view:rank";
    private String questionCachePrefix = "jelly:question:";
    private int questionCacheTtlMinutes = 30;
    /** 「最近」时间窗（天），超出窗口的问题不参与排行榜 */
    private int recentWindowDays = 7;
}
