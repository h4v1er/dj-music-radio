# 闃熷憳C 寮€鍙戞棩蹇?鈥?姣忔棩鎺ㄨ崘妯″潡

> 濮撳悕锛歭1nky777  鍒嗘敮锛歚dev-rec`  鍚庣锛歮odule-rec (:8083)

---

## 璐熻矗鍐呭
- 馃搳 鎺ㄨ崘闈㈡澘锛圧ecPanel.vue锛?
- 鐑棬姒滃崟锛圧edis ZSET锛?
- 涓€у寲鎺ㄨ崘绠楁硶
- 鐢ㄦ埛琛屼负鍒嗘瀽
- 姣忔棩鎺ㄨ崘瀹氭椂浠诲姟锛園Scheduled锛?
- RabbitMQ 鎺ㄨ崘鎺ㄩ€?

---

## 绗?澶╋紙鏃ユ湡锛?026-07-16锛?- [x] 浠?`master` 鍒涘缓闃熷憳C寮€鍙戝垎鏀?`dev-rec`
- [x] 鎼缓 module-rec 鍚庣妗嗘灦锛屽畬鎴?`GET /rec/hello` 鏈嶅姟鍋ュ悍妫€鏌?
- [x] 瀹屾垚 `GET /rec/hot` 鐑棬姒滃崟鎺ュ彛锛孯edis ZSET 鍙嶅悜鎺掑簭杩斿洖 TOP10
- [x] 瀹屾垚 `POST /rec/behavior` 鐢ㄦ埛琛屼负涓婃姤鎺ュ彛锛岃涓鸿鍒嗭紙play+1/like+3/share+2锛夊啓搴撳悓姝ユ洿鏂?Redis
- [x] 瀹屾垚 `GET /rec/similar?songId=` 鐩镐技姝屾洸鎺ㄨ崘锛屽洓灞傜瓥鐣ワ紙鍚屾祦娲锯啋鍚屾瓕鎵嬧啋琛屼负鍗忓悓杩囨护鈫掔┖鍒楄〃锛?
- [x] 瀹屾垚 `GET /rec/daily?userId=` 浠婃棩鎺ㄨ崘鏌ヨ鎺ュ彛
- [x] 瀹屾垚 `GET /rec/preferences?userId=` 鐢ㄦ埛鍋忓ソ鏍囩鎺ュ彛锛屼粠琛屼负鏁版嵁缁熻娴佹淳棰戞
- [x] 鏂板 `MusicFeignClient` 閫氳繃 OpenFeign 璋冪敤 module-music锛屽惈 `MusicFeignFallback` 鍥為€€宸ュ巶
- [x] 鏂板 `SongDTO` / `ResultDTO` 鏁版嵁浼犺緭瀵硅薄锛屽尮閰?API 瑙勮寖
- [x] 鍚庣 enrich 鐑棬姒滃崟鍜屾瘡鏃ユ帹鑽愭暟鎹紝Feign 琛ュ叏姝屾洸鏍囬姝屾墜锛屽け璐ヨ嚜鍔ㄩ檷绾?
- [x] `UserBehaviorMapper` 鏂板 `findSimilarByBehavior` 鍗忓悓杩囨护 SQL
- [x] 鏂板 `RecommendScheduler` 瀹氭椂浠诲姟锛宍@Scheduled` 姣忓ぉ鍑屾櫒 2:00 鐢熸垚褰撴棩鎺ㄨ崘
- [x] 鏂板 RabbitMQ 閰嶇疆 `RabbitMQConfig` + `RecNotificationProducer`锛屾帹鑽愮敓鎴愬悗鍙戦€侀€氱煡娑堟伅
- [x] `RecPanel.vue` 鎺ュ叆鐪熷疄鍚庣 API锛岀儹闂ㄦ鍗?/ 浠婃棩鎺ㄨ崘 / 鍋忓ソ鏍囩涓夋€佸鐞?
- [x] 鍒涘缓鏁版嵁搴撹〃 `user_behavior` 鍜?`daily_recommend`
- [x] 瀹夎骞堕厤缃?Redis 3.0.504銆丯acos 2.4.3銆丒rlang OTP + RabbitMQ Server 4.1.3
- [x] 楠岃瘉閫氳繃锛歁aven 缂栬瘧鎴愬姛锛屾ā鍧楁甯稿惎鍔ㄥ苟娉ㄥ唽鍒?Nacos锛宍/rec/hello` `/rec/hot` `/rec/preferences` 鎺ュ彛姝ｅ父杩斿洖

## 褰撳墠娉ㄦ剰鐐?
- [ ] 闇€瑕佽ˉ鍏?`module-rec` 鍒濆鍖?SQL 鏂囦欢锛屽綋鍓嶆柊浜鸿繍琛屾墜鍐屼腑鍏堣褰曚簡寤鸿〃 SQL銆?- [ ] 鏂扮敤鎴锋病鏈夎涓烘暟鎹椂锛岀儹闂ㄦ鍜屾瘡鏃ユ帹鑽愬彲鑳戒负绌猴紝闇€瑕佸噯澶囨紨绀鸿涓烘暟鎹垨鍐峰惎鍔ㄧ瓥鐣ャ€?- [ ] 鍚庣画鎺ュ叆 user 妯″潡鍚庯紝灏嗛粯璁?`userId` 鏀逛负鐪熷疄鐧诲綍鐢ㄦ埛銆?
## 绗?澶╋紙鏃ユ湡锛歘_____锛?
- [ ]

## 绗?澶╋紙鏃ユ湡锛歘_____锛?
- [ ]

## 绗?澶╋紙鏃ユ湡锛歘_____锛?
- [ ]
