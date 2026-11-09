# AI Opponent Analysis：Recent Form 与报告缓存 v2

## 原架构与本次变更

`OpponentAnalysisReportController` 保留现有 POST、latest、history 和按 reportId 读取接口及权限。
`OpponentAnalysisApplicationServiceImpl` 校验比赛为 SCHEDULED，调用
`MatchOpponentMetricsServiceImpl.getMetrics(matchId, forceRefresh)`；该服务读取积分榜 Snapshot，
分别经 `FlaRecentFormService` / `FlaTeamMatchesSnapshotService` 获取独立球队比赛来源，使用既有 Calculator 计算指标。
本次没有修改 HTML Parser、统计规则、球队 Mapping 或 standings Snapshot 模型，也没有恢复赛季验证阻塞。

原服务在映射 AI Input 之前按 matchId、standings snapshotId、provider、model、promptVersion、language、
COMPLETED 查询复用。原 Mapper 只传积分榜和近期五场结果，未传 Recent Form 的统计、来源、状态和球队身份映射。

现流程为：Metrics → 结构化 AI Input → SHA-256 Fingerprint → 精确查询 COMPLETED → 复用或生成。
`forceRefresh=true` 原样传给 Metrics Service，刷新底层三类来源，并跳过报告复用。
`forceRefresh=false` 也必须先取得当前 Metrics，不能跳过来源有效性检查。

`OpenRouterOpponentReportGenerator` 通过现有 RestClient 调用 chat completions；
`PromptLoader` 加载 `prompts/opponent-analysis-system-prompt.txt`，user message 为 AI Input JSON。
现有 provider/model、重试、超时、token usage 和中文输出行为保持。
输出依靠 Prompt 约定及本地 JSON/DTO 校验，没有新增 provider-specific JSON Schema 参数。
生成器现在额外校验必填数组、字符串、非空 finding evidence 以及禁止多余 JSON 内容。

Writer 仍通过 REQUIRES_NEW 保存 PENDING，再按该记录 ID 写入 COMPLETED 或 FAILED。
新增 Fingerprint 和 AI Input 在 PENDING 时就保存，失败记录保留输入和错误码，不能参与 COMPLETED 复用。
latest 仍返回最新 COMPLETED；history 包含所有状态；历史 reportId 仍可读取。
并发请求没有新增全局锁或唯一键：可能生成多个相同输入的历史版本，每个请求只完成自己的记录；
已有 JPA @Version 保留。查询按 createdAt DESC、id DESC 排序，避免选取不确定或不匹配的报告。

## Fingerprint 与追溯

`MetricsInputFingerprint` 只接受 AI Input 或其 JSON 树。递归按对象 key 排序、保留数组顺序和 null，
规范化数值精度（0.80 与 0.8 等价），序列化为 UTF-8 JSON 后计算 SHA-256（64 位十六进制）。
Mapper 把比赛时间规范化为 UTC，因此同一时刻的不同 offset 不产生额外版本。
不使用业务对象默认 toString，也不直接 hash 整个 Metrics response。

AI Input 包含：

- matchId、比赛时间、赛事/赛季、主客角色。
- 两队内部 ID/名称、FLA ID/名称及 OUR_TEAM / OPPONENT 角色。
- 原有积分榜、联赛平均值和比较指标。
- 两队 Recent Form 状态、可用性、实际样本量、最近五/十场结果、窗口胜平负/进失球/胜率、六项连续战绩。
- competitionScope、seasonScope、来源 versionId/payloadHash、来源球队/赛季 ID 和不可用错误码。
- standings snapshotId/payloadHash、来源赛事、排序/排名语义。
- contractVersion=v2、recentFormRulesVersion=v1-paris-date-league-cup-cross-season。

不传 fetchedAt、cacheStatus、forceRefreshRequested、TTL 给 LLM，也不将它们纳入 Fingerprint。
报告仍在原 Metrics JSON 和 sourceFetchedAt 中保存抓取信息，但模型不分析具体抓取年龄。
抓取时间若改变 Calculator 的实际统计，则映射后统计变化仍会改变 Fingerprint。
规则版本由输入契约维护：未来改变 Calculator 过滤语义时必须同步提升 recentFormRulesVersion。

采用保守追溯策略：来源 Snapshot/version 改变，即使窗口统计相同，也会生成新报告。
因此复用报告始终绑定相同来源，不需要把新来源附到旧报告上。
provider/model/language/promptVersion 是独立查询维度，不重复纳入 hash。
本服务原有语言固定为 zh-CN；没有新增公开 language 参数。

## AI Input 与分析内容

保留原字段 `name`、积分榜指标、`recentFive`、`leagueAverage`、`comparison`、`dataLimitations`。
扩展 `identity`、`recentForm` 和 `provenance`。窗口采用 `RecentFormMetrics.Window`，避免重复展开胜平负字段。
`ourTeam.recentFive` 为五场结果；`ourTeam.recentForm.recentFive` 为五场统计；
`recentForm.recentTen` 为十场结果，`recentTenStatistics` 为十场统计。
胜率使用 0–1 比例，例如 0.8 为 80%。

matchId=11 Fixture 的输入节选（内部 ID、积分榜与第六场比分为合成测试数据；用户提供的五场统计和名称映射保持）：

```json
{
  "ourTeam": {
    "name": "YEXIAO PARIS FC",
    "identity": {"role":"OUR_TEAM","internalTeamName":"Coach Docker Team","flaTeamId":270,"flaTeamName":"YEXIAO PARIS FC"},
    "recentFive": ["lost","won","won","won","won"],
    "recentForm": {
      "recentFormAvailable":true,"recentFormStatus":"AVAILABLE","recentFormSampleSize":6,
      "recentFive":{"sampleSize":5,"wins":4,"draws":0,"losses":1,"goalsFor":27,"goalsAgainst":8,"winRate":0.8},
      "currentLosingStreak":1,"longestWinningStreak":5,
      "competitionScope":"ALL_COMPETITIONS","seasonScope":"CROSS_SEASON_ALLOWED"
    }
  },
  "opponent": {
    "name":"PARIS CROCOS FC",
    "identity":{"role":"OPPONENT","internalTeamName":"FLA Test Team 1","flaTeamId":204,"flaTeamName":"PARIS CROCOS FC"},
    "recentFive":["lost","lost","lost","lost","lost"],
    "recentForm":{
      "recentFormAvailable":true,"recentFormStatus":"AVAILABLE","recentFormSampleSize":5,
      "recentFive":{"sampleSize":5,"wins":0,"draws":0,"losses":5,"goalsFor":6,"goalsAgainst":16,"winRate":0},
      "currentLosingStreak":5
    }
  },
  "comparison":{"recentFiveWinsDifference":4,"recentFormComparisonAvailable":true}
}
```

SOURCE_UNAVAILABLE、NO_COMPLETED_MATCHES、MATCH_TIME_UNAVAILABLE 等状态分别保留；
不可用时窗口、胜率和连续战绩为 null，不用零值伪装缺失数据。
旧 Metrics 无 recentForm 时，Mapper 明确标为 SOURCE_UNAVAILABLE，不用旧空数组推断战绩。
INSUFFICIENT_MATCHES 保持可用并采用实际场数。一方不可用时，双方近期胜场差为 null、比较可用性为 false。

Prompt Version 在 Java 默认值及三个 application 配置中从 v1 提升至 v2。
原输出结构不变：threatLevel、summary、comparison、strengths、weaknesses、recommendations、dataLimitations。
近期状态/攻防趋势写入 summary 和 comparison；对手优势/弱点附带统计证据；备战建议写入 recommendations。
Prompt 要求区分事实、推断与未知，不虚构阵型、球员、录像结论、具体失球方式或训练情况。
限制包括所有已识别正式联赛/杯赛、允许跨季但来源历史可能不完整、实际样本量、
积分榜为抓取时点 Snapshot、缺少录像及球员级数据。

## 数据库迁移与部署

迁移：`match-service/sql/alter-opponent-analysis-report-input-fingerprint.sql`。
在已有 match schema 和 opponent_analysis_report 表上，使用有 DDL 权限的部署账号手工执行：

```bash
psql "$MATCH_DATABASE_URL" -v ON_ERROR_STOP=1 --single-transaction \
  -f match-service/sql/alter-opponent-analysis-report-input-fingerprint.sql
```

该脚本使用 ADD COLUMN IF NOT EXISTS 和 CREATE INDEX IF NOT EXISTS，可重复执行；
增加 nullable metrics_input_fingerprint VARCHAR(64)、ai_input_json TEXT 及复用查询索引。
不删除、回填或更新历史报告；不变更 snapshotId 或现有数据来源表。
历史指纹保持 null，仍可查看，但无法匹配新请求产生的非空 SHA-256。
部署顺序：先执行 SQL，再部署应用；确认运行配置 prompt-version=v2，且没有外部配置覆盖为旧版本。
普通 CREATE INDEX 会在建索引期间占用表锁，按现有部署窗口执行。
本次仅在本地 H2 测试执行脚本，没有执行生产迁移。

## 验证与可复现示例

运行整个 match-service（本机 Mockito 需启动代理，避免沙箱不允许动态 attach）：

```bash
mvn -pl match-service test \
  -DargLine="-javaagent:$HOME/.m2/repository/org/mockito/mockito-core/5.20.0/mockito-core-5.20.0.jar"
```

重点测试：

- `MetricsInputFingerprintTest`：相同输入、JSON key 顺序、数字精度、等价时间 offset、抓取/缓存元数据稳定；比分、结果、身份、时间、可用性、来源和规则/契约版本改变失效。
- `OpponentAnalysisInputMapperTest`：match 11 五场真实统计、角色、样本、连续战绩、缺失与部分可用数据、不足五场实际样本。
- `OpponentAnalysisApplicationServiceImplTest`：缓存命中、新建、forceRefresh、失败标记、历史读取。
- `OpponentAnalysisReportRepositoryTest`：H2 实际持久化与完整复用维度、null 历史指纹排除、FAILED 排除、时间相同按 ID 选择、SQL 连续执行两次。
- `OpenRouterOpponentReportGeneratorTest`：MockRestServiceServer 捕获实际 RestClient 请求，验证 Recent Form 和来源限制进入 Prompt，旧字段仍存在，新输入通过旧输出 DTO，错误 JSON 被拒绝。
- 原 Recent Form、Snapshot、Metrics Service 和全量回归测试均执行；没有删除测试或跳过断言。

`OpponentAnalysisReportRepositoryTest.matchElevenPostUsesRealMapperGeneratorAndPersistenceWithMockLlm`
通过 MockMvc 调用原有接口：

```text
POST /api/v1/matches/11/opponent-analysis?forceRefresh=true
→ reused=false；Mock OpenRouter 收到真实结构的 Recent Form；H2 保存 64 位 Fingerprint 与 ai_input_json。
POST /api/v1/matches/11/opponent-analysis?forceRefresh=false
→ reused=true；返回同一 reportId；不再调用 LLM。
改变 Recent Form 比分/来源（standings 不变）再 POST forceRefresh=false
→ reused=false；保存新报告。
改变 standings（Recent Form 不变）再 POST forceRefresh=false
→ reused=false。
SOURCE_UNAVAILABLE，再恢复 AVAILABLE
→ 两次均生成与各自输入匹配的新报告。
再次 POST forceRefresh=true（已有匹配报告）
→ 仍生成新版本，保留全部历史。
```

这是 Controller + Application Service + Mapper + Generator + JPA Repository/Writer 的本地组合验证。
Metrics Service、比赛查询和 OpenRouter 是 Mock；数据库为 H2 PostgreSQL 模式。
没有连接真实 PostgreSQL、抓取 FLA 或调用付费 LLM，也没有宣称验证了生产 Gateway/鉴权网络链路。
Postman CLI 的 status 检查因本机 Keychain `SecItemCopyMatching failed -50` 退出；未使用 Postman simulation，
本次 HTTP 验证使用仓库已有 MockRestServiceServer，未创建额外 Postman 配置。

最终全量结果：401 项，400 通过，1 项失败，0 错误，0 跳过。唯一失败为已知的
`OpponentAnalysisMetricsControllerTest.exposesOpenApiContract`，期望 Tag `Opponent Analysis Metrics`，
实际为 `Opponent Analysis`；本次未修改该 Tag 或弱化断言。`git diff --check` 通过。

## 已知限制与后续

LLM 的自然语言事实准确性依赖 Prompt，JSON 验证器只能保证结构、必填项和现有语言约束，
不能证明所有文字数值或战术推断完全正确；上线前应抽查报告的统计引用。
部署环境仍需验证 PostgreSQL DDL 与一次完整 API 请求；这属于真实集成验证，不能由 Mock 替代。
同一 Fingerprint 并发可能重复计费生成，但不会错误复用或覆盖报告；如流量需要，可后续增加轻量请求合并。
来源版本变化即重新生成是有意的保守策略，后续若要跨来源复用，需要先设计新的追溯关联。

## 修改文件清单

- `match-service/docs/opponent-analysis-recent-form-ai.md`
- `match-service/sql/alter-opponent-analysis-report-input-fingerprint.sql`
- `match-service/src/main/java/com/yukai/team/matchservice/opponentanalysis/config/OpponentAiProperties.java`
- `match-service/src/main/java/com/yukai/team/matchservice/opponentanalysis/controller/OpponentAnalysisReportController.java`
- `match-service/src/main/java/com/yukai/team/matchservice/opponentanalysis/dto/MatchOpponentMetricsResponse.java`
- `match-service/src/main/java/com/yukai/team/matchservice/opponentanalysis/dto/ai/OpponentAnalysisInput.java`
- `match-service/src/main/java/com/yukai/team/matchservice/opponentanalysis/entity/OpponentAnalysisReport.java`
- `match-service/src/main/java/com/yukai/team/matchservice/opponentanalysis/repository/OpponentAnalysisReportRepository.java`
- `match-service/src/main/java/com/yukai/team/matchservice/opponentanalysis/service/MetricsInputFingerprint.java`
- `match-service/src/main/java/com/yukai/team/matchservice/opponentanalysis/service/OpenRouterOpponentReportGenerator.java`
- `match-service/src/main/java/com/yukai/team/matchservice/opponentanalysis/service/OpponentAnalysisApplicationServiceImpl.java`
- `match-service/src/main/java/com/yukai/team/matchservice/opponentanalysis/service/OpponentAnalysisInputMapper.java`
- `match-service/src/main/java/com/yukai/team/matchservice/opponentanalysis/service/OpponentAnalysisReportWriter.java`
- `match-service/src/main/java/com/yukai/team/matchservice/opponentanalysis/service/OpponentAnalysisReportWriterImpl.java`
- `match-service/src/main/resources/application-docker.yml`
- `match-service/src/main/resources/application-prod.yml`
- `match-service/src/main/resources/application.yml`
- `match-service/src/main/resources/prompts/opponent-analysis-system-prompt.txt`
- `match-service/src/test/java/com/yukai/team/matchservice/opponentanalysis/repository/OpponentAnalysisReportRepositoryTest.java`
- `match-service/src/test/java/com/yukai/team/matchservice/opponentanalysis/service/MetricsInputFingerprintTest.java`
- `match-service/src/test/java/com/yukai/team/matchservice/opponentanalysis/service/OpenRouterOpponentReportGeneratorTest.java`
- `match-service/src/test/java/com/yukai/team/matchservice/opponentanalysis/service/OpponentAnalysisApplicationServiceImplTest.java`
- `match-service/src/test/java/com/yukai/team/matchservice/opponentanalysis/service/OpponentAnalysisInputMapperTest.java`
- `match-service/src/test/java/com/yukai/team/matchservice/opponentanalysis/service/RecentFormAiFixture.java`
- `match-service/src/test/java/com/yukai/team/matchservice/opponentanalysis/service/RecentFormSerializationTest.java`
