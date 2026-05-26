# JellyStudy · 微服务智能学习平台

学号 **32308117** · 吕宇轩

**在线仓库**：https://github.com/gorgeoustrouble10-maker/JellyStudy

基于 **Spring Boot 微服务 + Dubbo + Redis + MongoDB + RabbitMQ + 通义千问** 的全栈学习平台，含 **JellyCoach 智能成长教练**（AI 学情、宠物、苏格拉底、周报可视化）。

---

## 架构概览

```text
浏览器 http://127.0.0.1:9945  (Vue 3 + Vite)
        │  Bearer Token（写操作 + Coach 鉴权）
        ▼
Gateway :8080  (Spring Cloud Gateway)
   ├─► 8081  jellystudy-knowledge     MySQL · 知识点
   ├─► 8082  jellystudy-qa             MySQL + Redis · 问答/热榜
   ├─► 8083  jellystudy-evaluate       MySQL + Redis + MQ · 千问评估
   └─► 8084  jellystudy-coach          MongoDB + Redis + MQ · JellyCoach + 登录

8082 ──Dubbo tri──► 8083  异步评估
8084 ──Dubbo tri──► 8081/8083  知识点白名单 / 苏格拉底评分
8083 ──RabbitMQ──► 8084  评估完成 → Coach 积分（按回答 author 归因）

基础设施 (docker-compose.core.yml):
  MySQL :3307 | Redis :6379 | Nacos :8848 | MongoDB :27017 | RabbitMQ :5672 | SkyWalking :8090
```

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 17 · Spring Boot 3.2.11 |
| 微服务 | Apache Dubbo 3.2.7 triple · Nacos 2.2.3 |
| 网关 | Spring Cloud Gateway :8080 |
| 缓存 | Redis 7（热榜 / 评估缓存 / 打卡 / 排行榜 / 登录 Token） |
| 数据库 | MySQL 8（`jellystudy` + `jellystudy_evaluate`）· MongoDB（Coach） |
| 消息 | RabbitMQ（评估完成 → Coach） |
| 大模型 | 阿里云 DashScope 千问（Mock 降级） |
| 前端 | Vue 3 · Vite 6 · ECharts · Tailwind |
| 测试 | JUnit 5 · Testcontainers |
| CI | GitHub Actions（Maven + 前端 build + Redis/Mongo 集成测） |

---

## 项目结构

```text
32308117_吕宇轩2/
├── jellystudy-parent/
│   ├── jellystudy-common/            # Dubbo 接口、DTO
│   ├── jellystudy-knowledge/         # :8081
│   ├── jellystudy-qa/                # :8082
│   ├── jellystudy-evaluate-service/  # :8083
│   ├── jellystudy-coach-service/     # :8084 JellyCoach + Auth
│   └── jellystudy-gateway/         # :8080
├── frontend/                         # Vue :9945
├── docker-compose.core.yml
├── scripts/start-java-services.ps1   # ★ 推荐一键启后端
├── init-database.sql
└── docs/                             # 审计报告、GitHub 指南、安全说明
```

---

## 一键启动（推荐）

### 1. 基础设施

```bat
docker compose -f docker-compose.core.yml up -d
```

### 2. 编译

```bat
cd jellystudy-parent
mvn clean package -DskipTests
```

### 3. 启动 5 个 Java 服务

```powershell
powershell -File scripts\start-java-services.ps1
```

| 服务 | 端口 | 说明 |
|------|------|------|
| knowledge | 8081 | 知识点 |
| evaluate | 8083 | 大模型评估 |
| **coach** | **8084** | JellyCoach + `/api/auth` |
| qa | 8082 | 问答 + Redis 榜 |
| **gateway** | **8080** | 统一入口 |

> 启动后等待 **30～60 秒** 各服务就绪。

### 4. 前端

```bat
cd frontend
npm install
npx vite --host 127.0.0.1 --port 9945
```

打开 **http://127.0.0.1:9945/** → 可匿名浏览；**提问/回答/编辑**或进入 **成长教练** 时需登录（`32308117` / `123456` 或 `demo` / `demo123`）。

---

## 上 GitHub

1. 确认 `.gitignore` 已排除 `local-secrets.bat`、`.env`、`tools/`、打包 zip
2. 仓库根目录含 `LICENSE`（MIT）、`.env.example`、`README.md`
3. 勿提交真实 `DASHSCOPE_API_KEY`；CI 使用 Mock 评估
4. 详见 [docs/GITHUB上传指南.md](docs/GITHUB上传指南.md)

---

## 核心能力

### Redis 三场景（课程实验）

| 场景 | Key | API |
|------|-----|-----|
| 热门榜 | `jelly:hot:questions` ZSET | `GET /api/questions/hot` |
| 常看榜 | `jelly:view:rank` | `GET /api/questions/recommended` |
| 评估缓存 | `jelly:eval:*` | `GET /api/evaluations/...` |

### JellyCoach 与平台鉴权

| 范围 | 行为 |
|------|------|
| **读接口** | 知识点 / 问题 / 回答 / 评估 **无需登录** |
| **写接口** | `POST/PUT/DELETE` 问答与知识点 **需 Bearer Token**；`author` 由服务端从 Token 写入，不可伪造 |
| **Coach** | 全部业务 API 需登录（MongoDB 用户 + Redis Token） |
| **Evaluate** | 不校验登录（内部评估引擎） |

- **学情闭环**：Dubbo 同步知识点 → AI 出题/批改 → 自然日打卡 → 积分排行榜
- **MQ 归因**：回答 `author` 来自服务端 Token → 评估完成 MQ → Coach 给**对应用户**加积分
- **掌握度**：聚合 AI 练习分数 + 苏格拉底 Dubbo 评分（非写死百分比）
- **苏格拉底**：多轮对话 + MongoDB 存总结 + 周报引用
- **宠物**：等级进化 + 主题皮肤 + 并排对比

---

## Gateway 路由

| 路径 | 目标 |
|------|------|
| `/api/knowledge-points/**` | 8081 |
| `/api/questions/**`, `/api/answers/**` | 8082 |
| `/api/evaluations/**` | 8083 |
| `/api/coach/**`, `/api/auth/**` | 8084 |
| `/api/health/{service}` | 各服务 `/api/health` |

---

## 测试

```bat
cd jellystudy-parent
mvn test -pl jellystudy-qa -Dtest=QuestionRankScoringTest,QuestionRedisServiceTestcontainersTest
mvn test -pl jellystudy-coach-service -Dtest=KnowledgeMasteryBuilderTest,AuthTokenStoreTestcontainersTest
```

需 **Docker Desktop**（Testcontainers）。

---

## 配置

复制 `local-secrets.bat.example` → `local-secrets.bat`：

```bat
set DASHSCOPE_API_KEY=sk-你的密钥
set EVALUATE_MODEL_TYPE=qianwen
```

无 Key 时自动 Mock，不影响演示。

---

## 文档索引

| 文档 | 用途 |
|------|------|
| [RUN_GUIDE.md](RUN_GUIDE.md) | 排错手册 |
| [docs/GITHUB上传指南.md](docs/GITHUB上传指南.md) | 上 GitHub |
| [docs/项目深度审计与启动报告.md](docs/项目深度审计与启动报告.md) | 深度审计 |
| [docs/SECURITY.md](docs/SECURITY.md) | 生产安全 |
| [实验报告-Redis与微服务.md](实验报告-Redis与微服务.md) | 实验报告 |

---

## 许可证

[MIT License](LICENSE) — 课程/portfolio 开源展示用；生产部署请阅读 [docs/SECURITY.md](docs/SECURITY.md)。
