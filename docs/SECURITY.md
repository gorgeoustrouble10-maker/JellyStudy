# 安全配置说明

## 默认行为（课程演示）

- **Bearer 写鉴权开启**：`JELLYSTUDY_BEARER_WRITE_ENABLED=true`（默认）
  - 问答 / 知识点 **读** 无需登录；**写**（增删改、点赞、评论）需 `Authorization: Bearer <token>`
  - Token 由 Coach `/api/auth/login` 签发，存 Redis `jelly:auth:token:*`
  - 回答/问题的 `author` 由服务端从 Token 写入，前端不可伪造
- **Coach 全量鉴权**：除 `/api/auth/*` 与公开 config/leaderboard 外均需 Bearer
- **Evaluate**：不校验登录（评估引擎内部调用）
- **API Key 认证关闭**：`JELLYSTUDY_SECURITY_ENABLED=false`（默认）
- **Swagger 开启**：`SWAGGER_ENABLED=true`
- **CORS** 仅允许 `http://127.0.0.1:9945` 与 `http://localhost:9945`
- **Redis 密码**：Docker 默认 `jellystudy_redis`（见 `.env.example`）
- **Nacos 认证**：已开启，控制台 `nacos` / `nacos`
- **评估引擎**：默认尝试千问；`local-secrets.bat` 中无有效 `sk-` 密钥时自动 Mock
- **SkyWalking**：已并入 `docker-compose.core.yml`，UI http://localhost:8090

## 生产 / 公网部署

1. 启动时加 Spring profile：`spring.profiles.active=prod`
2. 设置环境变量：
   - `JELLYSTUDY_BEARER_WRITE_ENABLED=true`（建议保持开启）
   - `JELLYSTUDY_SECURITY_ENABLED=true`
   - `JELLYSTUDY_API_KEY=<强随机字符串>`
   - `MYSQL_PASSWORD`、`REDIS_PASSWORD` 使用强密码
3. 前端构建前设置 `VITE_JELLYSTUDY_API_KEY`（与后端一致）
4. `prod` profile 会：关闭 Swagger、`ddl-auto=validate`、要求 API Key

## 未实现（有意范围外）

- 邮箱验证、找回密码、OAuth 第三方登录
- RBAC 角色权限、Gateway 统一 JWT 校验
- 写操作作者与资源所有权校验（任意登录用户可删任意问题，课程 demo 可接受）

## 依赖安全

- Spring Boot 已升级至 **3.2.11**（修复 CVE-2024-38816 / CVE-2024-38819 等）
- 前端开发依赖：`package.json` 中 `overrides.esbuild` 缓解 dev-server 漏洞

## Redis 升级说明

若此前 Redis 无密码，更新后请：

```bat
docker compose -f docker-compose.core.yml down
docker volume rm 32308117_吕宇轩2_jellystudy-redis-data
docker compose -f docker-compose.core.yml up -d
```

（卷名以 `docker volume ls` 为准）

## 密钥泄露

若 `DASHSCOPE_API_KEY` 曾泄露，请在阿里云控制台**立即轮换**。
