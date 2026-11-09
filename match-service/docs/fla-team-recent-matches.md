# FLA 球队近期比赛调查与独立实现

> 本文记录上一阶段的独立解析交付。后续 Metrics 接入状态、独立缓存和生产阻塞点见 [集成报告](fla-recent-form-integration.md)。

## A. 真实 HTML 调查

验证日期：**2026-11-09**。使用项目现有 `FlaClientConfig.flaRestClient` 发出
`GET` / `Accept: text/html` 请求，再用 Jsoup 检查真实响应，三个页面均成功返回：

- `https://football-loisir-amateur.fr/teams/63`
- `https://football-loisir-amateur.fr/teams/270`
- `https://football-loisir-amateur.fr/championships/16/rankings?season=2`

| 球队页 ID | 页面球队名 | 积分榜球队链接 | 赛季比赛数 | 已完成 | À jouer |
| --- | --- | --- | ---: | ---: | ---: |
| 63 | 18 EMPIRE | /teams/63 | 29 | 7 | 22 |
| 270 | YEXIAO PARIS FC | /teams/270 | 26 | 6 | 20 |

ID 属于同一套标识，无需转换。球队页 canonical URL 也分别包含 63 和 270。
没有修改 FlaTeamMapping 或数据库。

真实 DOM 的关键结构如下；为说明结构省略了样式、空白和球场链接，测试 fixture 保留真实页面：

```html
<div x-data="teamPage()">
  <!-- 球队 hero 区域包含唯一球队 h1 -->
  <h1>YEXIAO PARIS FC</h1>
  <!-- 其他区域也渲染比赛，解析时只选下面的赛季日历 -->
  <div data-testid="calendrier-saison">
    <div data-testid="rencontre">
      <div>
        <span>Championnat</span>
        <span>Foot à 7 - Vendredi - 2ème division</span>
        <span>· Journée 3</span><span>· Aller</span>
        <span>Extérieur</span>
      </div>
      <div>
        <div>
          <p>Se déplace chez 18 EMPIRE</p>
          <p>ven. 6 nov. 2026 · 19h30 · <!-- 球场链接 --></p>
        </div>
        <div>
          <span data-testid="rencontre-score">2 – 3</span>
          <span>D</span>
        </div>
      </div>
    </div>
  </div>
</div>
```

可获取球队名称、球队页 ID、完整日期（含年份）、开球时间、对手名称、主客场、比分、V/N/D
结果标记、赛事类型、赛事名称和联赛轮次/往返信息。63 的页面含 26 场联赛及 3 场杯赛；270
的页面含 26 场联赛。杯赛名称为 `COUPE A 7 VENDREDI — Phase de poules`。

比赛卡片没有对手球队链接、对手 ID、赛事 ID 或唯一比赛 ID。只有球场地图链接；不能从地图
链接推断球队身份。上述 ID 不推造，已知的当前球队 ID 放入对应主队或客队字段，对手 ID 为 null。

**比分是当前球队视角的“本队 – 对手”，不是固定主队 – 客队。** 交叉验证 2026-11-06：
63 页面显示 `Reçoit YEXIAO PARIS FC / Domicile / 3 – 2 / V`；270 页面显示
`Se déplace chez 18 EMPIRE / Extérieur / 2 – 3 / D`。两者规范化后均为主队 18 EMPIRE 3–2
客队 YEXIAO PARIS FC。日期使用明确的 2026/2027 年，不假设当前年份。

已完成卡片同时提供比分和 V/N/D；未开赛卡片显示 `À jouer` 且无比分。两个样本没有延期或
取消状态，暂不声称识别这些状态，未验证标记保留为 UNKNOWN。有比分但缺少明确结果标记也不算已完成。

日历标题为 `Toutes les rencontres de la saison`，显示总数分别为 29 和 26，与日历卡片数量一致。
它包含当前页面提供的整个赛季已排赛程，不能据此保证将来新增杯赛轮次已存在，也未验证历史赛季查询。
两个响应没有比赛分页、赛季筛选或动态加载请求。`teamPage()` 脚本只切换 overview/matches/stats
标签和 URL hash，所有比赛已经在服务端 HTML 中，无需浏览器执行 JavaScript。

全页卡片数分别为 87 和 78。同一比赛在概览、赛季日历、统计区域重复出现。只解析唯一
`[data-testid=calendrier-saison]`，再对日历内部去重，不合并其他区域。

## B. 代码修改与接口

新增文件（相对于 match-service）：

| 文件 | 用途 |
| --- | --- |
| `src/main/java/com/yukai/team/matchservice/opponentanalysis/dto/FlaTeamMatchResponse.java` | 不可变比赛 DTO；含日期、时间、主客队、可空比分/ID、赛事类型/名称/轮次、状态；内嵌 MatchStatus、CompetitionType、MatchResult 枚举 |
| `src/main/java/com/yukai/team/matchservice/opponentanalysis/parser/FlaTeamMatchesHtmlParser.java` | 校验页面身份和赛季 DOM，提取比赛、转换客场比分、验证结果、去重 |
| `src/test/java/com/yukai/team/matchservice/opponentanalysis/parser/FlaTeamMatchesHtmlParserTest.java` | 真实 HTML 解析及基于该 DOM 的边界变异测试 |
| `src/test/java/com/yukai/team/matchservice/opponentanalysis/client/FlaTeamMatchesClientTest.java` | MockRestServiceServer 客户端、异常映射、近期筛选测试 |
| `src/test/resources/fla/team-63.html` | 真实 63 页面，2026-11-09 抓取，仅 CSRF meta token 替换为 REDACTED |
| `src/test/resources/fla/team-270.html` | 同日真实 270 页面，相同脱敏方式 |
| `docs/fla-team-recent-matches.md` | 本报告、实际示例、验证命令和后续建议 |

修改文件：

- `FlaClient.java`：新增下面两个方法，原积分榜方法不变。
- `FlaClientImpl.java`：注入独立比赛解析器；新增获取和筛选；将原有 GET/异常映射提取为私有
  `getHtml` 供两条路径复用。继续使用同一 RestClient、Base URL、超时、UTF-8 和 FlaClientException。
  积分榜 HTTP 状态映射、错误码和拒绝请求文案保持原行为。
- `FlaClientImplTest.java`：仅调整两个构造调用，传入新增解析器；原测试断言不变。

```java
List<FlaTeamMatchResponse> getTeamMatches(Long flaTeamId);
List<FlaTeamMatchResponse> getRecentCompletedMatches(Long flaTeamId, int limit);
```

`getTeamMatches` 返回当前赛季页面里有日期和对手的记录，包含未开赛比赛。缺失日期或对手的
记录跳过；日期没有年份或不合法时抛出明确解析异常。没有比分保持 null，0–0 是有效比分。
`resultFor(teamId)` 仅在明确已完成、有比分且该队 ID 可识别时返回 WIN/DRAW/LOSS，否则为 null。

`getRecentCompletedMatches` 只接受正数 limit，不限制为 5 或 10；要求 COMPLETED、双方比分
有效且比赛日期不晚于 Europe/Paris 当日，按日期倒序，同日按已知开球时间倒序。实际数量不足
时原样返回，不填充结果。默认包含所有赛事；后续按赛事筛选必须在截取 limit 之前完成。

日历内部使用日期、时间、主客队名、赛事类型/名称/轮次作为内部去重键，完全相同的卡片只保留
一条；相同键的比分或状态冲突时抛异常。这个键不作为外部比赛 ID，也不用于数据库持久化。
由于上游缺少比赛 ID，无法保证区分同日同时间、同赛事同轮次同主客队的两场真实比赛。

缺失身份、错误球队页、登录页、缺失日历或无法识别的非空日历抛出
`502 / FLA_INVALID_RESPONSE`。有效页面内明确的空日历返回空列表；有缺失日期等被过滤的记录
时也可能返回空列表。HTTP 404 对应 `502 / FLA_INVALID_REQUEST`，5xx 对应
`502 / FLA_UNAVAILABLE`，超时对应 `504 / FLA_TIMEOUT`，其他连接错误对应
`502 / FLA_IO_ERROR`。网络失败不会返回空列表，HTTP 错误响应内容不进入异常链。

未修改积分榜 Parser、FlaProperties、Snapshot 缓存和结构、映射数据、Metrics 服务/计算器、
Prompt、AI 报告服务、现有 API Response、前端或数据库；没有新增 REST endpoint 或数据库迁移。
新增方法尚无生产业务调用，现有同步和 AI 请求不会额外抓取球队页面。

## C. 真实 fixture 解析结果

下面是 `team-270.html` 解析得到的一条实际 DTO（JSON 展示，日期/时间为 ISO 文本）：

```json
{
  "matchDate": "2026-11-06",
  "kickoffTime": "19:30",
  "homeTeamId": null,
  "homeTeamName": "18 EMPIRE",
  "awayTeamId": 270,
  "awayTeamName": "YEXIAO PARIS FC",
  "homeScore": 3,
  "awayScore": 2,
  "competitionType": "LEAGUE",
  "competitionName": "Foot à 7 - Vendredi - 2ème division",
  "competitionRound": "· Journée 3 · Aller",
  "status": "COMPLETED"
}
```

`resultFor(270L) == LOSS`。尽管另一页已经验证 18 EMPIRE 为 63，此独立解析不跨页面猜测或
填补对手 ID，所以本条 homeTeamId 仍为 null。

以下表格通过真实 fixture 和客户端近期筛选方法取得，截至 2026-11-09。
比分列均为**当前球队进球 – 失球**；DTO 则始终按主队/客队保存。
每表前 5 行就是最近 5 场；全表就是请求最近 10 场的实际返回。

18 EMPIRE（63）：赛季 29 条，已完成 **7** 条；最近 5 返回 **5** 条，最近 10 返回 **7** 条。
其中已完成联赛 4 场、杯赛 3 场。

| 序号 | 日期 | 对手 | 主客场 | 本队比分 | 本队结果 | 赛事 |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | 2026-11-06 | YEXIAO PARIS FC | 主 | 3–2 | WIN | 联赛 |
| 2 | 2026-10-30 | ATHLETIK JEUNESSE FC | 客 | 4–2 | WIN | 联赛 |
| 3 | 2026-10-23 | FC MATE | 客 | 4–4 | DRAW | 联赛 |
| 4 | 2026-10-16 | NASSOUS FC | 客 | 0–3 | LOSS | 联赛 |
| 5 | 2026-10-09 | ASC PARISII B | 客 | 2–3 | LOSS | 杯赛 |
| 6 | 2026-10-02 | OLYMPIAKOS 7 | 客 | 2–2 | DRAW | 杯赛 |
| 7 | 2026-09-25 | AS CODE-BARRES | 客 | 2–2 | DRAW | 杯赛 |

YEXIAO PARIS FC（270）：赛季 26 条，已完成 **6** 条（均为联赛）；最近 5 返回 **5** 条，
最近 10 返回 **6** 条。

| 序号 | 日期 | 对手 | 主客场 | 本队比分 | 本队结果 | 赛事 |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | 2026-11-06 | 18 EMPIRE | 客 | 2–3 | LOSS | 联赛 |
| 2 | 2026-10-30 | 707 NATION | 主 | 6–2 | WIN | 联赛 |
| 3 | 2026-10-16 | PARIS CROCOS FC | 主 | 4–1 | WIN | 联赛 |
| 4 | 2026-10-09 | 707 NATION | 客 | 12–0 | WIN | 联赛 |
| 5 | 2026-10-02 | PARIS CROCOS FC | 主 | 3–2 | WIN | 联赛 |
| 6 | 2026-09-25 | INTER PARISIEN | 客 | 4–1 | WIN | 联赛 |

两队均不足 10 场，未补造数据。不足 5 场的行为另有基于真实 DOM 缩减为一场的单元测试。

## D. 测试与结果

Java 17，JUnit 5、AssertJ、Mockito、Spring MockRestServiceServer。测试不连接真实 FLA。
新增 53 项测试（解析器 31、客户端/筛选 22），覆盖主客场比分、胜平负、0–0、特殊名称、
未来比赛、跨年、缺失比分/日期/对手、未知状态、空日历、重复及冲突、结构变更、错误页面身份、
非法 ID/limit、HTTP 200/404/500/503/302、超时和其他 I/O 错误、最近 5/10、不足数量及倒序。

执行命令（仓库根目录；路径对应本机缓存的 Mockito 版本）：

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home \
mvn -o -pl match-service \
  -DargLine=-javaagent:/Users/yukhoi/.m2/repository/org/mockito/mockito-core/5.20.0/mockito-core-5.20.0.jar \
  -Dtest=FlaTeamMatchesHtmlParserTest,FlaTeamMatchesClientTest,FlaClientImplTest,FlaStandingsHtmlParserTest,FlaSyncServiceImplTest,FlaStandingsSnapshotServiceImplTest \
  test

JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home \
mvn -o -pl match-service \
  -DargLine=-javaagent:/Users/yukhoi/.m2/repository/org/mockito/mockito-core/5.20.0/mockito-core-5.20.0.jar \
  test
```

| 验证 | 总数 | 通过 | 失败 | 错误 | 跳过 |
| --- | ---: | ---: | ---: | ---: | ---: |
| 定向：新增 + 积分榜 + 同步 + Snapshot | 134 | 134 | 0 | 0 | 0 |
| Match Service 全量 | 328 | 327 | 1 | 0 | 0 |
| 未修改 HEAD 副本复现失败项 | 1 | 0 | 1 | 0 | 0 |

唯一失败：`OpponentAnalysisMetricsControllerTest.exposesOpenApiContract:199`，期望标签
`Opponent Analysis Metrics`，实际为 `Opponent Analysis`。通过 `git archive HEAD` 导出未修改代码
到 `/tmp/fla-team-matches-baseline`，以相同 Java/Mockito 参数运行
`-Dtest=OpponentAnalysisMetricsControllerTest#exposesOpenApiContract`，复现相同失败，确认与本次修改无关。

首次不带 `-javaagent` 的定向命令因沙箱禁止 Mockito 自附加而出现 22 个初始化错误；显式加载
现有 Mockito agent 后全部通过，未因此修改 Maven 配置或测试行为。主代码和测试均以 release 17 编译。

真实网络验证与离线测试分开：Phase 1 已用现有生产 HTTP 配置请求两队页面及积分榜，并用 Jsoup
确认结构/ID；近期列表则通过 committed fixture 的 mock HTTP 响应调用真实客户端实现取得。
没有执行数据库同步、写入 Snapshot 或调用 AI。

## E. 后续集成建议（本次未实施）

1. 在 `MatchOpponentMetricsServiceImpl` 已取得双方 FlaTeamMapping 后，按两个 flaTeamId 获取
   比赛数据。首先验证默认球队页赛季与 mapping 的 flaSaisonId 一致；当前没有已验证的历史赛季
   选择方式，不能把“当前赛季”记录静默用于任意历史 mapping。历史赛前分析还应按目标比赛时间
   截止，避免纳入目标比赛之后的结果。
2. 新建独立比赛数据来源/缓存策略，明确 fetchedAt、来源球队/赛季、payload hash、实际可用场次和
   失败状态；不要将比赛 JSON 写进现有积分榜 Snapshot 类型或覆盖其缓存。后续 AI 报告缓存的来源
   标识也应包含比赛数据版本，避免只依据 standings hash 复用旧报告。
3. 先选择需要的赛事范围，再排序和截取 5/10。若 recentFive/recentTen 要反映联赛状态，只使用
   同一联赛；63 当前仅有 4 场已完成联赛，不能混入 3 场杯赛补足。赛事名称保留可供筛选，但页面
   没有赛事 ID，自动匹配内部联赛前还需要明确、可验证的名称/上下文规则。
4. 让 `OpponentMetricsCalculator` 通过新增输入参数或单独近期指标输入接收这些独立比赛 DTO，
   保持纯计算、不负责 HTTP。按 flaTeamId 选择本队进球/失球和 `resultFor`，将 WIN/DRAW/LOSS
   映射为现有规范 `won/draw/lost`，生成最新到最早的 recentFive/recentTen 及胜平负数量/连胜状态。
5. 窗口胜率 = 窗口胜场 / 实际有效场次；窗口进球、失球分别累计本队视角比分，可另算场均和净胜球。
   两队使用相同赛事范围、截止时间和窗口规则再比较近期胜率、进失球及结果序列；保留实际样本量，
   空样本表示不可用，避免伪装成 0% 胜率。新的近期进失球字段如需公开，另行审查 API 合约。
6. 球队页失败应作为明确的来源不可用状态处理；不能把网络失败转换成“最近没有比赛”。先完成来源
   语义及缓存设计，再决定是否让已有指标部分降级，并为新组合路径补充集成测试。
