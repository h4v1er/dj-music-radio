# module-music 杩愯渚濊禆璇存槑

> 鑼冨洿锛氶槦鍛楤 `module-music`銆乣MusicPanel.vue`銆佹挱鏀惧櫒銆佹瓕鍗曘€佺綉鏄撲簯銆佹儏缁垎鏋愩€?
> 褰撳墠鐘舵€侊細鏍稿績鎺ュ彛宸插疄鐜帮紝浣嗗畬鏁翠綋楠屼緷璧栨湰鏈虹綉鏄撲簯浠ｇ悊銆丮ySQL 鏁版嵁鍜?DeepSeek key銆?
## 1. 渚濊禆鎬昏

| 鍔熻兘 | 渚濊禆 | 绔彛/閰嶇疆 | 璇存槑 |
|:--|:--|:--|:--|
| 闊充箰鏈嶅姟 | `module-music` | 8082 | Spring Boot 鏈嶅姟 |
| 鏈湴姝屾洸/姝屽崟/鏀惰棌/鍘嗗彶 | MySQL `dj_music_radio` | 3306 | 闇€瑕佹墽琛屽垵濮嬪寲 SQL |
| 缃戞槗浜戞悳绱?鎾斁/姝岃瘝/姝屽崟 | `NeteaseCloudMusicApi` | 3000 | 姣忓彴鏈哄櫒鏈満鍚姩 |
| 姝屽崟瀵煎叆寮傛澶勭悊 | RabbitMQ | 5672 | 瀵煎叆浠诲姟娑堟伅 |
| 鏈嶅姟娉ㄥ唽鍙戠幇 | Nacos | 8848 | Gateway/Feign 璋冪敤 |
| AI 鎯呯华鍒嗘瀽 | DeepSeek | `DEEPSEEK_API_KEY` | 涓嶉厤缃椂鍥為€€鍏抽敭璇嶅垎鏋?|

## 2. 鏁版嵁搴撳垵濮嬪寲

鏁版嵁搴擄細

```sql
CREATE DATABASE IF NOT EXISTS dj_music_radio DEFAULT CHARACTER SET utf8mb4;
```

鎵ц锛?
```text
module-music/src/main/resources/init.sql
module-music/src/main/resources/emotion_schema.sql
module-music/src/main/resources/emotion_keywords.sql
```

鏍稿績琛細

| 琛?| 浣滅敤 |
|:--|:--|
| `song` | 姝屾洸搴擄紝鏀寔鏈湴瀵煎叆鍜岀綉鏄撲簯鏉ユ簮 |
| `playlist` | 鐢ㄦ埛姝屽崟 |
| `playlist_song` | 姝屽崟姝屾洸鍏宠仈 |
| `user_favorite` | 鏀惰棌 |
| `play_history` | 鎾斁鍘嗗彶 |
| `song_emotion` | 姝屾洸鎯呯华鐢诲儚 |
| `emotion_keyword` | 鎯呯华璇嶅吀 |
| `user_taste` | 鐢ㄦ埛鍝佸懗鐢诲儚 |

`init.sql` 褰撳墠宸茬Щ闄ら缃祴璇曟瓕鏇诧紝鎵€浠ユ柊鏁版嵁搴撶殑 `song` 琛ㄤ负绌烘槸姝ｅ父鐨勩€傞渶瑕侀€氳繃缃戞槗浜戞瓕鍗曞鍏ユ垨鍚庣画鏁版嵁鑴氭湰瀵煎叆鐪熷疄姝屾洸銆?
## 3. 缃戞槗浜?API 浠ｇ悊

鍚庣 `NeteaseController` 鍥哄畾璋冪敤锛?
```text
http://localhost:3000
```

鍥犳姣忎釜寮€鍙戣€呮湰鏈洪兘瑕佸惎鍔?`NeteaseCloudMusicApi`锛屽惁鍒欙細

- 缃戞槗浜戞悳绱㈠け璐?- 鎾斁 URL 鑾峰彇澶辫触
- 姝岃瘝鑾峰彇澶辫触
- 姝屽崟瀵煎叆鏃犳硶鎷夊埌缃戞槗浜戣鎯?
鍚姩锛?
```powershell
powershell -ExecutionPolicy Bypass -File scripts/netease/start-netease-api.ps1
```

鍙畨瑁呬笉鍚姩锛?
```powershell
powershell -ExecutionPolicy Bypass -File scripts/netease/start-netease-api.ps1 -InstallOnly
```

鑴氭湰浼氭妸渚濊禆鏀惧埌锛?
```text
.runtime/netease-api/
```

`.runtime/` 涓嶆彁浜?Git銆?
楠岃瘉锛?
```text
http://127.0.0.1:8080/music/netease/ping
http://127.0.0.1:8080/music/netease/search?keywords=鍛ㄦ澃浼?limit=3
```

娉ㄦ剰锛氱綉鏄撲簯杩斿洖鎾斁 URL 鍙楃増鏉冦€乂IP銆佸湴鍖恒€佺櫥褰曟€佸奖鍝嶃€傛湁浜涙瓕娌℃湁鍙挱鏀?URL 鏄钩鍙伴檺鍒讹紝涓嶄竴瀹氭槸椤圭洰閿欒銆?
## 4. DeepSeek 鎯呯华鍒嗘瀽

`module-music` 鎯呯华鍒嗘瀽搴旈€氳繃鐜鍙橀噺璇诲彇 key锛?
```yaml
deepseek:
  api:
    key: ${DEEPSEEK_API_KEY:}
    url: ${DEEPSEEK_API_URL:https://api.deepseek.com/v1/chat/completions}
  model: ${DEEPSEEK_MODEL:deepseek-chat}
```

鏈満閰嶇疆锛?
```powershell
[Environment]::SetEnvironmentVariable("DEEPSEEK_API_KEY", "<浣犵殑DeepSeek Key>", "User")
```

涓嶉厤缃椂锛屾儏缁垎鏋愪細鍥為€€鍒板叧閿瘝璇嶅吀鍜岃鍒欒绠楋紱瑕侀獙鏀垛€滅湡瀹?AI 鎯呯华鍒嗘瀽鈥濓紝闇€瑕佺湅鏃ュ織涓嚭鐜?DeepSeek 璋冪敤鎴愬姛璁板綍銆?
## 5. 宸插疄鐜版帴鍙?
璇﹁ [API鎺ュ彛瑙勮寖.md](./API鎺ュ彛瑙勮寖.md)銆傛ā鍧椾富瑕佽兘鍔涳細

- 姝屾洸鍒嗛〉銆佽鎯呫€佹悳绱€佹祦娲惧垪琛ㄣ€?- 姝岃瘝淇濆瓨銆佸崟鏇叉儏缁垎鏋愩€?- 姝屽崟鍒涘缓銆佷慨鏀广€佸垹闄ゃ€佹坊鍔?绉婚櫎姝屾洸銆佹帓搴忋€?- 姝屽崟寮傛瀵煎叆锛屽綋鍓嶇姸鎬佹帴鍙ｇ畝鍖栬繑鍥?completed銆?- 鏀惰棌鍒楄〃銆佹敹钘忋€佸彇娑堟敹钘忋€佹敹钘忔鏌ャ€?- 鎾斁鍘嗗彶璁板綍鍜屾煡璇€?- 缃戞槗浜戞悳绱€佹挱鏀?URL銆佽鎯呫€佹瓕璇嶃€佹瓕鍗曘€佸皝闈唬鐞嗐€?- 姝屾洸鎯呯华鐢诲儚銆佹瓕鍗曟儏缁€昏銆佹儏缁爣绛炬悳绱€佺敤鎴峰搧鍛崇敾鍍忋€?
## 6. 涓庡叾浠栨ā鍧楃殑鍏崇郴

| 璋冪敤鏂?| 璋冪敤鍐呭 | 璇存槑 |
|:--|:--|:--|
| `module-chat` | `/music/song/search`銆乣/music/song/list`銆乣/music/netease/search` | AI 瀵硅瘽鑾峰彇鐪熷疄姝屾洸鍊欓€?|
| `module-rec` | `/music/song/{id}`銆乣/music/song/search` | 鎺ㄨ崘琛ュ叏姝屾洸淇℃伅銆佹煡鍚屾祦娲?鍚屾瓕鎵?|
| 鍓嶇 `MusicPanel.vue` | `/music/**` | 鎾斁鍣ㄣ€佹瓕鍗曘€佺綉鏄撲簯銆佹儏缁垎鏋?|

## 7. 甯歌闂

| 鐜拌薄 | 鍘熷洜 | 澶勭悊 |
|:--|:--|:--|
| 缃戞槗浜戞悳绱㈠け璐?| 3000 浠ｇ悊娌″惎鍔?| 杩愯 `scripts/netease/start-netease-api.ps1` |
| 鎼滃埌姝屼絾鎾斁涓嶄簡 | VIP/鐗堟潈/鍦板尯闄愬埗锛孶RL 涓虹┖ | 鎹㈡瓕鎴栫櫥褰曟€佹柟妗堝悗缁鐞?|
| 鏈湴鎼滅储涓虹┖ | `song` 琛ㄤ负绌?| 鍏堝鍏ユ瓕鍗曟垨姝屾洸 |
| 鎯呯华鍒嗘瀽娌℃湁 AI 鏁堟灉 | 鏈厤缃?DeepSeek key | 閰嶇疆 `DEEPSEEK_API_KEY` 骞堕噸鍚?|
| 鍚姩 RabbitMQ 鎶ラ敊 | RabbitMQ 娌″惎鍔?| 鍚姩 RabbitMQ锛岀‘璁?guest/guest 鍙敤 |
| 姝屽崟瀵煎叆鐘舵€佹€绘槸 completed | 褰撳墠鐘舵€佹帴鍙ｄ负绠€鍖栧疄鐜?| 鍚庣画鍙帴 Redis 浠诲姟鐘舵€?|
