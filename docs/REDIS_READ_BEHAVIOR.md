# Redis 读路径说明（写报告时可引用）

## 三种读法对比

| 场景 | 接口 | 主要数据源 | 说明 |
|------|------|------------|------|
| 热门榜 | `GET /questions/hot` | Redis ZSET `jelly:hot:questions` | 按热度分排序，详情优先读 `jelly:question:{id}` |
| 常看榜 | `GET /questions/recommended` | Redis ZSET `jelly:view:rank` | 按累计浏览量排序 |
| 问题详情 | `GET /questions/{id}` | **MySQL 为权威** | 浏览量 +1 写库后同步 Redis；库无记录时才退回缓存 |

## 为何详情不「纯读缓存」？

浏览量是业务真值，必须在 MySQL 递增，否则重启或缓存过期会丢次数。流程是：

1. 读库并 `view_count + 1`
2. 写回 Redis（ZSET + 详情 STRING）

这样满足作业「降低数据库负荷」：列表类高频读走 Redis；详情写一次库保证一致。

## 「最近」定义（7 天窗口）

- 仅 `updatedAt`（无则用 `createdAt`）在 **7 天内** 的问题进入热门/常看 ZSET
- 得分带时间衰减：`基础热度 × (1 - 天数×0.85/7)`，超出窗口 score=0 并从榜中移除
- 配置项：`jellystudy.redis.recent-window-days`（默认 7）

## 同步策略

- **写穿**：提问/点赞/回答/浏览时更新 MySQL + Redis
- **启动预热**：QA 服务启动后立即从 MySQL 全量同步 ZSET
- **定时**：每 5 分钟 `QuestionRankSyncScheduler` 全量对齐 ZSET
- **空榜兜底**：访问热门/常看接口时若 Redis 为空则自动 rebuild
