package com.jellystudy.common.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 平台 Bearer 写操作鉴权（与 Coach 共用 Redis Token）。
 */
@Data
@ConfigurationProperties(prefix = "jellystudy.auth")
public class JellystudyAuthProperties {

    /**
     * 为 true 时，问答/知识点等写接口要求 Authorization: Bearer &lt;token&gt;。
     */
    private boolean bearerWriteEnabled = true;
}
