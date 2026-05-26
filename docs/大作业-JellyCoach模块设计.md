# JellyCoach 智能成长教练模块 — 大作业设计

## 1. 模块定位

**JellyCoach** 是独立于用户管理、权限管理、知识点管理、QA 问答的第五大模块，端口 **8084**。

在「提问 → 评估」闭环之上，增加 **评估 → AI 诊断 → 巩固练习 → 积分激励 → 宠物成长** 链路。

## 2. 目标描述

利用用户历史评估数据与大模型能力，自动识别薄弱知识点，生成个性化巩固任务与周报；通过 RabbitMQ 解耦评估高峰、MongoDB 存储成长档案、Redis 缓存今日任务与排行榜；知识宠物提供游戏化激励，满足自主感、胜任感、归属感（SDT 理论）。

## 3. 功能描述

| 功能 | 说明 |
|------|------|
| F1 学情画像 | 汇总评估结果，AI 归纳薄弱点 Top3 |
| F2 每日任务 | Redis 缓存 Nacos 配置的每日目标数 |
| F3 AI 巩固出题 | 针对薄弱点调用 DashScope 生成练习题 |
| F4 AI 练习批改 | 提交答案后 AI 打分并奖励积分 |
| F5 学习周报 | 一键生成 Markdown 周报 |
| F6 知识宠物 | 积分喂养、升级、形态变化（MongoDB） |

## 4. 技术栈对照

| 技术 | 用途 |
|------|------|
| Spring Boot | `jellystudy-coach-service` |
| Dubbo | `ICoachService` 对外暴露 |
| MongoDB | 成长档案、AI 题目、宠物状态 |
| Redis | 今日任务、打卡 streak、排行榜 ZSET |
| RabbitMQ | `evaluation.completed` → Coach 异步消费 |
| AI 大模型 | 诊断 / 出题 / 批改 / 周报（DashScope） |
| Nacos | `coach.growth.daily-goal-count` 等动态配置 |
| SkyWalking | Agent 挂载，Trace 可见 MQ → AI → MongoDB |
| Docker | evaluate×2 + coach + mongo + rabbitmq |

## 5. 架构

```
评估完成(8083) --RabbitMQ--> Coach(8084)
                              ├─ AI 诊断 (DashScope)
                              ├─ MongoDB 档案
                              ├─ Redis 任务/排行
                              └─ 前端宠物页 (#coach)
```

## 6. 作业一（Docker + Nacos）

- **双实例**：`docker-compose.services.yml` 中 `evaluate-1`(8083) + `evaluate-2`(8085)
- **Nacos 配置**：`nacos-config/jellystudy-evaluate-service.yaml`、`jellystudy-coach-service.yaml`
- **验证**：`GET /api/evaluations/instance-info` 查看实例 ID 与 Nacos 模型名

## 7. 启动顺序

```powershell
docker compose -f docker-compose.core.yml up -d
# 导入 nacos-config/*.yaml 到 Nacos 控制台
mvn -f jellystudy-parent/pom.xml package -DskipTests
.\scripts\start-java-services.ps1
cd frontend && npx vite --host 127.0.0.1 --port 9945
```

Docker 全栈：

```powershell
mvn -f jellystudy-parent/pom.xml package -DskipTests
docker compose -f docker-compose.core.yml -f docker-compose.services.yml up -d --build
```
