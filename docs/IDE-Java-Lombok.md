# 减少 IDE 红点（Lombok / Java）

项目 **Maven 能编译即可交作业**；红点多为 IDE 未识别 Lombok 生成的方法。

## 1. 安装扩展（Cursor / VS Code）

按 `Ctrl+Shift+X`，安装：

- **Extension Pack for Java**（`vscjava.vscode-java-pack`，内含 Lombok 支持）

**不要**再装旧的 `GabrielBB.vscode-lombok`（易与内置支持冲突）。

## 2. 用工作区打开（推荐）

双击打开根目录下的 **`JellyStudy.code-workspace`**，不要只打开单个随意子文件夹。

## 3. 刷新 Java 语言服务

1. `Ctrl+Shift+P` → **Java: Clean Java Language Server Workspace**
2. 选 **Reload and delete**
3. 等待右下角 **Building workspace** 完成（首次可能 2～5 分钟）

## 4. 确认设置（已写在 `.vscode/settings.json`）

- `java.jdt.lombokSupport.enabled`: `true`
- `maven.executable.path`: 指向项目内 `tools/apache-maven-3.9.6/bin/mvn.cmd`

## 5. 仍以 Maven 为准

```bat
cd jellystudy-parent
..\tools\apache-maven-3.9.6\bin\mvn.cmd compile -DskipTests
```

显示 **BUILD SUCCESS** 即代码无问题，可忽略残留 IDE 警告。
