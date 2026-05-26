# 实验报告 — Docker 双实例、Nacos 配置与 JellyCoach 模块

> 学号：32308117　姓名：吕宇轩　工程名：jellystudy  
> 导出 Word：`32308117_吕宇轩.docx`（将本 Markdown 粘贴至 Word 并插入截图）

---

## 1. 实验目的

1. 掌握 Docker 打包与 **一个服务两个实例** 部署（评估服务 evaluate-1 / evaluate-2）。
2. 掌握 **Nacos 配置中心** 动态参数读取与刷新。
3. 设计并实现 **JellyCoach 智能成长教练** 独立模块，集成 Spring Boot、Dubbo、MongoDB、Redis、RabbitMQ、AI 大模型、Nacos、调用链、Docker。

## 2. 实验环境

| 组件 | 版本/端口 |
|------|-----------|
| JDK | 17 |
| Spring Boot | 3.2.11 |
| Dubbo | 3.2.7 |
| Nacos | 8848 |
| MongoDB | 27017 |
| RabbitMQ | 5672 / 管理 15672 |
| Redis | 6379 |
| Evaluate | 8083、8085（双实例） |
| Coach | 8084 |
| Gateway | 8080 |
| 前端 | 9945 |

## 3. 作业一：Docker 双实例 + Nacos

### 3.1 Docker 打包

评估服务 Dockerfile：`jellystudy-parent/jellystudy-evaluate-service/Dockerfile`

双实例编排：`docker-compose.services.yml` 中 `evaluate-1`、`evaluate-2`。

启动：`start-docker-services.bat` 或

```powershell
docker compose -f docker-compose.core.yml -f docker-compose.services.yml up -d --build
```

### 3.2 Nacos 配置

在 Nacos 控制台创建配置（或导入 `nacos-config/`）：

- Data ID：`jellystudy-evaluate-service.yaml`
- Data ID：`jellystudy-coach-service.yaml`

关键参数示例：`evaluate.model.model-name`、`coach.growth.daily-goal-count`

验证接口：

- `GET http://localhost:8083/api/evaluations/instance-info`
- `GET http://localhost:8085/api/evaluations/instance-info`
- `GET http://localhost:8084/api/coach/config`

**【截图位】** Nacos 配置页、两个 instance-info 返回不同 instanceId

## 4. 作业二：JellyCoach 模块设计

详见 `docs/大作业-JellyCoach模块设计.md`

### 4.1 目标描述

（见设计文档 §2）

### 4.2 功能描述

（见设计文档 §3 — 学情画像、每日任务、AI 出题/批改、周报、知识宠物）

### 4.3 架构图

```
用户评估答案 → Evaluate(8083) → RabbitMQ → Coach(8084)
                                              → AI DashScope
                                              → MongoDB / Redis
前端 #coach → Gateway(8080) → Coach API → 宠物 & 任务 UI
```

**【截图位】** 架构图、SkyWalking Trace（MQ → Coach → HTTP DashScope）

## 5. 运行与演示

### 5.1 本地全栈

```powershell
docker compose -f docker-compose.core.yml up -d
.\scripts\start-java-services.ps1
cd frontend; npx vite --host 127.0.0.1 --port 9945
```

### 5.2 演示流程

1. 在「智能评估」页完成一次答案评估
2. 打开「成长教练」页 — 查看 AI 诊断、薄弱点、宠物
3. 点击薄弱点「练习」— AI 出题 → 提交 → 获得积分
4. 喂养宠物、生成周报

**【截图位】** 成长教练页、宠物升级、AI 批改结果、Redis/Mongo 数据

## 6. 技术对照表

| 要求 | 实现位置 |
|------|----------|
| Spring Boot | jellystudy-coach-service |
| Dubbo | ICoachService / @DubboService |
| MongoDB | growth_profiles, ai_quizzes, pet_states |
| Redis | jelly:coach:tasks:*, streak, leaderboard |
| RabbitMQ | jelly.coach.evaluation.completed |
| AI API | CoachAiEngine → DashScope |
| Nacos | coach.growth.* / evaluate.model.* |
| 调用链 | SkyWalking Agent |
| Docker | evaluate×2 + coach |

## 7. 总结

（填写个人收获：双实例负载、配置中心、事件驱动、AI 闭环）

## 8. 附件

- 源代码：`32308117_jellystudy.zip`（`package-project.bat` 生成）
