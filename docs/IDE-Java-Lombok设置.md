# 减少 IDE 红点（Java + Lombok）

项目已开启 `java.jdt.lombokSupport.enabled`，**不必**再装第三方 Lombok 插件（易冲突）。

## 1. 安装扩展（一次性）

1. 按 `Ctrl+Shift+X` 打开扩展  
2. 搜索并安装：**Extension Pack for Java**（`vscjava.vscode-java-pack`）  
   - 内含 Language Support for Java、Maven、调试等  

## 2. 让 IDE 重新加载 Maven 工程

1. `Ctrl+Shift+P` → 输入 **`Java: Clean Java Language Server Workspace`** → 回车 → 选 **Reload**  
2. 等待右下角 **Building workspace** 完成（首次可能 2～5 分钟）  
3. 若仍红点：`Java: Force Java Compilation` → **Full**

## 3. 确认打开的是本仓库根目录

应打开文件夹：`32308117_吕宇轩2`（内含 `jellystudy-parent/pom.xml`）。

## 4. 以 Maven 为准

终端执行：

```bat
cd jellystudy-parent
..\tools\apache-maven-3.9.6\bin\mvn.cmd compile -DskipTests
```

若 **BUILD SUCCESS**，红点可忽略，不影响运行与交作业。

## 5. 工作区设置说明

`.vscode/settings.json` 已配置：

- Lombok 支持  
- 排除 `frontend`、`node_modules` 等，避免误索引  
- Maven 使用项目自带 `tools/apache-maven-3.9.6`  

**切换工作区**有时能清缓存，但根本解决办法是第 2 步 Clean Workspace。
