package com.jellystudy.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API 网关：前端 / 外部客户端只访问 :8080，由 Gateway 转发到三个微服务。
 * <p>
 * 为什么需要 Gateway？
 * <ul>
 *   <li>统一入口：前端不再记 8081/8082/8083 三个端口</li>
 *   <li>路由集中：改后端端口只改 yml，不动前端</li>
 *   <li>后续可扩展：鉴权、限流、日志、CORS 一处配置</li>
 * </ul>
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
