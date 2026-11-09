# FLA Recent Five：全正式赛事规则

## 当前行为

Recent Form 已解除 sourceSaisonId 和精确联赛名称门槛，可以直接使用真实球队页面中已完成的联赛、杯赛。
保留原结果编码、recentTen、独立 Snapshot 缓存和数据库结构；没有修改 AI 生成/复用逻辑。

处理流程：

1. 使用原 FlaTeamMatchesSnapshotService 获取该球队的独立 Snapshot，forceRefresh 原样传递。
2. 继续校验 HTML/缓存身份和 flaTeamId；失败返回 SOURCE_UNAVAILABLE 及明确 errorCode。
3. 保留 sourceSaisonId、versionId/payloadHash、fetchedAt、TTL 和缓存状态，仅作来源元数据。
   sourceSaisonId 为 null 或不同于 Mapping 都不阻止计算；页面 championship ID 也不限制近期赛事范围。
4. 只纳入明确的 LEAGUE 或 CUP 类型，且状态为 COMPLETED、双方比分有效、能确认当前球队视角。
   OTHER、未知/null 类型不会自动视为正式比赛。
5. 将目标 matchTime 转为 Europe/Paris 日期，排除当天及之后的所有比赛。因没有完赛时间，即使同日
   有更早 kickoff 也不纳入。同时保留不得晚于来源观测日期的检查，不使用“今天”替代历史目标时间。
6. 去重后按日期、开球时间倒序，同日未知时间排在已知时间之后。截取最近 5/10，实际不足时不补造。
7. 通过原 OpponentMetricsCalculator 合并近期结果、胜平负数量、streak 与比较；保留原 standings 指标。

范围明确返回：

```json
{
  "competitionScope": "ALL_COMPETITIONS",
  "seasonScope": "CROSS_SEASON_ALLOWED"
}
```

ALL_COMPETITIONS 当前明确指 LEAGUE 和 CUP，不包含尚未确认的其他类型。
**CROSS_SEASON_ALLOWED 表示规则允许跨赛季，不表示 FLA 页面已提供跨赛季完整历史。** 两份真实
fixture 仅代表页面当时提供的赛季赛程，不能声称已获得完整历史最近五场。

| 合格比赛数 | 状态 | recentFormAvailable |
| ---: | --- | --- |
| ≥5 | AVAILABLE | true |
| 1–4 | INSUFFICIENT_MATCHES | true |
| 0，来源有效且截止时间已知 | NO_COMPLETED_MATCHES | false |
| 来源失败 | SOURCE_UNAVAILABLE | false |
| 目标时间缺失 | MATCH_TIME_UNAVAILABLE | false |

sampleSize 保留全部合格可用记录数，两个嵌套窗口另有各自实际 sampleSize。主状态以五场为阈值，
recentTen 不足十场时仍返回实际记录，不因此把已有五场标为不足。没有数据时不计算胜率；不可用的
顶层计数/streak 和近期比较仍为 null，真实零胜与数据不可用可区分。

旧 SEASON_UNVERIFIED 等枚举值仅为读取历史报告保留，当前计算路径不再发出这些验证状态。
新增 scope 是 DTO 字段；旧存档缺失它们时保留 null，避免将历史“仅联赛”报告错误标记成全赛事。
已删除不再使用的 recentFormLeagueNames 配置属性，无需配置精确赛事名称。

## 本次修改文件

Java 路径相对于 `src/main/java/com/yukai/team/matchservice/opponentanalysis/`：

| 文件 | 修改 |
| --- | --- |
| service/FlaRecentFormService.java | 移除赛季/championship/名称 Gate，保留并显式检查球队身份和来源错误 |
| service/RecentFormCalculator.java | 纳入 LEAGUE+CUP，不限名称或赛季；不足阈值改为 5；输出统计范围 |
| config/FlaProperties.java | 删除不再使用的精确联赛名称配置；独立 TTL 不变 |
| dto/RecentFormMetrics.java | 增加 competitionScope/seasonScope，保留旧构造签名和历史状态读取 |
| dto/TeamPerformanceMetrics.java | 更新 OpenAPI 描述，说明合格正式比赛样本和不可用条件 |
| controller/OpponentAnalysisMetricsController.java | 更新 OpenAPI 规则说明和真实 fixture 的完整响应示例 |

测试路径相对于 `src/test/java/com/yukai/team/matchservice/opponentanalysis/service/`：

- FlaRecentFormServiceTest.java：null/不同 season、不同/未知 championship 无需门槛；身份错误仍失败；forceRefresh 和来源错误。
- RecentFormCalculatorTest.java：更新联赛+杯赛预期；增加跨赛季、不同联赛、未知类型、未知状态/无效比分和同日时间排序覆盖。
- MatchOpponentMetricsServiceImplTest.java：真实 Parser + Recent Form 服务 + Metrics 路径验证可用结果，并验证历史目标仍为空、standings 不变。
- RecentFormSerializationTest.java：新增 scope JSON 往返及历史缺字段兼容。

文档：

- docs/fla-recent-form-all-competitions.md：本次报告。
- docs/fla-recent-form-api-examples.json：重新生成完整 MISS/HIT Metrics 响应。
- docs/fla-recent-form-integration.md：标注原严格验证方案为历史方案并链接本报告。

已检查 MatchOpponentMetricsServiceImpl 和 OpponentMetricsCalculator，其现有合并路径可以直接使用
新结果，无需再次改动实现。双方 Mapping 的原有共同 standings 上下文校验保留，它不等于验证球队
页面的 sourceSaisonId。Snapshot 服务、实体、哈希规则、SQL、数据库结构、积分榜 Parser、Mapping
数据、前端、Prompt、OpenRouter、AI Generator 和 AI Application Service 均未修改。

## 真实 fixture 结果与 API 示例

使用 team-270.html 和 team-63.html，目标为示例内部比赛 **2026-11-13T20:00:00Z**，晚于样本中
所有已完成比赛。来源页面日期为 2026-11-09；积分榜沿用原 rankings fixture（2026-11-03 抓取）。
内部 matchId、双方内部 ID、standings snapshotId 和持久化层为示例/Mock，不声称是实际数据库比赛。

这次结果来自真实 Metrics 服务路径，**已不是绕过验证的纯计算预览**：

| 指标 | YEXIAO PARIS FC（270） | 18 EMPIRE（63） |
| --- | --- | --- |
| 状态 / 可用性 | AVAILABLE / true | AVAILABLE / true |
| sourceSaisonId | null | null |
| 实际合格样本 | 6 | 7 |
| recentFive | lost, won, won, won, won | won, won, draw, lost, lost |
| recentTen | lost, won, won, won, won, won | won, won, draw, lost, lost, draw, draw |
| 五场窗口胜/平/负 | 4/0/1 | 2/1/2 |
| 十场窗口胜/平/负 | 5/0/1 | 2/3/2 |
| 当前连胜/连平/连败 | 0/0/1 | 2/0/0 |
| 当前连续不败 | 0 | 3 |
| 可用样本内最长连胜/连败 | 5/1 | 2/2 |
| 五场窗口进球/失球 | 27/8 | 13/14 |
| 十场窗口进球/失球 | 31/9 | 17/18 |
| 五场窗口胜率 | 0.80 | 0.40 |
| 十场窗口胜率 | 0.83 | 0.29 |

18 EMPIRE 的杯赛现在按新规则正常参与。双方 recentFiveWinsDifference=2，
recentFormComparisonAvailable=true。客场比分仍从当前球队视角计算，例如 YEXIAO 在 18 EMPIRE
3–2 YEXIAO 一场的进球为 2、失球为 3。

完整可解析 JSON 见 [fla-recent-form-api-examples.json](fla-recent-form-api-examples.json)，包含第一次
MISS 和第二次 HIT 的全部 Metrics 字段，以及实际示例运行的 fetchedAt。两队来源版本分别为：

- 270：`2c9c7ee937c71308b9834f6f38f1068542a3d74ae4b679263f2f64d8c127f98b`
- 63：`86720ec9043361fcf35f2ffb5b7dafafb1be56531914942214894e57918ecf26`

同一来源数据在新业务规则下不需要重建 Snapshot；内容哈希和缓存语义保持不变。
以 2026 年 8 月为目标时，真实 fixture 仍没有合格比赛，返回 NO_COMPLETED_MATCHES，不会把
11 月结果用于 8 月赛前指标。独立数据库版本和缓存状态不代表统计历史覆盖完整。

## 测试结果

定向命令（仓库根目录，Java 17；显式 agent 避免本环境 Mockito 自附加限制）：

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home \
mvn -o -pl match-service \
  -DargLine=-javaagent:/Users/yukhoi/.m2/repository/org/mockito/mockito-core/5.20.0/mockito-core-5.20.0.jar \
  -Dtest=RecentFormCalculatorTest,FlaRecentFormServiceTest,RecentFormSerializationTest,MatchOpponentMetricsServiceImplTest,FlaTeamMatchesSnapshotServiceTest,FlaTeamMatchesSnapshotPersistenceTest \
  test
```

**49 项全部通过**。包含原独立缓存的 HIT/MISS/REFRESHED/REUSED_IDENTICAL_PAYLOAD、forceRefresh、
TTL 和持久化回归；缓存代码没有修改。

首次全量执行在测试发现前遇到 TeamPerformanceMetrics.class 缺失，未运行任何测试。将当前源码
复制到 `/tmp/fla-all-competitions-verify`，排除原 target 构建产物，在独立目录运行同一模块全部测试：

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home \
mvn -o -f /tmp/fla-all-competitions-verify/match-service/pom.xml \
  -DargLine=-javaagent:/Users/yukhoi/.m2/repository/org/mockito/mockito-core/5.20.0/mockito-core-5.20.0.jar \
  test
```

全量 **366 项，365 通过，1 失败，0 错误，0 跳过**。相对上一阶段净增 4 项，新增失败为 0。
唯一失败仍为既有 `OpponentAnalysisMetricsControllerTest.exposesOpenApiContract:199`，期望 Tag
`Opponent Analysis Metrics`，实际为 `Opponent Analysis`。没有修改 Tag、删除断言、禁用测试或更改
项目构建配置。Parser、Client、standings Snapshot、Metrics、AI 和其他 Match Service 测试均执行。

## 未解决的 AI Report 缓存风险

按要求没有修改 AI Report 业务逻辑。报告复用仍只依据 standings snapshotId 等既有键，未包含双方
recent source versionId 或计算规则版本。因此仍可能返回旧报告：

- 积分榜不变，近期比分、结果或来源可用性发生变化。
- 本次业务规则变化使同一来源 hash 从 SEASON_UNVERIFIED 变为 AVAILABLE，但报告键仍相同。

后续复用键必须包含双方近期来源版本、可用性、目标时间和计算规则/统计范围版本，不能只加入
payloadHash。当前响应继续保留 REQUIRES_COMPOSITE_SOURCE_KEY 提示。现有 forceRefresh=true
可绕过一次报告复用，但没有解决自动一致性问题；本次没有自动调用 AI 或声称旧报告已失效。
