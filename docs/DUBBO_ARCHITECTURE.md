# Dubbo 调用规范（统一约定）

## 原则

| 场景 | 方式 | 说明 |
|------|------|------|
| **本服务 HTTP 入口** | 注入本进程 `*ServiceImpl` | 不走 Dubbo 自调用，避免回环超时 |
| **跨微服务调用** | `@DubboReference` + Nacos | 如 QA → 知识点、QA → 评估 |
| **对外暴露能力** | `@DubboService` + Nacos 注册 | 供其他服务消费 |

## 注册中心

- 地址：`nacos://127.0.0.1:8848`
- 控制台：http://localhost:8848/nacos（`nacos` / `nacos`）
- **Dubbo 3 标准三元组**（Provider 均需配置）：
  - `dubbo.registry` — 实例注册
  - `dubbo.metadata-report` — 接口元数据
  - `dubbo.config-center` — Dubbo 动态配置
- **register-mode: instance** — 应用级服务发现（Dubbo 3 推荐）

## 服务划分

- `jellystudy-knowledge`（8081）：`IKnowledgePointService`
- `jellystudy-qa`（8082）：`IQuestionService`、`IAnswerService`
- `jellystudy-evaluate-service`（8083/8085）：`IEvaluateService`
- `jellystudy-coach-service`（8084）：`ICoachService`

## 示例

- `QuestionController` → `QuestionServiceImpl`（本地）
- `QuestionServiceImpl` → `@DubboReference IKnowledgePointService`（远程 8081）
- `EvaluateAsyncExecutor` → `@DubboReference IEvaluateService`（远程 8083）

这不是「混用」，而是 Spring Cloud 微服务常见分层：**REST 适配层本地直连，服务间 RPC 走 Dubbo**。
