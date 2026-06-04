# 平台认证与 JellyCoach 模块边界说明

> 答辩/实验报告用：说明「登录鉴权」与「JellyCoach 业务模块」在架构上的划分。

## 1. 为什么 Auth 代码在 Coach 进程里？

JellyStudy 采用**轻量微服务**策略：不设独立的「用户管理服务」进程，避免为课程演示引入过多部署单元。

| 能力 | 部署位置 | 说明 |
|------|----------|------|
| **平台认证（Auth）** | Coach 进程 `:8084` 的 `/api/auth/*` | 登录、Token 签发与校验 |
| **平台写权限（Permission）** | `jellystudy-common` 的 `JellystudyBearerWriteAuthFilter` | QA / Knowledge 写操作需 Bearer Token |
| **JellyCoach 业务** | Coach 进程 `:8084` 的 `/api/coach/*` | 学情、任务、宠物、周报、苏格拉底 |

Auth **不是** JellyCoach 的业务功能，而是**全平台共享的基础设施**，因 Coach 已持有 MongoDB 用户库与 Redis Token，故由同一进程托管 HTTP 入口，Gateway 将 `/api/auth/**` 路由至 8084。

## 2. 三层边界（答辩可直接引用）

```
┌─────────────────────────────────────────────────────────┐
│  Layer A — 平台基础设施（跨模块）                          │
│  · AuthController / AuthService / AuthTokenStore        │
│  · JellystudyBearerWriteAuthFilter（common）              │
│  · Redis jelly:auth:token:*                             │
└─────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────────────────────────────────────┐
│  Layer B — 既有业务模块（作业前期）                        │
│  · Knowledge :8081  · QA :8082  · Evaluate :8083        │
└─────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────────────────────────────────────┐
│  Layer C — 大作业独立模块 JellyCoach（Layer B 之上扩展）   │
│  · CoachServiceImpl / CoachController / CoachAiEngine   │
│  · MongoDB 成长档案 · MQ 消费 · 宠物/任务/周报            │
└─────────────────────────────────────────────────────────┘
```

- **Layer A** 为所有模块提供「谁在用系统」，不包含学情/宠物逻辑。
- **Layer C** 仅依赖 Layer A 的 `userId`，不修改 QA/Knowledge 的领域模型。

## 3. 代码包级划分

| 包/目录 | 归属 | 职责 |
|---------|------|------|
| `com.jellystudy.coach.auth.*` | **平台 Auth** | Token 存储、Coach API 拦截器 |
| `com.jellystudy.coach.controller.AuthController` | **平台 Auth** | `POST /api/auth/login` 等 |
| `com.jellystudy.coach.service.AuthService` | **平台 Auth** | 用户校验、Token 生命周期 |
| `com.jellystudy.common.auth.*` | **平台 Permission** | QA/Knowledge 写鉴权 Filter |
| `com.jellystudy.coach.controller.CoachController` | **JellyCoach 业务** | 成长教练 REST API |
| `com.jellystudy.coach.service.CoachServiceImpl` | **JellyCoach 业务** | 诊断、任务、Quiz、周报 |
| `com.jellystudy.coach.ai.CoachAiEngine` | **JellyCoach 业务** | DashScope 调用 |
| `com.jellystudy.coach.mq.*` | **JellyCoach 业务** | 评估完成事件消费 |

## 4. API 路由与鉴权差异

| 路径前缀 | 模块 | 读操作 | 写操作 |
|----------|------|--------|--------|
| `/api/auth/**` | 平台 Auth | 开放 | 登录/注册 |
| `/api/knowledge-points/**` | Knowledge | 开放 | 需 Bearer（common Filter） |
| `/api/questions/**`、`/api/answers/**` | QA | 开放 | 需 Bearer |
| `/api/coach/**` | JellyCoach | — | 全部需 Bearer（CoachAuthInterceptor） |
| `/api/evaluations/**` | Evaluate | 开放 | 视接口而定 |

JellyCoach 业务接口**统一强鉴权**；平台写权限对 QA/Knowledge **仅保护写操作**，读仍开放，符合「学习社区可浏览、创作需登录」的产品设定。

## 5. 数据存储边界

| 存储 | Auth 使用 | JellyCoach 业务使用 |
|------|-----------|---------------------|
| MongoDB `app_users` | ✅ 账号密码 | ❌ |
| MongoDB `growth_profiles` 等 | ❌ | ✅ 学情/宠物/Quiz |
| Redis `jelly:auth:token:*` | ✅ 全平台 Token | Coach 读 Token 验身份 |
| Redis `jelly:coach:*` | ❌ | ✅ 任务/打卡/排行榜 |

## 6. 与「独立模块」要求的对齐

老师要求 JellyCoach **独立于用户管理、权限管理、知识点、QA**：

| 维度 | 说明 |
|------|------|
| **Maven/部署** | `jellystudy-coach-service` 独立 JAR、独立端口 8084、可单独扩缩容 |
| **领域模型** | 成长档案/宠物/Quiz 等不在 MySQL QA 库中 |
| **业务闭环** | 评估 → MQ → 诊断 → 练习 → 积分，不依赖 QA 表结构 |
| **Auth 关系** | Auth 是**宿主进程内的平台子系统**，类比「Spring Security 内嵌于业务应用」，**不计入** JellyCoach 功能清单（实验报告 F1–F8 不含登录） |

若未来演进：可将 `coach.auth` + `AuthController` 拆为 `jellystudy-auth-service`，JellyCoach 仅保留 `/api/coach/**`，当前课程规模下**文档化边界即可**。

## 7. 实验报告建议写法

在「JellyCoach 模块设计」章节增加一小节 **「与平台认证的关系」**，引用本文档 Layer A/B/C 图；在功能表中将 F9 登录鉴权标注为「平台基础设施，由 Coach 进程托管，非 Coach 业务域功能」。
