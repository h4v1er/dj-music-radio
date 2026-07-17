# module-chat 杩愯渚濊禆璇存槑

> 鑼冨洿锛氶槦鍛楢 `module-chat`銆乣ChatPanel.vue`銆乣WeatherWidget.vue`銆乣TimeWidget.vue`銆?
> 褰撳墠瀹氫綅锛氫笉鏄浐瀹氬叧閿瘝鍥炲锛岃€屾槸 DeepSeek 瑙勫垝宸ュ叿 + 鍚庣鎵ц宸ュ叿 + DeepSeek 鐢熸垚鑷劧鍥炲銆?
## 1. 蹇呰鏈嶅姟

| 鏈嶅姟 | 鐢ㄩ€?| 绔彛 |
|:--|:--|:--:|
| Gateway | 鍓嶇缁熶竴 HTTP/WS 鍏ュ彛 | 8080 |
| module-chat | 瀵硅瘽銆佸巻鍙层€佸ぉ姘斻€佸伐鍏风紪鎺?| 8081 |
| module-music | 姝屾洸鎼滅储銆佹湰鍦板€欓€夋睜銆佺綉鏄撲簯鎼滅储 | 8082 |
| module-rec | 姣忔棩鎺ㄨ崘銆佺儹闂ㄦ銆佸亸濂芥爣绛?| 8083 |
| MySQL | `chat_history` 瀵硅瘽鍘嗗彶 | 3306 |
| Nacos | 鏈嶅姟鍙戠幇锛孎eign 璋冪敤渚濊禆 | 8848 |

## 2. 鐜鍙橀噺

| 鍙橀噺 | 蹇呭～ | 璇存槑 |
|:--|:--:|:--|
| `MYSQL_PASSWORD` | 鏄?| 杩炴帴 `mall` 鏁版嵁搴?|
| `DEEPSEEK_API_KEY` | 寤鸿 | 鍚敤鐪熷疄 AI 宸ュ叿瑙勫垝鍜岃嚜鐒跺洖澶?|
| `DEEPSEEK_API_URL` | 鍚?| 榛樿 `https://api.deepseek.com/v1/chat/completions` |
| `DEEPSEEK_MODEL` | 鍚?| 榛樿 `deepseek-chat` |
| `QWEATHER_API_KEY` | 寤鸿 | 鍚敤鍜岄澶╂皵鐪熷疄澶╂皵 |
| `QWEATHER_API_HOST` | 寤鸿 | 鍜岄澶╂皵鎺у埗鍙伴」鐩笓灞?API Host |
| `QWEATHER_WEATHER_URL` | 鍚?| 鎵嬪姩瑕嗙洊瀹炴椂澶╂皵鎺ュ彛 |
| `QWEATHER_GEO_URL` | 鍚?| 鎵嬪姩瑕嗙洊鍩庡競鏌ヨ鎺ュ彛 |

涓嶅厑璁告妸鐪熷疄 key 鎻愪氦鍒?Git銆?
## 3. 鏁版嵁搴?
`module-chat` 浣跨敤 `mall` 鏁版嵁搴撲腑鐨?`chat_history` 琛細

```text
module-chat/src/main/resources/sql/chat_history.sql
```

濡傛灉鏁版嵁搴撲笉鍙敤锛宍ChatService` 浼氱煭鏃堕棿鍥為€€鍒板唴瀛樺巻鍙诧紝浣嗚繖鍙€傚悎涓存椂寮€鍙戯紝涓嶉€傚悎浣滀负鏈€缁堟紨绀虹姸鎬併€?
## 4. AI 瀵硅瘽娴佺▼

褰撳墠瀵硅瘽涓嶆槸鈥滃厛鍐欐瑙勫垯鍐嶅妯℃澘鈥濓紝鑰屾槸锛?
```text
鐢ㄦ埛娑堟伅
  -> DeepSeek 瑙勫垝鏄惁闇€瑕佸伐鍏?  -> 鍚庣鎵ц宸ュ叿锛歮usic / rec / weather / time
  -> 濡傛灉闇€瑕佹祻瑙堝櫒瀹氫綅锛屽悗绔繑鍥?location.current 宸ュ叿璇锋眰
  -> 鍓嶇鎵ц瀹氫綅锛屽啀甯?context.location 閲嶅彂鍘熸秷鎭?  -> DeepSeek 鍩轰簬宸ュ叿缁撴灉鐢熸垚鑷劧鍥炲
  -> 杩斿洖 reply + selectedSongs + toolCalls
```

鍏抽敭鍘熷垯锛?
- 鏅€氳亰澶╀笉寮鸿鎺ㄨ崘姝屾洸銆?- 瑁稿叧閿瘝鎴栬８浜哄悕浼氫紭鍏堣拷闂敤鎴锋兂鍋氫粈涔堛€?- 姝屾洸鍙兘浠庣湡瀹炲伐鍏疯繑鍥炵殑鍊欓€夊垪琛ㄩ噷閫夛紝涓嶈 AI 缂栨瓕鍚嶃€?- 澶╂皵銆佹椂闂淬€佹帹鑽愩€佸亸濂介兘浠ュ伐鍏风粨鏋滀负鍑嗐€?- DeepSeek 涓嶅彲鐢ㄦ椂浼氳繘鍏ユ湰鍦伴檷绾э紝涓嶅簲鎶婇檷绾у綋浣溾€滅湡瀹?AI 宸叉垚鍔熲€濄€?
## 5. 宸插疄鐜板伐鍏?
| 宸ュ叿 | 鎵ц绔?| 璇存槑 |
|:--|:--|:--|
| `location.current` | 鍓嶇 | 娴忚鍣ㄥ畾浣嶏紝闇€瑕佺敤鎴锋巿鏉?|
| `time.current` | 鍚庣 | 褰撳墠鏃ユ湡銆佹椂闂淬€佹槦鏈熴€佹椂鍖?|
| `weather.now` | 鍚庣 | 鍜岄澶╂皵瀹炴椂澶╂皵 |
| `music.search` | 鍚庣 | 鏈湴姝屾洸搴撳叧閿瘝鎼滅储 |
| `music.catalog` | 鍚庣 | 鏈湴姝屾洸鍊欓€夋睜 |
| `music.neteaseSearch` | 鍚庣 | 缃戞槗浜戞悳绱紝渚濊禆 `NeteaseCloudMusicApi:3000` |
| `rec.daily` | 鍚庣 | 浠婃棩鎺ㄨ崘 |
| `rec.hot` | 鍚庣 | Redis 鐑棬姒?|
| `rec.preferences` | 鍚庣 | 鐢ㄦ埛鍋忓ソ鏍囩 |

褰撳墠娌℃湁 `web.search` 宸ュ叿銆傚悗缁鏋滆鍔犺仈缃戞悳绱紝搴旀寜宸ュ叿褰㈠紡灏佽锛屼笉瑕佹妸缃戦〉鍐呭纭杩?prompt銆?
## 6. 澶╂皵 API 閰嶇疆

鍜岄澶╂皵鏂扮増闇€瑕?API Key 鍜岄」鐩?API Host锛?
```powershell
[Environment]::SetEnvironmentVariable("QWEATHER_API_KEY", "<浣犵殑Key>", "User")
[Environment]::SetEnvironmentVariable("QWEATHER_API_HOST", "<浣犵殑Host>", "User")
```

`QWEATHER_API_HOST` 鍙互鍐欙細

```text
abc123xyz.re.qweatherapi.com
https://abc123xyz.re.qweatherapi.com
```

閰嶇疆鍚庨噸鍚?`module-chat`銆?
楠岃瘉锛?
```powershell
Invoke-RestMethod "http://127.0.0.1:8080/chat/weather?city=濞佹捣"
```

鎴愬姛鏃讹細

```json
{
  "source": "real",
  "message": "鍜岄澶╂皵瀹炴椂鏁版嵁"
}
```

濡傛灉杩斿洖 `source=demo`锛岀湅 `message` 鍒ゆ柇鍘熷洜锛屽父瑙佷负鏈厤缃?key銆乭ost 閿欒銆佹湇鍔℃湭閲嶅惎銆佹帴鍙ｉ搴︽垨缃戠粶寮傚父銆?
## 7. 澶╂皵鎺ュ彛瀛楁

`GET /chat/weather?city=濞佹捣`

```json
{
  "city": "灞变笢 濞佹捣",
  "icon": "馃導锔?,
  "temp": "24掳",
  "text": "灏忛洦",
  "greeting": "涓嬪崍濂斤紝鎯冲惉鐐逛粈涔堬紵",
  "source": "real",
  "obsTime": "2026-07-17T14:00+08:00",
  "message": "鍜岄澶╂皵瀹炴椂鏁版嵁",
  "feelsLike": "27掳",
  "windDir": "涓滃崡椋?,
  "windScale": "3",
  "windSpeed": "13",
  "humidity": "94",
  "precip": "0.0",
  "pressure": "996",
  "vis": "7",
  "cloud": "100",
  "dew": "23",
  "updateTime": "2026-07-17T14:03+08:00",
  "fxLink": "https://www.qweather.com/..."
}
```

鍓嶇 `WeatherWidget.vue` 浼氬湪椤舵爮鏄剧ず绠€鐗堝ぉ姘旓紝榧犳爣鎮诞鏄剧ず瀹屾暣瀛楁銆?
## 8. 鍓嶇瀹氫綅宸ュ叿

澶╂皵灏忛儴浠跺惎鍔ㄦ椂浼氬皾璇曟祻瑙堝櫒瀹氫綅锛?
- 鎺堟潈鎴愬姛锛氫娇鐢ㄧ粡绾害鏌ヨ澶╂皵锛屽苟鎶婂畾浣嶅煄甯傚瓨鍏?`localStorage`銆?- 鎺堟潈澶辫触锛氬洖閫€榛樿鍩庡競鍖椾含銆?- 瀵硅瘽涓敤鎴疯鈥滄垜杩欓噷/褰撳墠浣嶇疆/褰撳湴鈥濓紝鍚庣鍙姹?`location.current`锛屽墠绔墽琛屽畾浣嶅悗閲嶅彂娑堟伅銆?
瀹氫綅鏄鎴风宸ュ叿锛屼笉搴旇姣忔鏃犺剳浣滀负涓婁笅鏂囧彂閫併€傚彧鏈夊墠绔凡鏈夋湁鏁堝畾浣嶄笂涓嬫枃锛屾垨鍚庣鏄庣‘璇锋眰宸ュ叿鏃舵墠浣跨敤銆?
## 9. 楠岃瘉鍛戒护

```bash
curl http://127.0.0.1:8080/chat/hello
curl "http://127.0.0.1:8080/chat/weather?city=濞佹捣"
curl -H "Content-Type: application/json" \
  -d '{"userId":1,"content":"鐜板湪鍑犵偣锛屼粖澶╁懆鍑?}' \
  http://127.0.0.1:8080/chat/send
curl -H "Content-Type: application/json" \
  -d '{"userId":1,"content":"濞佹捣鐜板湪澶╂皵璇︾粏璇磋"}' \
  http://127.0.0.1:8080/chat/send
curl -H "Content-Type: application/json" \
  -d '{"userId":1,"content":"鎺ㄨ崘鍑犻閫傚悎涓嬮洦澶╁惉鐨勬瓕"}' \
  http://127.0.0.1:8080/chat/send
```

鐪嬪搷搴斾腑鐨?`toolCalls` 鍙互纭瀹為檯璋冪敤浜嗗摢浜涘伐鍏枫€?
## 10. 甯歌闂

| 鐜拌薄 | 鍘熷洜 | 澶勭悊 |
|:--|:--|:--|
| 鍥炲鍍忓浐瀹氭ā鏉?| 娌￠厤 DeepSeek key 鎴?API 璋冪敤澶辫触 | 閰嶇疆 `DEEPSEEK_API_KEY` 骞剁湅鍚庣鏃ュ織 |
| 澶╂皵鎬绘槸鍖椾含 | 娴忚鍣ㄥ畾浣嶆湭鎺堟潈鎴栧畾浣嶅け璐?| 鍏佽瀹氫綅锛屾垨鐩存帴闂€滃▉娴峰ぉ姘斺€?|
| 澶╂皵鏄紨绀烘暟鎹?| 鍜岄澶╂皵 key/host 涓嶅彲鐢?| 閰嶇疆鐜鍙橀噺骞堕噸鍚?chat |
| 鎺ㄨ崘姝屾洸涓虹┖ | 鏈湴搴撲负绌恒€佺綉鏄撲簯浠ｇ悊娌″惎鍔ㄣ€乺ec 娌℃暟鎹?| 瀵煎叆姝屽崟锛屽惎鍔?`NeteaseCloudMusicApi:3000`锛屼骇鐢熻涓烘暟鎹?|
| 鈥滄垜杩欓噷澶╂皵鈥濆厛娌″洖绛?| 鍚庣鍦ㄨ姹傛祻瑙堝櫒瀹氫綅宸ュ叿 | 鍓嶇鍏佽瀹氫綅鍚庝細鑷姩缁х画 |
