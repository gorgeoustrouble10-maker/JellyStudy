# JellyCoach 成长教练 — 深度审计与修复报告

> 审计时间：2026-05-21  
> 问题：**成长教练页按钮点击「没反应」**

---

## 1. 审计结论（Executive Summary）

| 层级 | 状态 | 说明 |
|------|------|------|
| 后端 API | ✅ 正常 | Gateway 8080 → Coach 8084 路由通，出题/周报/批改可用 |
| 前端交互 | ❌ 已修复 | 原代码**静默吞掉错误**、**无 loading 反馈**、**出题后需滚动**才看到题目 |
| 业务逻辑 | ⚠️ 已修复 | 新用户 0 积分 → 喂养直接 500；无友好提示 |
| 异常处理 | ❌ 已修复 | Coach 无 `@RestControllerAdvice`，业务异常变 500 |

**根因不是「功能没开发」，而是 UX + 异常处理导致用户感知为「按了没反应」。**

---

## 2. 逐项审计

### 2.1 后端链路（通过）

```
浏览器 → Vite:9945/api/coach/* → Gateway:8080 → Coach:8084
```

实测（修复前）：

| 接口 | 结果 |
|------|------|
| GET `/api/coach/profile` | 200 ✅ |
| POST `/api/coach/quiz/generate?weakPoint=Redis` | 200 ✅ AI 出题 |
| GET `/api/coach/report/weekly` | 200 ✅ AI 周报 |
| POST `/api/coach/pet/feed?points=10` | **500** ❌ 积分不足抛未捕获异常 |

### 2.2 前端问题（已修复）

| # | 问题 | 影响 |
|---|------|------|
| F1 | `startQuiz` / `loadReport` / `submitQuiz` **无 catch** | API 失败时页面无任何提示 |
| F2 | 无 loading 文案 | AI 调用 3～10 秒，用户以为按钮无效 |
| F3 | 出题成功后题目在**页面下方**，无滚动 | 用户看不到「AI 巩固练习」区块 |
| F4 | 喂养失败返回 500，错误格式不统一 | 红字提示不明确 |
| F5 | 薄弱点用 `<span @click>` 而非 `<button>` | 部分浏览器/accessibility 点击体验差 |
| F6 | 0 积分时喂养按钮仍可点 | 必然失败 |

### 2.3 业务设计问题（已修复）

- 老用户 `demo-user` 在 MongoDB 中 `totalPoints=0`
- 喂养需 10 分，但**没有先赚积分的引导**
- 修复：**积分 &lt; 10 时自动补发 30 新手积分**；新用户建档默认 30 分

---

## 3. 本次修复清单

### 后端 `jellystudy-coach-service`

1. `CoachExceptionHandler` — `IllegalArgumentException` → HTTP 400 + `{ message: "..." }`
2. `grantWelcomeBonusIfNeeded()` — demo-user 积分不足时补 30 分
3. 喂养错误文案：`积分不足（当前 X 分）。请先完成下方 AI 练习获取积分。`

### 前端 `CoachPanel.vue`

1. 全局 `actionLoading` + 旋转 loading 条（出题中/批改中/周报中）
2. 绿色 `success` / 红色 `error` 提示条
3. 所有 async 操作统一 `extractError()` 解析后端 message
4. 出题成功后 `scrollIntoView` 滚到练习区
5. 积分 &lt; 10 时禁用喂养按钮并显示当前积分
6. 薄弱点/任务改为 `<button type="button">`

---

## 4. 推荐演示流程（答辩用）

1. 打开 **成长教练** → 应看到积分 ≥ 30（新手礼包）
2. 点击 **Redis → 练习** → 顶部出现「AI 正在出题…」→ 自动滚到题目
3. 填写答案 → **提交 AI 批改** → 显示分数 + 积分增加
4. 点击 **喂养 (-10 积分)** → 宠物 EXP 增加
5. 点击 **生成周报** → 下方出现 AI Markdown 周报

---

## 5. 技术栈对照（大作业）

| 要求 | Coach 模块实现 | 验证方式 |
|------|----------------|----------|
| Spring Boot | jellystudy-coach-service:8084 | `/api/health/coach` |
| Dubbo | ICoachService @DubboService | Nacos 服务列表 |
| MongoDB | growth_profiles / pet_states / ai_quizzes | Mongo Express 或 mongosh |
| Redis | 今日任务 / streak / 排行榜 | `redis-cli KEYS jelly:coach:*` |
| RabbitMQ | evaluation.completed 消费 | 评估答案后看 Coach 诊断更新 |
| AI API | CoachAiEngine → DashScope | 出题/批改/周报 Trace |
| Nacos | coach.growth.daily-goal-count | `/api/coach/config` |
| 调用链 | SkyWalking agent | UI :8090 |
| Docker | evaluate×2 + coach | docker-compose.services.yml |

---

## 6. 仍待完善（非阻塞）

- [ ] 真实用户体系（当前固定 demo-user）
- [ ] 评估完成 → RabbitMQ → Coach 自动诊断（需先完成一次答案评估）
- [ ] Coach 单元测试 / 集成测试
- [ ] 积分规则可全部放 Nacos 热更新

---

## 7. 重启说明

修改后端后需重启 Coach：

```powershell
# 停掉旧 coach 进程后
java -jar jellystudy-parent\jellystudy-coach-service\target\jellystudy-coach-service-1.0.0-SNAPSHOT.jar
```

前端 HMR 自动生效，或硬刷新 `Ctrl+Shift+R`。
