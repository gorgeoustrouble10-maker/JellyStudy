# JellyStudy 运行指南（推荐并行流程）

## 当前状态速查

| 项目 | 状态 |
|------|------|
| Maven 编译 | 终端 `mvn compile` 应为 **BUILD SUCCESS** |
| IDE 300 红线 | 多为 Lombok 误报，以 Maven 为准 |
| Redis | 已可单独运行 `jellystudy-redis:6379` |
| 业务数据 | **MySQL** 库 `jellystudy`（知识点/问答），重启不丢 |
| 评估数据 | **MySQL** 库 `jellystudy_evaluate` |
| 不用 MongoDB | 全项目统一 MySQL + Redis 缓存 |

**数据库端口**：Docker 默认 `localhost:3307`（映射容器 3306）。若本机 3306 空闲可设 `set MYSQL_PORT=3306`。

**前端问题列表**：`全部`（MySQL 时间倒序）| `热门`（Redis 热度榜）| `常看`（Redis 浏览榜）

**中文路径**：若 Maven/Docker 乱码，运行 `scripts\copy-to-dev-path.bat` 复制到 `C:\dev\jellystudy`

**Clash/代理**：访问 `127.0.0.1` 前执行 `$env:HTTP_PROXY=''; $env:HTTPS_PROXY=''; $env:ALL_PROXY=''`，否则 Gateway 可能 502。

**提交清单**：见 `docs/SUBMISSION_CHECKLIST.md`；一键准备：`powershell -File scripts\submit-prep.ps1`

**千问评估**：`set DASHSCOPE_API_KEY=sk-xxx` 后运行 `start-evaluate-qianwen.bat`

**千问 Coach（苏格拉底/出题）**：在项目根配置 `local-secrets.bat`（可复制 `local-secrets.bat.example`），用 `scripts\start-full-stack.ps1` 或 `scripts\start-java-services.ps1` 启动；仅重启 Coach 可双击 `restart-coach.bat` 或执行 `scripts\restart-coach.ps1`（会自动 `call local-secrets.bat`，勿裸跑 `java -jar`）。

**苏格拉底两种模式**（成长教练页）：`追问` = 苏格拉底引导 + 卡壳拆解；`纯科普` = 先 4～8 句通俗讲解（零基础友好），偏好会保存在浏览器本地。

**产品化自检**（无截图/报告）：`powershell -File scripts\check-runtime-prereqs.ps1 -RunMavenTests`；Coach 健康 `GET http://127.0.0.1:8084/api/health` 含 `components.dashscope`（密钥未配时为 DOWN）；前端列表统一走 `unwrapApiList` 解析 `{code,data}` 信封。

**GitHub 同步**：远程 [JellyStudy](https://github.com/gorgeoustrouble10-maker/JellyStudy.git)。本地若有未提交改动，需自行 `git add` / `commit` / `push` 后 GitHub 才与电脑一致（当前 `main` 可能与 `origin/main` 同提交但工作区仍有大量未推送修改）。

**SkyWalking**：`pull-skywalking-images.bat` → `docker compose -f docker-compose.skywalking.yml up -d` → UI http://localhost:8090 → `scripts\download-skywalking-agent.ps1` → `start-with-skywalking.bat`

**写报告用素材**：`docs/week11-sequence-diagrams.md`、`docs/REDIS_READ_BEHAVIOR.md`

---

## 数据持久化说明

| 存储 | 用途 |
|------|------|
| MySQL `jellystudy` | 知识点、问题、回答（JPA，`ddl-auto: update`） |
| MySQL `jellystudy_evaluate` | 大模型评估记录 |
| Redis | 热门榜、浏览缓存、评估读缓存（非主存储） |

首次启动 Docker MySQL 会自动执行 `init-database.sql`。若容器已存在旧卷、缺表，可：

```powershell
docker exec -i jellystudy-mysql mysql -uroot -p123456 < init-database.sql
```

或依赖 JPA `ddl-auto: update` 自动建表。

---

## 路径 A：不等待 Docker（立刻能做）

### 1. 消除 IDE 误报

1. `Ctrl+Shift+P` → **Java: Clean Java Language Server Workspace** → Reload  
2. Maven 更新选 **Yes**  
3. 安装扩展：**Lombok**（GabrielBB.vscode-lombok）

### 2. 验证编译

```powershell
cd jellystudy-parent
& "C:\Users\xuan\.cursor\extensions\oracle.oracle-java-25.1.0-universal\nbcode\java\maven\bin\mvn.cmd" clean compile -DskipTests
```

### 3. 写报告

编辑 `实验报告-Redis与微服务.md`，补截图后导出 PDF：`32308117_吕宇轩.pdf`

### 4. 先起知识点服务（不依赖 Nacos）

```powershell
cd jellystudy-parent\jellystudy-knowledge
mvn spring-boot:run
```

### 5. 前端

```powershell
cd frontend
npm install
npm run dev
```

访问 http://127.0.0.1:9945

---

## 路径 B：Docker 核心设施（MySQL + Nacos + Redis）

**不要一次拉全套**，用脚本逐个拉：

```powershell
# 双击或在终端运行
pull-core-images.bat
start-core.bat
```

或手动：

```powershell
docker pull mysql:8.0
docker pull nacos/nacos-server:v2.2.3
docker compose -f docker-compose.core.yml up -d
```

验证：

- Nacos http://localhost:8848/nacos  
- `docker exec jellystudy-redis redis-cli -a jellystudy_redis ping` → PONG  

---

## 路径 C：启动全部后端（需路径 B 完成）

顺序：**knowledge → evaluate → qa**

```powershell
# 终端1
cd jellystudy-parent\jellystudy-knowledge
mvn spring-boot:run

# 终端2
cd jellystudy-parent\jellystudy-evaluate-service
mvn spring-boot:run

# 终端3
cd jellystudy-parent\jellystudy-qa
mvn spring-boot:run
```

或双击 `start-all-services.bat`（会先编译再开三个窗口）。

---

## 路径 D：SkyWalking（可选，最后做）

```powershell
pull-skywalking.bat
docker compose up -d skywalking-oap skywalking-ui
```

UI：http://localhost:8090  

Agent 参数见下文。

---

## SkyWalking Java Agent（演示链路）

```text
-javaagent:C:\tools\skywalking-agent\skywalking-agent.jar
-Dskywalking.agent.service_name=jellystudy-qa
-Dskywalking.collector.backend_service=127.0.0.1:11800
```

---

## 演示检查清单

1. Nacos 看到 3 个 Dubbo 服务  
2. 创建问题 → 评估面板有数据  
3. 提交回答 → DBeaver 查 `answer_evaluation`  
4. `GET /api/questions/hot` 热门榜  
5. `docker exec jellystudy-redis redis-cli -a jellystudy_redis ZREVRANGE jelly:hot:questions 0 9 WITHSCORES`  
6. SkyWalking 一张调用链截图  

---

## 打包提交

```powershell
package-project.bat
```

- `32308117_jellystudy.zip`  
- `32308117_吕宇轩.pdf`  

---

## 常见问题

**Docker pull EOF** → 换热点 / 重跑 `pull-core-images.bat`  

**Dubbo 调不通** → 确认 Nacos 已起，三服务日志无 `Failed to subscribe`  

**评估无数据** → 确认 MySQL 已 init，`evaluate.model.type` 为 `mock`  
