package com.jellystudy.common.health;

/**
 * 可选组件探活（由各服务 classpath 上的自动配置注册）。
 */
public interface HealthProbe {

    String component();

    boolean isUp();
}
