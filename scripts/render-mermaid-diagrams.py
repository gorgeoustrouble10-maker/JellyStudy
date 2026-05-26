# -*- coding: utf-8 -*-
"""Render Mermaid diagrams to PNG via mermaid.ink (needs network)."""
import base64
import json
import urllib.error
import urllib.request
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs" / "diagrams"
OUT.mkdir(parents=True, exist_ok=True)

DIAGRAMS = {
    "seq-01-hot": """sequenceDiagram
    participant U as 用户
    participant F as 前端
    participant QA as 问答服务8082
    participant R as Redis
    participant DB as MySQL
    U->>F: 点击热门
    F->>QA: GET /api/questions/hot
    QA->>R: ZREVRANGE jelly:hot:questions
    R-->>QA: questionId + score
    loop 每个 id
        QA->>R: GET jelly:question:id
        alt 缓存命中
            R-->>QA: JSON
        else 未命中
            QA->>DB: SELECT question
            DB-->>QA: 行数据
        end
    end
    QA-->>F: 热门列表
    F-->>U: 展示""",
    "seq-02-viewed": """sequenceDiagram
    participant U as 用户
    participant F as 前端
    participant QA as 问答服务
    participant R as Redis
    participant DB as MySQL
    U->>F: 点击常看
    F->>QA: GET /api/questions/recommended
    QA->>R: ZREVRANGE jelly:view:rank
    loop 每个 id
        QA->>R: GET jelly:question:id
        alt 未命中
            QA->>DB: SELECT
        end
    end
    QA-->>F: 常看列表""",
    "seq-03-detail": """sequenceDiagram
    participant U as 用户
    participant F as 前端
    participant QA as 问答服务
    participant DB as MySQL
    participant R as Redis
    U->>F: 展开问题卡片
    F->>QA: GET /api/questions/id
    QA->>DB: view_count + 1
    QA->>R: 更新 view rank 与 question 缓存
    QA-->>F: QuestionDTO""",
    "seq-04-create-eval": """sequenceDiagram
    participant U as 用户
    participant F as 前端
    participant QA as 问答服务
    participant EV as 评估服务8083
    participant DB as MySQL
    participant R as Redis
    U->>F: 提交问题
    F->>QA: POST /api/questions
    QA->>DB: INSERT question
    QA->>R: ZADD 热门与浏览榜
    QA->>EV: Dubbo evaluateQuestion
    EV->>EV: 千问或 Mock
    EV->>DB: INSERT question_evaluation
    EV->>R: SET eval question id
    QA-->>F: 201 成功""",
    "seq-05-eval-cache": """sequenceDiagram
    participant F as 前端
    participant EV as 评估服务
    participant R as Redis
    participant DB as MySQL
    F->>EV: GET evaluations questions id
    EV->>R: GET jelly:eval:question:id
    alt 命中
        R-->>EV: JSON
    else 未命中
        EV->>DB: SELECT
        EV->>R: SET TTL 10min
    end
    EV-->>F: 评估 DTO""",
}


def pako_deflate(data: bytes) -> bytes:
    # mermaid.ink uses raw deflate (pako compatible)
    co = zlib.compressobj(level=9, wbits=-zlib.MAX_WBITS)
    return co.compress(data) + co.flush()


def to_url(graph: str) -> str:
    compressed = pako_deflate(graph.encode("utf-8"))
    encoded = base64.urlsafe_b64encode(compressed).decode("ascii")
    return f"https://mermaid.ink/img/{encoded}?type=png&bgColor=white"


def download(name: str, graph: str) -> None:
    url = to_url(graph)
    dest = OUT / f"{name}.png"
    req = urllib.request.Request(url, headers={"User-Agent": "JellyStudy-report/1.0"})
    with urllib.request.urlopen(req, timeout=120) as resp:
        dest.write_bytes(resp.read())
    print(f"OK {dest} ({dest.stat().st_size} bytes)")


def main() -> None:
    manifest = {}
    for name, graph in DIAGRAMS.items():
        try:
            download(name, graph)
            manifest[name] = f"docs/diagrams/{name}.png"
        except urllib.error.HTTPError as e:
            print(f"FAIL {name}: HTTP {e.code}")
            raise
    (OUT / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print("Done:", OUT)


if __name__ == "__main__":
    main()
