# 时序图 PNG（报告第六节）

| 文件 | 说明 |
|------|------|
| `seq-01-hot.png` | 查询最受欢迎榜 |
| `seq-02-viewed.png` | 查询最常查看榜 |
| `seq-03-detail.png` | 查看详情（浏览量 + 缓存） |
| `seq-04-create-eval.png` | 创建问题 + 异步评估 |
| `seq-05-eval-cache.png` | 评估读缓存（自设计） |

重新生成（需联网，调用 kroki.io）：

```bat
cd frontend
node ..\scripts\render-mermaid-diagrams.mjs
```

源码定义在 `scripts/render-mermaid-diagrams.mjs`。
