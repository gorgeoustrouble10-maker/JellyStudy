# 上传 GitHub 指南

## 可以上 GitHub 吗？

**可以。** 本项目已做基本脱敏，适合作为**公开学习仓库**或**私有仓库**。

### 已保护（不会进 Git）

| 文件/目录 | 说明 |
|-----------|------|
| `local-secrets.bat` | 含 DashScope Key，已在 `.gitignore` |
| `.env` / `.env.local` | 环境变量密钥 |
| `**/target/` | 编译产物 |
| `frontend/node_modules/` | 依赖 |
| `32308117_jellystudy.zip` | 提交用压缩包 |
| `tools/` | 本地 Maven 等工具 |

### 仓库里会有（课程默认密码，可公开）

- `application.yml` 里 MySQL `123456`、Redis `jellystudy_redis` —— **仅本地 Docker 开发用**
- 文档里的连接说明 —— 无真实生产数据

### 上传前必做检查

```bat
:: 1. 确认密钥文件未被 git 跟踪
git status
:: 不应出现 local-secrets.bat

:: 2. 全局搜 sk- 密钥（不应有真实 Key）
:: 在 IDE 搜 sk- 排除 .example 和文档占位符

:: 3. 若 Key 曾在聊天/截图泄露 → DashScope 控制台轮换
```

---

## 首次上传步骤（零基础版）

### 你刚才报错的原因

你在 `C:\Users\xuan>` 里输入了：

```bat
cd 32308117_吕宇轩2
```

系统说 **「找不到路径」**，因为项目**不在用户文件夹根目录**，而在 **桌面**：

```text
正确位置：C:\Users\xuan\Desktop\32308117_吕宇轩2
错误理解：C:\Users\xuan\32308117_吕宇轩2  （这里没有这个项目）
```

---

### 第 0 步：打开 CMD，进入项目文件夹

**方法一（推荐，复制这一行）：**

```bat
cd /d C:\Users\xuan\Desktop\32308117_吕宇轩2
```

- `cd` = 切换目录  
- `/d` = 可以同时换盘符（从 C 盘任意位置跳到 Desktop 下的文件夹）  
- 成功后提示符应变成类似：`C:\Users\xuan\Desktop\32308117_吕宇轩2>`

**方法二：** 在资源管理器打开桌面上的 `32308117_吕宇轩2` 文件夹 → 地址栏输入 `cmd` 回车 → 会自动在该目录打开 CMD。

**验证是否在正确目录：**

```bat
dir
```

应能看到 `jellystudy-parent`、`frontend`、`README.md` 等，而不是一堆别的文件。

---

### 第 1 步：检查是否已安装 Git

```bat
git --version
```

- 若显示 `git version 2.x.x` → 继续  
- 若提示不是内部命令 → 安装 [Git for Windows](https://git-scm.com/download/win)，装完**重新开 CMD**

---

### 第 2 步：初始化并提交（只在第一次做）

**逐行执行**（每行回车一次）：

```bat
cd /d C:\Users\xuan\Desktop\32308117_吕宇轩2

git init

git add .

git status
```

**看 `git status` 输出：**

| 应该看到 | 不应该看到 |
|----------|------------|
| `jellystudy-parent/`、`frontend/`、`README.md` 等 | `local-secrets.bat` |
| | 大量 `target/`、`node_modules/` |

若出现 `local-secrets.bat`，**不要 commit**，告诉我或先执行：

```bat
git reset HEAD local-secrets.bat
```

确认无误后提交：

```bat
git commit -m "feat: JellyStudy microservices with Redis, Gateway, Testcontainers"
```

---

### 第 3 步：在 GitHub 网站建空仓库

1. 浏览器打开 https://github.com 并登录  
2. 右上角 **+** → **New repository**  
3. **Repository name**：例如 `jellystudy`（英文，不要空格）  
4. 选 **Private**（作业建议私有）  
5. **不要**勾选 “Add a README” / “Add .gitignore”（本地已有）  
6. 点 **Create repository**  
7. 复制页面上 HTTPS 地址，形如：  
   `https://github.com/你的GitHub用户名/jellystudy.git`

---

### 第 4 步：推送上去

把下面命令里的 URL **换成你刚复制的**：

```bat
cd /d C:\Users\xuan\Desktop\32308117_吕宇轩2

git remote add origin https://github.com/你的GitHub用户名/jellystudy.git

git branch -M main

git push -u origin main
```

- 第一次 push 可能弹出 GitHub 登录窗口，按提示登录  
- 成功后会显示 `Branch 'main' set up to track remote branch 'main'`

---

### 第 5 步：看 CI 是否通过

1. 打开你的 GitHub 仓库页面  
2. 顶部点 **Actions**  
3. 等 1～3 分钟，出现绿色 ✓ 即 Testcontainers 等在云端跑过

---

## 外部 CMD 跑 Testcontainers（5 passed）

与上传 GitHub **分开做**；用于本机验证 Redis 测试。

**前提：** Docker Desktop 已启动（托盘图标正常）。

```bat
cd /d C:\Users\xuan\Desktop\32308117_吕宇轩2\jellystudy-parent

..\tools\apache-maven-3.9.6\bin\mvn.cmd test -pl jellystudy-qa -Dtest=QuestionRedisServiceTestcontainersTest
```

成功最后一行类似：

```text
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

若 `Skipped: 5`：Docker 未就绪 → 开 Docker Desktop 等 1 分钟再跑；或 push 后看 GitHub Actions 绿勾即可。

---

## 命令速查（你当前项目完整路径）

| 要做的事 | 命令 |
|----------|------|
| 进入项目根目录 | `cd /d C:\Users\xuan\Desktop\32308117_吕宇轩2` |
| 进入 Maven 工程 | `cd /d C:\Users\xuan\Desktop\32308117_吕宇轩2\jellystudy-parent` |
| 跑 Redis 测试 | 见上一节 |
| 上传 GitHub | 见「首次上传步骤」第 2～4 步 |

---

## CI（GitHub Actions）

推送后自动运行 `.github/workflows/ci.yml`：

1. JDK 17 编译全项目  
2. 跑 `QuestionRankScoringTest` + `QuestionRedisServiceTestcontainersTest`（GitHub 自带 Docker）  
3. 编译 Gateway 模块  

在仓库 **Actions** 页查看绿勾即通过。

---

## 推荐仓库设置

| 项 | 建议 |
|----|------|
| 可见性 | Private（作业）或 Public（作品集，去掉学号路径名） |
| Description | Spring Boot + Dubbo + Redis + Gateway 学习项目 |
| Topics | `spring-boot`, `redis`, `dubbo`, `microservices`, `vue3` |
| 不要提交 | PDF 实验报告、截图 zip（体积大，可选单独 Release） |

---

## 克隆后他人如何跑

见根目录 `README.md` 与 `RUN_GUIDE.md`。  
密钥：复制 `local-secrets.bat.example` → `local-secrets.bat` 自行填写。
