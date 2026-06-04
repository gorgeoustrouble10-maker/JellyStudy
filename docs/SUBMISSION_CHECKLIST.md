# JellyStudy 提交清单（32308117 吕宇轩）

> 除 **截图 PNG** 与 **Word 实验报告** 需人工完成外，其余自动化项已于 `scripts/submit-prep.ps1` 覆盖。

## 自动化已完成项

| 项 | 状态 | 说明 |
|----|:----:|------|
| Docker 双实例 evaluate-1/2 | ✅ | `start-full-stack.ps1` + Gateway `dual` profile |
| Nacos 四服务配置 | ✅ | evaluate / coach / **knowledge** / **qa** |
| Nacos 热更新 | ✅ | 已实测 QA `recent-window-days` 7→14 后 `/api/questions/config` 同步 |
| JellyCoach 独立模块 + 边界文档 | ✅ | `docs/平台认证与JellyCoach模块边界.md` |
| 验收脚本 | ✅ | `scripts/verify-z12-demo.ps1` 全绿 |
| 提交 zip | ✅ | `32308117_jellystudy.zip`（无 target/node_modules） |

## 仍需人工完成（P0）

1. **截图** — 按 `docs/截图操作指南.md` 保存至 `docs/screenshots/`  
   - 基础 8 张 + Z12 专用 10 张（含 `z12-nacos-refresh.png`）
2. **Word** — 按 `docs/Word报告粘贴指南.md` 从 `实验报告-第十二周-Z12.md` 复制一至七章 → **`32308117_吕宇轩.docx`**
3. **上传** — `32308117_吕宇轩.docx` + `32308117_jellystudy.zip`

## 一键命令（答辩/演示前）

```powershell
# 清代理（Clash 会导致 localhost 502）
$env:HTTP_PROXY=''; $env:HTTPS_PROXY=''; $env:ALL_PROXY=''

cd "c:\Users\xuan\Desktop\32308117_吕宇轩2"
docker compose -f docker-compose.core.yml up -d
powershell -File scripts\import-nacos-config.ps1
powershell -File scripts\start-full-stack.ps1
powershell -File scripts\verify-z12-demo.ps1
```

前端另开：`cd frontend && npm install && npx vite --host 127.0.0.1 --port 9945`

## Nacos 配置验证 URL

| 服务 | 接口 |
|------|------|
| Evaluate | `GET /api/evaluations/instance-info`（8083 / 8085 / Gateway 轮询） |
| Coach | `GET /api/coach/config` |
| Knowledge | `GET /api/knowledge-points/config` |
| QA | `GET /api/questions/config` |

## 热更新演示步骤（拍 z12-nacos-refresh.png）

1. 浏览器打开 Nacos → 配置管理 → `jellystudy-qa.yaml`
2. 将 `recent-window-days: 7` 改为 `14` → 发布
3. 刷新 `http://127.0.0.1:8082/api/questions/config`，应显示 `"recentWindowDays":14`
4. 截图：左 Nacos 配置 + 右浏览器 JSON 对比

Knowledge 同理：改 `knowledge.list.max-list-size` 为 `3`，再访问 `/api/knowledge-points` 列表变短。

## 答辩：Auth 与 JellyCoach 如何区分？

引用 **`docs/平台认证与JellyCoach模块边界.md`**：Auth 是 Layer A 平台基础设施（`/api/auth`），JellyCoach 是 Layer C 业务（`/api/coach`），同进程部署、文档化分离。

## 环境问题说明（可写入报告）

- **Clash/代理**：访问 `127.0.0.1` 前清空 `HTTP_PROXY`/`HTTPS_PROXY`，或将 localhost 加入直连
- **Docker Desktop**：需先启动，再 `docker compose up`
- **中文路径**：evaluate-2 使用相对路径 `java -jar` 规避（见 `start-full-stack.ps1`）
