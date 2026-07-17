# API 鎺ュ彛瑙勮寖

> 缁熶竴鍏ュ彛锛欸ateway `http://127.0.0.1:8080`
> 鍓嶇寮€鍙戯細閮ㄥ垎 API 璧?Vite 浠ｇ悊锛宍/api/**` 杞?Gateway锛宍/music/**` 鐩磋繛闊充箰鏈嶅姟浠ｇ悊銆?
## 閫氱敤绾﹀畾

- 璇锋眰鏍煎紡锛歚application/json; charset=utf-8`
- 涓氬姟鍝嶅簲涓嶅畬鍏ㄧ粺涓€锛歚module-music` 浣跨敤 `{ code, msg, data }`锛宍module-chat` 鍜?`module-rec` 澶氭暟鎺ュ彛鐩存帴杩斿洖瀵硅薄/鏁扮粍銆?- 褰撳墠榛樿鐢ㄦ埛锛氬墠绔ぇ澶氫娇鐢?`userId=1`銆?- 褰撳墠 `module-user` 鍙湁鍋ュ悍妫€鏌ワ紝鐧诲綍/JWT 鐩稿叧鎺ュ彛浠嶅緟闃熷憳D瀹炵幇銆?
## 涓€銆佸璇濇湇鍔?`/chat`锛堥槦鍛楢锛岀鍙?8081锛?
| 鏂规硶 | 璺緞 | 璇存槑 | 鐘舵€?|
|:--|:--|:--|:--:|
| `GET` | `/chat/hello` | 鍋ュ悍妫€鏌?| 宸插疄鐜?|
| `POST` | `/chat/send` | REST 瀵硅瘽鍙戦€?| 宸插疄鐜?|
| `GET` | `/chat/history?userId=1` | 鏈€杩戝璇濆巻鍙诧紝榛樿 10 鏉?| 宸插疄鐜?|
| `GET` | `/chat/weather?city=濞佹捣` | 鏌ヨ澶╂皵锛屽煄甯備篃鍙紶缁忕含搴?`lon,lat` | 宸插疄鐜?|
| `WS` | `/chat/ws?userId=1` | WebSocket 瀹炴椂瀵硅瘽 | 宸插疄鐜?|

### `POST /chat/send`

璇锋眰锛?
```json
{
  "userId": 1,
  "content": "濞佹捣鐜板湪澶╂皵鎬庝箞鏍凤紝閫傚悎鍚粈涔堟瓕",
  "context": {
    "location": {
      "city": "濞佹捣甯?,
      "latitude": 37.5,
      "longitude": 122.1,
      "source": "browser_geolocation"
    }
  }
}
```

鍝嶅簲锛?
```json
{
  "reply": {
    "role": "dj",
    "text": "濞佹捣鐜板湪鍋忔疆婀匡紝閫傚悎鍚竴鐐硅垝缂撲絾涓嶅お娌夌殑姝屻€?,
    "time": "14:30"
  },
  "songs": ["姝屾洸A - 姝屾墜A"],
  "selectedSongs": [
    {
      "id": "123",
      "title": "姝屾洸A",
      "artist": "姝屾墜A",
      "source": "NETEASE_SEARCH",
      "label": "姝屾洸A - 姝屾墜A"
    }
  ],
  "toolCalls": [
    {
      "name": "weather.now",
      "purpose": "鑾峰彇鍩庡競澶╂皵",
      "status": "ok",
      "summary": "濞佹捣 灏忛洦 24掳锛屾箍搴?86%锛岄鍚?涓滃崡椋?,
      "count": 0
    }
  ],
  "clientToolRequests": []
}
```

濡傛灉鍚庣鍒ゆ柇蹇呴』鐢辨祻瑙堝櫒鎵ц瀹氫綅宸ュ叿锛屼細杩斿洖锛?
```json
{
  "reply": {
    "role": "tool",
    "text": "",
    "time": "14:30"
  },
  "songs": [],
  "selectedSongs": [],
  "toolCalls": [],
  "clientToolRequests": [
    {
      "id": "location-current",
      "name": "location.current",
      "purpose": "鑾峰彇娴忚鍣ㄥ綋鍓嶄綅缃?
    }
  ]
}
```

### WebSocket 娑堟伅

瀹㈡埛绔彂閫侊細

```json
{
  "type": "message",
  "userId": 1,
  "content": "鎴戣繖閲屽ぉ姘旈€傚悎鍚粈涔?,
  "context": {
    "location": {
      "city": "濞佹捣甯?,
      "latitude": 37.5,
      "longitude": 122.1,
      "source": "browser_geolocation"
    }
  }
}
```

鏈嶅姟绔櫘閫氬洖澶嶏細

```json
{
  "type": "reply",
  "content": "鐜板湪杩欎釜澶╂皵閫傚悎鍚竴鐐规竻鐖姐€佹斁鏉剧殑姝屻€?,
  "songs": ["姝屾洸A - 姝屾墜A"],
  "selectedSongs": [
    {
      "id": "123",
      "title": "姝屾洸A",
      "artist": "姝屾墜A",
      "source": "NETEASE_SEARCH",
      "label": "姝屾洸A - 姝屾墜A"
    }
  ],
  "time": "14:30",
  "toolCalls": []
}
```

鏈嶅姟绔姹傚鎴风宸ュ叿锛?
```json
{
  "type": "tool_request",
  "content": "鎴戣繖閲屽ぉ姘旀€庝箞鏍?,
  "clientToolRequests": [
    {
      "id": "location-current",
      "name": "location.current",
      "purpose": "鑾峰彇娴忚鍣ㄥ綋鍓嶄綅缃?
    }
  ]
}
```

### 瀵硅瘽鍙敤宸ュ叿

| 宸ュ叿 | 鎵ц绔?| 鏁版嵁鏉ユ簮 | 鐢ㄩ€?|
|:--|:--|:--|:--|
| `location.current` | 鍓嶇 | Browser Geolocation | 鑾峰彇鐢ㄦ埛褰撳墠浣嶇疆锛岄渶瑕佹祻瑙堝櫒鎺堟潈 |
| `time.current` | 鍚庣 | Java 鏈湴鏃堕棿 | 褰撳墠鏃ユ湡銆佹椂闂淬€佹槦鏈熴€佹椂鍖?|
| `weather.now` | 鍚庣 | 鍜岄澶╂皵 API | 瀹炴椂澶╂皵銆佷綋鎰熴€佹箍搴︺€侀銆侀檷姘淬€佹皵鍘嬨€佽兘瑙佸害 |
| `music.search` | 鍚庣 | `module-music` 鏈湴搴?| 鎸夋瓕鍚嶃€佹瓕鎵嬨€佹祦娲俱€佸叧閿瘝鎼滅储 |
| `music.catalog` | 鍚庣 | `module-music` 鏈湴搴?| 鑾峰彇鍊欓€夋睜锛岃 AI 浠庣湡瀹炴瓕鏇蹭腑閫夋嫨 |
| `music.neteaseSearch` | 鍚庣 | `module-music` + 缃戞槗浜戜唬鐞?| 澶栭儴缃戞槗浜戞悳绱?|
| `rec.daily` | 鍚庣 | `module-rec` | 浠婃棩鎺ㄨ崘 |
| `rec.hot` | 鍚庣 | `module-rec` + Redis | 鐑棬姒滃崟 |
| `rec.preferences` | 鍚庣 | `module-rec` | 鐢ㄦ埛鍋忓ソ鏍囩 |

### `GET /chat/weather`

绀轰緥锛?
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

`source=real` 琛ㄧず鐪熷疄鍜岄澶╂皵锛沗source=demo` 琛ㄧず鏄惧紡婕旂ず闄嶇骇锛屽師鍥犵湅 `message`銆?
## 浜屻€侀煶涔愭湇鍔?`/music`锛堥槦鍛楤锛岀鍙?8082锛?
`module-music` 杩斿洖缁熶竴 `Result<T>`锛?
```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

### 鍋ュ悍妫€鏌?
| 鏂规硶 | 璺緞 | 璇存槑 | 鐘舵€?|
|:--|:--|:--|:--:|
| `GET` | `/music/hello` | 鍋ュ悍妫€鏌?| 宸插疄鐜?|

### 姝屾洸

| 鏂规硶 | 璺緞 | 璇存槑 | 鐘舵€?|
|:--|:--|:--|:--:|
| `GET` | `/music/song/list?page=1&size=20&genre=鎽囨粴` | 鍒嗛〉姝屾洸鍒楄〃锛屽彲鎸夋祦娲剧瓫閫?| 宸插疄鐜?|
| `GET` | `/music/song/{id}` | 姝屾洸璇︽儏锛屽苟澧炲姞鎾斁娆℃暟 | 宸插疄鐜?|
| `GET` | `/music/song/search?kw=鍛ㄦ澃浼?page=1&size=20` | 鏈湴搴撳叧閿瘝鎼滅储 | 宸插疄鐜?|
| `GET` | `/music/song/genres` | 鑾峰彇鍏ㄩ儴娴佹淳 | 宸插疄鐜?|
| `PUT` | `/music/song/{id}/lyric` | 淇濆瓨姝岃瘝 | 宸插疄鐜?|
| `POST` | `/music/song/{id}/analyze-emotion` | 鎵嬪姩瑙﹀彂鎯呯华鍒嗘瀽 | 宸插疄鐜?|

### 姝屽崟

| 鏂规硶 | 璺緞 | 璇存槑 | 鐘舵€?|
|:--|:--|:--|:--:|
| `GET` | `/music/playlist/list?userId=1` | 鐢ㄦ埛姝屽崟鍒楄〃 | 宸插疄鐜?|
| `GET` | `/music/playlist/{id}` | 姝屽崟璇︽儏 | 宸插疄鐜?|
| `GET` | `/music/playlist/{id}/songs` | 姝屽崟姝屾洸 | 宸插疄鐜?|
| `POST` | `/music/playlist` | 鍒涘缓姝屽崟 `{ name, description, userId }` | 宸插疄鐜?|
| `PUT` | `/music/playlist/{id}` | 鏇存柊姝屽崟 `{ name, description }` | 宸插疄鐜?|
| `DELETE` | `/music/playlist/{id}` | 鍒犻櫎姝屽崟 | 宸插疄鐜?|
| `POST` | `/music/playlist/{id}/song/{songId}` | 娣诲姞姝屾洸鍒版瓕鍗?| 宸插疄鐜?|
| `DELETE` | `/music/playlist/{id}/song/{songId}` | 浠庢瓕鍗曠Щ闄ゆ瓕鏇?| 宸插疄鐜?|
| `PUT` | `/music/playlist/{id}/sort` | 姝屽崟鎺掑簭 `{ songIds: [] }` | 宸插疄鐜?|
| `POST` | `/music/playlist/import` | 寮傛瀵煎叆姝屽崟 | 宸插疄鐜?|
| `GET` | `/music/playlist/import/status/{taskId}` | 瀵煎叆鐘舵€侊紝鐩墠绠€鍖栬繑鍥?completed | 宸插疄鐜?|

瀵煎叆璇锋眰锛?
```json
{
  "name": "鎴戠殑缃戞槗浜戞瓕鍗?,
  "content": "姝屽崟閾炬帴鎴栨瓕鍗曞唴瀹?,
  "userId": 1
}
```

### 鏀惰棌涓庡巻鍙?
| 鏂规硶 | 璺緞 | 璇存槑 | 鐘舵€?|
|:--|:--|:--|:--:|
| `GET` | `/music/favorite/list?userId=1` | 鏀惰棌姝屾洸鍒楄〃 | 宸插疄鐜?|
| `POST` | `/music/favorite/{songId}?userId=1` | 鏀惰棌姝屾洸 | 宸插疄鐜?|
| `DELETE` | `/music/favorite/{songId}?userId=1` | 鍙栨秷鏀惰棌 | 宸插疄鐜?|
| `GET` | `/music/favorite/check/{songId}?userId=1` | 妫€鏌ユ槸鍚﹀凡鏀惰棌 | 宸插疄鐜?|
| `GET` | `/music/history/list?userId=1` | 鎾斁鍘嗗彶 | 宸插疄鐜?|
| `POST` | `/music/history` | 璁板綍鎾斁 `{ userId, songId }` | 宸插疄鐜?|

### 缃戞槗浜戜唬鐞?
杩欎簺鎺ュ彛渚濊禆鏈満 `NeteaseCloudMusicApi:3000`銆?
| 鏂规硶 | 璺緞 | 璇存槑 | 鐘舵€?|
|:--|:--|:--|:--:|
| `GET` | `/music/netease/ping` | 妫€鏌ョ綉鏄撲簯浠ｇ悊鏄惁鍙敤 | 宸插疄鐜?|
| `GET` | `/music/netease/search?keywords=鍛ㄦ澃浼?limit=30` | 缃戞槗浜戞悳绱?| 宸插疄鐜?|
| `GET` | `/music/netease/url?id=姝屾洸ID` | 鑾峰彇鎾斁 URL锛屽闊宠川 fallback | 宸插疄鐜?|
| `GET` | `/music/netease/detail?ids=1,2` | 姝屾洸璇︽儏 | 宸插疄鐜?|
| `GET` | `/music/netease/lyric?id=姝屾洸ID` | 姝岃瘝 | 宸插疄鐜?|
| `GET` | `/music/netease/lyric/batch?ids=1,2` | 鎵归噺姝岃瘝 | 宸插疄鐜?|
| `GET` | `/music/netease/playlist?id=姝屽崟ID` | 姝屽崟璇︽儏涓庢瓕鏇插垪琛?| 宸插疄鐜?|
| `GET` | `/music/netease/cover?url=鍥剧墖URL` | 灏侀潰鍥剧墖浠ｇ悊 | 宸插疄鐜?|

### 鎯呯华鍒嗘瀽涓庣敤鎴峰搧鍛?
| 鏂规硶 | 璺緞 | 璇存槑 | 鐘舵€?|
|:--|:--|:--|:--:|
| `GET` | `/music/emotion/{songId}` | 鑾峰彇姝屾洸鎯呯华鐢诲儚锛屽繀瑕佹椂鑷姩鍒嗘瀽 | 宸插疄鐜?|
| `POST` | `/music/emotion/analyze/{songId}` | 瑙﹀彂鍗曢鎯呯华鍒嗘瀽 | 宸插疄鐜?|
| `POST` | `/music/emotion/batch/{playlistId}` | 寮傛鎵归噺鍒嗘瀽姝屽崟 | 宸插疄鐜?|
| `GET` | `/music/emotion/search?tag=娓╂殩娌绘剤` | 鎸夋儏缁爣绛炬悳姝?| 宸插疄鐜?|
| `GET` | `/music/emotion/playlist/{playlistId}/overview` | 姝屽崟鎯呯华鎬昏 | 宸插疄鐜?|
| `GET` | `/music/taste/{userId}` | 鐢ㄦ埛鎯呯华鍝佸懗鐢诲儚 | 宸插疄鐜?|
| `GET` | `/music/taste/refresh/{userId}` | 鍒锋柊鍝佸懗鐢诲儚 | 宸插疄鐜?|

## 涓夈€佹帹鑽愭湇鍔?`/rec`锛堥槦鍛楥锛岀鍙?8083锛?
| 鏂规硶 | 璺緞 | 璇存槑 | 鐘舵€?|
|:--|:--|:--|:--:|
| `GET` | `/rec/hello` | 鍋ュ悍妫€鏌?| 宸插疄鐜?|
| `GET` | `/rec/hot` | 鐑棬姒滃崟 TOP10锛屾潵鑷?Redis ZSET | 宸插疄鐜?|
| `GET` | `/rec/daily?userId=1` | 浠婃棩鎺ㄨ崘锛屾潵鑷?`daily_recommend` | 宸插疄鐜?|
| `GET` | `/rec/similar?songId=1` | 鐩镐技姝屾洸鎺ㄨ崘 | 宸插疄鐜?|
| `POST` | `/rec/behavior` | 涓婃姤鐢ㄦ埛琛屼负 | 宸插疄鐜?|
| `GET` | `/rec/preferences?userId=1` | 鐢ㄦ埛鍋忓ソ鏍囩 | 宸插疄鐜?|

琛屼负涓婃姤锛?
```json
{
  "userId": 1,
  "songId": 10,
  "action": "play"
}
```

`action` 鏀寔锛?
| action | 鍚箟 | 鐑棬姒滃姞鍒?|
|:--|:--|:--:|
| `play` | 鎾斁 | +1 |
| `like` | 鏀惰棌 | +3 |
| `share` | 鍒嗕韩 | +2 |
| `skip` | 璺宠繃 | +0 |

鐩镐技鎺ㄨ崘绛栫暐锛?
1. 閫氳繃 Feign 璋?music 鑾峰彇姝屾洸娴佹淳锛屼紭鍏堟帹鑽愬悓娴佹淳銆?2. 鍚屾祦娲句负绌烘椂锛屾寜鍚屾瓕鎵嬫悳绱€?3. 鍐嶅洖閫€鍒扮敤鎴疯涓哄崗鍚岃繃婊ゃ€?
## 鍥涖€佺敤鎴锋湇鍔?`/user`锛堥槦鍛楧锛岀鍙?8084锛?
| 鏂规硶 | 璺緞 | 璇存槑 | 鐘舵€?|
|:--|:--|:--|:--:|
| `GET` | `/user/hello` | 鍋ュ悍妫€鏌?| 宸插疄鐜?|
| `POST` | `/user/register` | 娉ㄥ唽 | 鏈疄鐜?|
| `POST` | `/user/login` | 鐧诲綍/JWT | 鏈疄鐜?|
| `GET` | `/user/info` | 鐢ㄦ埛璧勬枡 | 鏈疄鐜?|
| `PUT` | `/user/info` | 淇敼璧勬枡 | 鏈疄鐜?|
| `GET` | `/user/history` | 鐢ㄦ埛鍘嗗彶 | 鏈疄鐜?|

褰撳墠鍓嶇 `UserBar.vue` 鍙湁鐧诲綍寮圭獥鍗犱綅鍜?`/user/hello` 鑱旈€氭€ф鏌ャ€傛敹钘忓拰鎾斁鍘嗗彶宸插厛鐢?`module-music` 鎻愪緵銆?
## 浜斻€佸唴閮ㄨ皟鐢ㄥ叧绯?
| 璋冪敤鏂?| 琚皟鐢ㄦ柟 | 鎺ュ彛 | 鐢ㄩ€?|
|:--|:--|:--|:--|
| `module-chat` | `module-music` | `/music/song/search`銆乣/music/song/list`銆乣/music/netease/search` | AI 瀵硅瘽鏌ユ壘鐪熷疄姝屾洸鍊欓€?|
| `module-chat` | `module-rec` | `/rec/daily`銆乣/rec/hot`銆乣/rec/preferences` | AI 瀵硅瘽鑾峰彇鎺ㄨ崘鍜屽亸濂?|
| `module-rec` | `module-music` | `/music/song/{id}`銆乣/music/song/search` | 鎺ㄨ崘妯″潡琛ュ叏姝屾洸淇℃伅鍜屾煡鍚屾祦娲炬瓕鏇?|
| 鍓嶇 | `module-chat` | `/chat/ws`銆乣/chat/send` | 瀵硅瘽銆佸伐鍏疯皟鐢ㄣ€佸畾浣嶄笂涓嬫枃 |
| 鍓嶇 | `module-music` | `/music/**` | 鎾斁鍣ㄣ€佹瓕鍗曘€佺綉鏄撲簯銆佹儏缁垎鏋?|
| 鍓嶇 | `module-rec` | `/rec/**` | 鎺ㄨ崘闈㈡澘 |
