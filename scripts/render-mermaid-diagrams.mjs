import { writeFileSync, mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const OUT = join(__dirname, '..', 'docs', 'diagrams');
mkdirSync(OUT, { recursive: true });

const DIAGRAMS = {
  'seq-01-hot': `sequenceDiagram
    participant U as 用户
    participant F as 前端
    participant QA as "问答服务(8082)"
    participant R as Redis
    participant DB as MySQL
    U->>F: 点击「热门」
    F->>QA: GET /api/questions/hot
    QA->>R: ZREVRANGE jelly:hot:questions
    R-->>QA: questionId + score
    loop 每个 id
        QA->>R: GET jelly:question:{id}
        alt 缓存命中
            R-->>QA: JSON
        else 未命中
            QA->>DB: SELECT question
            DB-->>QA: 行数据
        end
    end
    QA-->>F: 热门列表
    F-->>U: 展示`,
  'seq-02-viewed': `sequenceDiagram
    participant U as 用户
    participant F as 前端
    participant QA as 问答服务
    participant R as Redis
    participant DB as MySQL
    U->>F: 点击「常看」
    F->>QA: GET /api/questions/recommended
    QA->>R: ZREVRANGE jelly:view:rank
    loop 每个 id
        QA->>R: GET jelly:question:{id}
        alt 未命中
            QA->>DB: SELECT
        end
    end
    QA-->>F: 常看列表`,
  'seq-03-detail': `sequenceDiagram
    participant U as 用户
    participant F as 前端
    participant QA as 问答服务
    participant DB as MySQL
    participant R as Redis
    U->>F: 展开问题卡片
    F->>QA: GET /api/questions/{id}
    QA->>DB: view_count + 1
    QA->>R: 更新 view:rank + SET question:{id}
    QA-->>F: QuestionDTO`,
  'seq-04-create-eval': `sequenceDiagram
    participant U as 用户
    participant F as 前端
    participant QA as 问答服务
    participant EV as "评估服务(8083)"
    participant DB as MySQL
    participant R as Redis
    U->>F: 提交问题
    F->>QA: POST /api/questions
    QA->>DB: INSERT question
    QA->>R: ZADD 热门/浏览榜
    QA->>EV: Dubbo evaluateQuestion(异步)
    EV->>EV: 千问/Mock
    EV->>DB: INSERT question_evaluation
    EV->>R: SET eval:question:{id}
    QA-->>F: 201 成功`,
  'seq-05-eval-cache': `sequenceDiagram
    participant F as 前端
    participant EV as 评估服务
    participant R as Redis
    participant DB as MySQL
    F->>EV: GET /api/evaluations/questions/{id}
    EV->>R: GET jelly:eval:question:{id}
    alt 命中
        R-->>EV: JSON
    else 未命中
        EV->>DB: SELECT
        EV->>R: SET TTL 10min
    end
    EV-->>F: 评估 DTO`,
};

async function fetchPng(graph) {
  const res = await fetch('https://kroki.io/mermaid/png', {
    method: 'POST',
    headers: { 'Content-Type': 'text/plain; charset=utf-8' },
    body: graph,
  });
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(`HTTP ${res.status}: ${text.slice(0, 200)}`);
  }
  return Buffer.from(await res.arrayBuffer());
}

for (const [name, graph] of Object.entries(DIAGRAMS)) {
  const buf = await fetchPng(graph);
  const dest = join(OUT, `${name}.png`);
  writeFileSync(dest, buf);
  console.log(`OK ${dest} (${buf.length} bytes)`);
}
