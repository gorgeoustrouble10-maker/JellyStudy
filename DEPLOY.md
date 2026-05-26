# JellyStudy 小型部署指南

适合课程演示、局域网试用，非大规模商用。

## 架构

- **基础设施**：`docker compose -f docker-compose.core.yml up -d`（MySQL、Nacos、Redis）
- **后端**：knowledge `8081`、qa `8082`、evaluate `8083`
- **前端**：Vue 开发服 `9945`，生产可 `npm run build` 后用 Nginx 托管 `frontend/dist`

## 环境变量（可选）

| 变量 | 默认 | 说明 |
|------|------|------|
| `MYSQL_HOST` | 127.0.0.1 | MySQL 地址 |
| `MYSQL_PORT` | 3307 | 宿主机映射端口 |
| `EVALUATE_MODEL_TYPE` | mock | 改为 `qianwen` 并配置 `DASHSCOPE_API_KEY` |

## 生产打包步骤

1. `pull-core-images.bat` → `start-core.bat`
2. `cd jellystudy-parent` → `mvn clean package -DskipTests`
3. 三个 jar：`jellystudy-knowledge/target/*.jar`、`jellystudy-qa/target/*.jar`、`jellystudy-evaluate-service/target/*.jar`
4. `java -jar xxx.jar` 各开一个终端，或使用 `start-all-services.bat`
5. 前端：`cd frontend && npm run build`，将 `dist` 放到 Nginx

## 数据备份

```powershell
docker exec jellystudy-mysql mysqldump -uroot -p123456 --databases jellystudy jellystudy_evaluate > backup.sql
```

## 为何不用 MongoDB

问答与知识点已用 **MySQL + JPA** 与评估库统一技术栈，便于 SQL 备份、作业说明与小型产品运维；Redis 仅作缓存与排行榜。
