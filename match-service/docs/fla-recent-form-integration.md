# FLA Recent Form Metrics 集成报告

> 本文记录上一阶段的严格赛季/联赛验证方案。新业务规则已解除该门槛，当前行为和测试结果以 [全赛事 Recent Five 报告](fla-recent-form-all-competitions.md) 为准；AI 报告缓存一致性风险仍未解决。

## 交付状态

**已完成可独立验证的缓存、计算、受验证状态控制的 Metrics 接入与测试；尚不能宣称生产 Recent Form 完整启用。**

真实球队 HTML 没有可验证的 season ID。因此当前实际 API 返回 `SEASON_UNVERIFIED`，不会把当前
赛程默认为 Mapping 的赛季。另有一项独立阻塞：既有 AI Report 缓存只使用 standings snapshotId，
不能保证近期比赛更新后报告不被错误复用。本次按要求没有修改该应用服务、Prompt、Generator 或 OpenRouter。

## A. 架构与实际检查结果

检查了 MatchOpponentMetricsServiceImpl、OpponentMetricsCalculator、实际响应类型
`MatchOpponentMetricsResponse`（仓库不存在名为 OpponentMetricsResponse 的类）、FlaClient/Impl、
FlaTeamMatchResponse、FlaTeamMatchesHtmlParser、FlaStandingsSnapshotServiceImpl、FlaCacheServiceImpl、
ExternalDataSnapshot、FlaTeamMapping、OpponentAnalysisApplicationServiceImpl、AI input mapper 和使用方。

原近期结果来自 `FlaStandingEntryResponse.form`，被规范化为 `won/draw/lost` 后截取 5/10，连胜等
在该序列上计算。新版积分榜没有这个数据，旧路径因此会产生空结果和误导性的零计数。没有已有的
独立 Recent Form 模型。MatchInfo 使用 OffsetDateTime 表示 matchTime，生命周期为
SCHEDULED/ONGOING/FINISHED/CANCELLED；它与外部 HTML 比赛状态是不同枚举。

现有 standings Snapshot 为 PostgreSQL/JPA，按 provider/championship/season/hash 持久化，TTL
默认 6 小时。FlaCacheServiceImpl 是 championship/team 数据库目录，不是 Redis 缓存。match-service
没有 Redis 依赖，也没有 Flyway/Liquibase；现有迁移方式是 `match-service/sql/*.sql` 手工脚本，
默认 application.yml 使用 ddl-auto=validate，Docker 配置为 update。新增实现沿用 JPA 和 SQL 脚本约定。

### 来源、缓存和版本

`GET /teams/{id}` 使用既有配置的 RestClient，`getTeamMatchesPage` 返回比赛与页面来源身份。
原 `getTeamMatches` 和 `getRecentCompletedMatches` 仍可使用；Metrics 不使用后者，因为其“截至今天、
所有赛事”的语义不能用于历史赛前分析。

比赛写入独立的 `match.fla_team_matches_snapshot`，绝不写入 external_data_snapshot。保存：
team ID、可空的来源 season ID、canonical JSON、SHA-256、fetchedAt。来源 championship ID、
比赛明细和 schemaVersion 包含在 JSON 中。SHA-256 同时作为稳定的 versionId/主键；哈希包含球队
身份、来源身份和比赛数据，不包含 fetchedAt、TTL 或请求的 Mapping。重排比赛不会改变哈希；
数据或来源身份改变会创建新版本。旧版本保留供报告 metricsJson 中的 versionId 追溯。

| 条件 | 行为 | 缓存状态 |
| --- | --- | --- |
| 非强制且缓存新鲜 | 读取数据库，不请求 FLA | HIT |
| 初次非强制抓取 | 保存新版本 | MISS |
| 过期/强制抓取，内容变化 | 保存新版本，保留旧版本 | REFRESHED |
| 抓取内容与已存版本相同 | 复用版本，只单调推进 fetchedAt | REUSED_IDENTICAL_PAYLOAD |

forceRefresh 首次抓取也标为 REFRESHED，与原 standings 语义一致。TTL 独立配置：
`opponent-analysis.fla.recent-matches-ttl` / `FLA_RECENT_MATCHES_TTL`，默认 6h。
HIT/MISS 等为获取状态，和 Recent Form 是否可验证是两个独立维度。

插入与 touch 使用独立事务。插入竞争导致的唯一键失败在提交时回滚并翻译异常后，外层才能重新
读取竞争获胜者，避免在已中止的 PostgreSQL 事务里继续查询。touch 使用条件更新防止 fetchedAt
倒退。两个球队按 team ID 查询，哈希也包含 team ID，读取时再校验 envelope/哈希，防止污染。

没有框架自动迁移，也没有执行数据库变更。部署前必须按现有流程应用
`sql/create-fla-team-matches-snapshot.sql`；否则默认 validate 模式不能通过新表校验。
脚本仅新增比赛表和索引，不修改原表或数据。历史版本暂无自动清理，后续清理应保留被报告引用的版本。

### 赛季与联赛验证

真实 `team-270.html`、`team-63.html` 均有唯一 `/championships/16` 链接，但没有 season ID。
页面上的年份、球队创建日期、分组数字、当前日期、积分榜默认赛季都不能代替来源赛季证据。
`parsePage` 因此明确返回 `saisonId=null`，没有增加假想 data-season 属性或根据日期推断季节。

验证顺序：来源失败 → 来源赛季可验证 → 与 Mapping 同季 → 来源 championship 与 Mapping 相同
→ 显式配置的精确联赛名称 → 时间和比赛筛选。对应状态包括 SOURCE_UNAVAILABLE、SEASON_UNVERIFIED、
SEASON_MISMATCH、COMPETITION_UNVERIFIED、COMPETITION_MISMATCH、MATCH_TIME_UNAVAILABLE。

未来来源若能可靠提供赛季身份，还需要人工核验后的精确名称映射，例如：

```yaml
opponent-analysis:
  fla:
    recent-matches-ttl: 6h
    recent-form-league-names:
      "16": "Foot à 7 - Vendredi - 2ème division"
```

**没有提供默认名称映射，也没有用本配置绕过赛季验证。** 配置必须通过实际 FLA 联赛关联核验，
不能用任意内部 Tournament 名称自动建立。FlaChampionnat.name 来自同步时传入的名称，不能证明
它就是 HTML 的 competitionName。仅 LEAGUE 且名称精确相等的记录进入计算；杯赛和相似名称不混入。
若页面已提供其他联赛名称却没有所配置名称，返回 COMPETITION_MISMATCH。

### 时间与纯计算

`RecentFormCalculator` 不访问 HTTP、数据库或缓存。输入明确球队、已验证的联赛名称、目标 matchTime、
来源 fetchedAt 与比赛记录。双方都使用同一个目标截止时间。

将目标 OffsetDateTime 转换为 Europe/Paris 日期，**只接受日期严格早于目标巴黎日期的比赛**。
由于 HTML 没有完赛时间，同日比赛全部排除，即使它有更早的开球时间；不会假设固定 60/90 分钟
即可完赛。还排除晚于来源观测日期的矛盾记录。系统当前时间只用于缓存 TTL，不用于历史赛前窗口。
这一规则会少算同日已结束比赛，但不会仅凭 kickoff 的先后把它们纳入。

先筛 LEAGUE/精确名称、状态与比分、球队身份、日期，再去重、按日期/时间倒序，最后截取 5/10。
没有比分使用 null，合法 0–0 可以计算。结果保持现有 `won/draw/lost` JSON 编码，不改为大写枚举。

窗口胜率以实际窗口场次为分母，保留两位小数；空窗口胜率为 null。进球与失球根据 team ID
选择主/客队比分。sampleSize 是全部符合条件的可用比赛数，不强制等于 5/10。所有 streak 由实际
完整可用序列计算，最长连胜/连败仅表示这份样本，不声称是完整赛季纪录。

### Metrics 合并与不可用数据

MatchOpponentMetricsServiceImpl 先完成原有比赛、双方 Mapping、standings Snapshot 和球队校验，
再取得双方近期数据并传入 OpponentMetricsCalculator 的新纯计算重载。同一请求用 team ID map
避免相同球队重复获取。forceRefresh 同时传到 standings 和 Recent Form 数据获取。

近期来源失败仍保留 standings/league 指标。超时、5xx、解析/身份错误保留明确 errorCode；数据库
或缓存损坏使用 FLA_RECENT_SNAPSHOT_UNAVAILABLE。不把失败转换成“零比赛”，也不静默读取过期
缓存。意外编程错误不会被统一 catch 成“来源失败”。

| 情况 | available | sampleSize | 顶层胜平负/streak | recentFive/recentTen |
| --- | --- | --- | --- | --- |
| ≥10 场可靠比赛 | true | 实际数量 | 实际整数 | 实际结果 |
| 1–9 场可靠比赛 | true，状态 INSUFFICIENT_MATCHES | 实际数量 | 实际整数 | 不补足 |
| 已验证上下文但没有完成比赛 | false，NO_COMPLETED_MATCHES | 0 | null | [] |
| 来源失败或上下文未验证 | false，明确原因 | null | null | [] |

已验证空样本的嵌套窗口计数是已知的 0，胜率仍为 null；顶层近期计数/streak 不作为可用指标。
近期比较只有双方都有非空可靠样本才返回 recentFiveWinsDifference，否则为 null，
recentFormComparisonAvailable=false。两队真实差值 0 时 availability=true，可以区分。

旧四参数 Calculator 重载保留给现有内部 league 聚合/旧调用；Metrics 集成路径使用带 RecentFormMetrics
的重载，绝不退回 entry.form 补数据。

standings 原始 snapshotId、payloadHash、fetchedAt 均不变。响应增加
`standingsTemporalScope=FETCH_TIME_SNAPSHOT_NOT_HISTORICAL_PREMATCH`，明确其观测时间属性，
不声称当前积分榜是目标比赛当时排名，没有实现历史积分榜回放。

## B. 文件修改清单

下表为本阶段新增文件（Java 路径相对于 `src/main/java/com/yukai/team/matchservice/opponentanalysis/`）：

| 文件 | 作用 |
| --- | --- |
| dto/FlaTeamMatchesPageResponse.java | 携带来源 team/championship/可空 season 与比赛 |
| dto/RecentFormMetrics.java | 状态、实际样本、结果、两窗口进失球/胜率、独立来源元数据 |
| entity/FlaTeamMatchesSnapshot.java | 单独的持久化内容版本 |
| repository/FlaTeamMatchesSnapshotRepository.java | 按球队读取最新版本，原子推进抓取时间 |
| service/FlaTeamMatchesSnapshotWriter.java | 独立事务插入和 touch，支持竞争失败后回滚重读 |
| service/FlaTeamMatchesSnapshotService.java | 独立 TTL、canonical 哈希、缓存四种状态和完整性验证 |
| service/FlaRecentFormService.java | 来源失败降级、赛季/赛事验证、调用纯计算器 |
| service/RecentFormCalculator.java | 保守时间/赛事过滤、窗口与本队进失球计算 |

其他新增文件：

- `sql/create-fla-team-matches-snapshot.sql`：遵循现有手工迁移方式的独立建表脚本，未执行。
- `docs/fla-recent-form-integration.md`：本报告。
- `docs/fla-recent-form-api-examples.json`：由真实 fixture 驱动实现生成的完整 MISS/HIT 响应；
  内部比赛上下文、数据库和 standings snapshot ID 是测试示例，不是真实数据库记录。
- `src/test/java/.../service/RecentFormCalculatorTest.java`：真实比分预览、主客场、窗口、过滤、streak、胜率和比较。
- `src/test/java/.../service/FlaRecentFormServiceTest.java`：真实赛季无法确认、模拟已验证来源契约、错误及赛事限制。
- `src/test/java/.../service/FlaTeamMatchesSnapshotServiceTest.java`：TTL、哈希、版本、刷新、隔离、错误及竞争重读。
- `src/test/java/.../repository/FlaTeamMatchesSnapshotPersistenceTest.java`：H2 PostgreSQL 模式下的真实 JPA 持久化与独立事务回滚验证。
- `src/test/java/.../service/RecentFormSerializationTest.java`：新增状态、null、历史 JSON 和 OpenAPI schema 兼容。

修改文件：

- client/FlaClient.java、FlaClientImpl.java：增加 getTeamMatchesPage，使用同一 HTTP 基础设施。
- parser/FlaTeamMatchesHtmlParser.java：增加页面来源提取；原比赛解析规则保留。
- config/FlaProperties.java：独立 TTL、显式精确联赛名称配置。
- service/MatchOpponentMetricsServiceImpl.java：合并受验证控制的 Recent Form，传递 forceRefresh，去除重复球队获取。
- service/OpponentMetricsCalculator.java：带近期输入的纯计算重载；不可用时 null；可空近期比较。
- dto/TeamPerformanceMetrics.java：增加 recentForm 及只读 availability/sampleSize 属性；既有字段名称和类型保留，
  旧构造签名保留。近期计数/streak 的 OpenAPI 标明可为 null。
- dto/TeamComparisonMetrics.java：近期比较可为 null，并增加只读 availability。
- dto/MatchOpponentMetricsResponse.java：增加只读 standings 时间属性和报告缓存兼容性限制标识。
- controller/OpponentAnalysisMetricsController.java：更新 OpenAPI 描述与不可用示例；没有修改 Tag 名称。
- application.yml、application-docker.yml、application-prod.yml：独立 TTL 环境变量。
- `src/test/java/.../service/MatchOpponentMetricsServiceImplTest.java`：保留原断言并增加可用/不可用合并、真实 fixture
  的 season gate、单侧缺失、forceRefresh 和同队去重验证。
- docs/fla-team-recent-matches.md：标注其为上一阶段报告并链接当前集成状态。

上一阶段已存在但尚未 Git 提交的 FlaTeamMatchResponse、两个 HTML fixture、Parser/Client 测试及
FlaClientImplTest 的构造调整保留在工作区；没有把它们重新计为本阶段新增测试。

没有修改 standings Parser/缓存、ExternalDataSnapshot、FlaTeamMapping 数据、Match/Player/Team
功能、前端、Prompt、OpenRouter、AI Generator、AI Application Service 或报告仓库复用查询。

## C. 真实 fixture 示例与计算预览

完整 API 示例见 [fla-recent-form-api-examples.json](fla-recent-form-api-examples.json)。
目标为示例内部比赛 `2026-11-13T20:00:00Z`（巴黎 21:00），球队数据来自 2026-11-09 抓取的真实 fixture；
积分榜 fixture 为 2026-11-03 的原有样本。目标上下文不是对外部 FLA 某场实际赛程的断言。

真实 API 的两队均为：

```json
{
  "recentFive": [],
  "recentTen": [],
  "recentFiveWins": null,
  "recentFiveDraws": null,
  "recentFiveLosses": null,
  "recentTenWins": null,
  "recentTenDraws": null,
  "recentTenLosses": null,
  "currentWinningStreak": null,
  "currentDrawingStreak": null,
  "currentLosingStreak": null,
  "currentUnbeatenStreak": null,
  "longestWinningStreak": null,
  "longestLosingStreak": null,
  "recentFormAvailable": false,
  "recentFormSampleSize": null,
  "recentForm": {
    "status": "SEASON_UNVERIFIED",
    "sampleSize": null,
    "results": [],
    "recentFive": null,
    "recentTen": null
  }
}
```

实际完整 recentForm 还带 source，示例 versionId/payloadHash 为：

| 球队 | 稳定版本 | 首次 | 再次 | 来源 season | TTL |
| --- | --- | --- | --- | --- | --- |
| 270 | 2c9c7ee937c71308b9834f6f38f1068542a3d74ae4b679263f2f64d8c127f98b | MISS | HIT | null | 21600 |
| 63 | 86720ec9043361fcf35f2ffb5b7dafafb1be56531914942214894e57918ecf26 | MISS | HIT | null | 21600 |

两队有不同哈希，重复请求不重新抓取。source 中包含实际示例运行的 fetchedAt、forceRefreshRequested=false。
comparison.recentFiveWinsDifference=null，recentFormComparisonAvailable=false。

下面是**纯计算器预览，不是已验证的 API 可用结果**。它证明真实比分在未来有赛季/联赛身份依据后
能得到什么结果，没有给 fixture 编造 season ID：

| 指标 | YEXIAO PARIS FC（270） | 18 EMPIRE（63，仅联赛） |
| --- | --- | --- |
| 实际可用联赛样本 | 6 | 4 |
| recentFive | lost, won, won, won, won | won, won, draw, lost |
| recentTen | lost, won, won, won, won, won | won, won, draw, lost |
| 五场窗口胜/平/负 | 4/0/1 | 2/1/1 |
| 十场窗口胜/平/负 | 5/0/1 | 2/1/1 |
| 当前连胜/连平/连败 | 0/0/1 | 2/0/0 |
| 当前连续不败 | 0 | 3 |
| 样本内最长连胜/连败 | 5/1 | 2/1 |
| 五场窗口进球/失球 | 27/8 | 11/11 |
| 十场窗口进球/失球 | 31/9 | 11/11 |
| 五场窗口胜率 | 0.80（4/5） | 0.50（2/4） |
| 十场窗口胜率 | 0.83（5/6） | 0.50（2/4） |

若双方来源均获验证，该预览 recentFiveWinsDifference=2。18 EMPIRE 的三场杯赛全部被排除。
2026-11-06 客场 18 EMPIRE 3–2 YEXIAO 中，YEXIAO 进球=2、失球=3，未发生客场比分反转错误。
同一 fixture 以 `2026-08-01T00:00:00Z` 为目标输入计算器，得到 NO_COMPLETED_MATCHES、sampleSize=0、
空结果和 null 胜率，11 月比赛不进入 8 月窗口。生产路径在更早的 season gate 就已经阻止未验证来源。

## D. 测试命令与结果

Java 17，JUnit 5、Mockito、Spring Mock HTTP、真实 Jsoup fixture，持久化测试为 H2 PostgreSQL 模式。
没有依赖 FLA 实时网络，没有执行生产数据库或 Redis 操作，没有新增框架依赖。

仓库根目录执行：

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home \
mvn -o -pl match-service \
  -DargLine=-javaagent:/Users/yukhoi/.m2/repository/org/mockito/mockito-core/5.20.0/mockito-core-5.20.0.jar \
  -Dtest=RecentFormCalculatorTest,FlaRecentFormServiceTest,FlaTeamMatchesSnapshotServiceTest,FlaTeamMatchesSnapshotPersistenceTest,RecentFormSerializationTest,MatchOpponentMetricsServiceImplTest,FlaTeamMatchesHtmlParserTest,FlaTeamMatchesClientTest,FlaClientImplTest,FlaStandingsSnapshotServiceImplTest,OpponentMetricsCalculatorTest,FlaStandingsHtmlParserTest \
  test

JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home \
mvn -o -pl match-service \
  -DargLine=-javaagent:/Users/yukhoi/.m2/repository/org/mockito/mockito-core/5.20.0/mockito-core-5.20.0.jar \
  test
```

本机显式 javaagent 用于避免沙箱限制 Mockito 自附加，不修改项目配置。

| 范围 | 总数 | 通过 | 失败 | 错误 | 跳过 |
| --- | ---: | ---: | ---: | ---: | ---: |
| 定向全部相关测试 | 173 | 173 | 0 | 0 | 0 |
| Match Service 全量 | 362 | 361 | 1 | 0 | 0 |

相对上一阶段 328 项，新增 **34** 项：5 个新测试类共 30 项，既有 Metrics Service 测试新增 4 项。
新增失败为 **0**。原 Parser、Client、standings Snapshot、Calculator、AI application/generator 和其他
服务测试都在全量执行范围内。

唯一失败仍为 `OpponentAnalysisMetricsControllerTest.exposesOpenApiContract:199`：期望
`Opponent Analysis Metrics`，实际 Tag 为 `Opponent Analysis`。上一阶段已在未修改 HEAD 副本复现，
本次全量仍观察到同一失败，没有改 Tag、删除断言或禁用测试。

覆盖真实来源赛季未知、模拟已验证契约的可用分支、窗口 5/10 与不足数量、主客场、平局与 0–0、
连胜/连败/不败、样本内最长 streak、胜率分母、赛事/杯赛过滤、历史与未来/观测日期过滤、巴黎/UTC
跨日及同日保守规则、单侧缺失、null 比较、forceRefresh、四种缓存状态、TTL 过期、版本变化、稳定哈希、
球队隔离、损坏缓存、竞争重读和真实事务回滚、保存/读取 JSON、历史缺字段读取以及 OpenAPI 新 schema。

H2 验证不能替代 PostgreSQL 并发负载测试；没有声称已在真实 PostgreSQL 实例执行建表或压力验证。

## E. 风险与后续工作

### 必须解决的启用条件

1. **来源赛季证据**：需要可靠的 FLA season metadata 或经验证的带赛季接口/页面关联；目前页面
   不提供，不能用 MatchInfo.seasonSnapshot、年份范围、默认 season 或人工猜测覆盖它。
2. **联赛身份**：需要核验 championship ID 与精确 competitionName 的关联，然后配置显式名称映射。
   当前不采用模糊匹配，也没有自动重写 FlaTeamMapping。
3. **AI Report 复用键**：当前查询是
   `(matchId, standingsSnapshotId, provider, model, promptVersion, language, COMPLETED)`。
   新数据由两个球队来源组成，仅沿用该键不正确。standings 不变、近期比分/状态变化，或同一
   standings 下来源从失败恢复为可用，都可能命中旧报告。

后续建议的键：保留现有模型/语言等维度，新增稳定的 metrics input fingerprint，至少包含
standings snapshotId/hash、双方 recent versionId、双方 availability/验证状态、来源 season 和
精确联赛范围、目标 matchTime/时区/过滤规则版本。来源失败等无版本情况必须编码为明确状态，不能
省略；单纯加入 recent versionId 也不能区分同版本的验证配置变化。不要把 composite hash 冒充
现有 Long snapshotId，也不要把比赛数据写入 standings 表来间接改变该 ID。

report.metricsJson 现在能保存来源版本，便于追溯，但**保存来源不等于复用查询已包含来源**。
响应明确带 `reportCacheCompatibility=REQUIRES_COMPOSITE_SOURCE_KEY`。本次没有修改复用行为，
因此不能保证已有报告缓存的正确失效，也没有声称端到端 AI 集成完整完成。
在键设计完成前，forceRefresh=true 会按现有逻辑绕过报告复用并刷新两类 FLA 来源，但这不能代替
自动缓存一致性的修复。

### API 与历史数据限制

既有字段名称、List<String>/Integer/BigDecimal 类型和 won/draw/lost 编码保留；增加 recentForm、
availability、sampleSize 和来源限制属性。Integer 近期计数/streak 以及 recentFiveWinsDifference
从“不可用时 0”改为 null，是需要调用方适配的语义变化，已更新 OpenAPI 并测试 JSON。
旧存档缺少新增字段仍可反序列化，保留原构造签名；未验证外部前端是否正确处理 null，不能声称
客户端兼容风险为零。仓库可见的 AI input mapper 使用可空 Integer，未发现额外拆箱失败，全量回归通过。

历史赛季数据仍缺失，不做回放；standings fetchedAt 可能晚于目标比赛，不能解读为历史排名。
近期样本是页面当前观测的结果，可能包含后续修正，不是当时完整历史档案。没有唯一外部比赛 ID，
当前解析器的复合去重键仍存在极少数同日同时间同主客队同轮次比赛不可区分的限制。

计算正确性不要求修改 AI Prompt。本阶段也没有修改 Prompt；若未来要让 AI 解释近期样本量、
不可用原因、窗口进失球和 standings 时间限制，还需另行更新 AI input contract 与相关提示，
并同时解决报告复用键。当前 AI input mapper 仍只映射原有近期结果列表等字段，没有声称新增
进失球和 availability 已被模型消费。
