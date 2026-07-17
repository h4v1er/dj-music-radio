# module-chat 运行依赖说明

> 负责范围：队员A `module-chat`、`ChatPanel.vue`、`WeatherWidget.vue`

## 必要服务

| 服务 | 用途 | 端口 |
|:---|:---|:---:|
| Gateway | 前端统一访问入口 | 8080 |
| module-chat | 对话、历史、天气接口 | 8081 |
| module-music | AI 对话调用真实歌曲搜索 | 8082 |
| module-rec | AI 对话调用推荐结果 | 8083 |
| Nacos | 服务发现 | 8848 |
| MySQL | 对话历史持久化 | 3306 |

## 环境变量

| 变量 | 必填 | 说明 |
|:---|:---:|:---|
| `MYSQL_PASSWORD` | 是 | `module-chat` 连接 MySQL 使用 |
| `DEEPSEEK_API_KEY` | 否 | 配置后启用真实 AI 对话规划和回复 |
| `DEEPSEEK_API_URL` | 否 | 默认 `https://api.deepseek.com/v1/chat/completions` |
| `DEEPSEEK_MODEL` | 否 | 默认 `deepseek-chat` |
| `QWEATHER_API_KEY` | 否 | 配置后启用和风天气真实天气 |
| `QWEATHER_API_HOST` | 建议 | 和风天气控制台项目的专属 API Host |
| `QWEATHER_WEATHER_URL` | 否 | 手动覆盖实时天气接口地址 |
| `QWEATHER_GEO_URL` | 否 | 手动覆盖城市查询接口地址 |

不要把任何 API key 提交到 Git。每台电脑需要自己配置本机环境变量。

## 配置真实天气 API

1. 在和风天气控制台创建项目，拿到 `API KEY` 和项目的 `API Host`。
2. 在远端 Windows 设置用户级环境变量：

```powershell
[Environment]::SetEnvironmentVariable("QWEATHER_API_KEY", "<你的和风天气KEY>", "User")
[Environment]::SetEnvironmentVariable("QWEATHER_API_HOST", "<你的API Host>", "User")
```

`QWEATHER_API_HOST` 可以写成 `abc123xyz.re.qweatherapi.com`，也可以带 `https://`。

3. 重启 `module-chat`。如果使用计划任务启动：

```powershell
Stop-ScheduledTask -TaskName DJMusicRadio-module-chat -ErrorAction SilentlyContinue
Start-ScheduledTask -TaskName DJMusicRadio-module-chat
```

4. 验证接口：

```powershell
Invoke-RestMethod "http://127.0.0.1:8080/chat/weather?city=北京"
```

真实天气成功时应看到：

```json
{
  "source": "real",
  "message": "和风天气实时数据"
}
```

如果返回 `source=demo`，说明走了显式演示降级。常见原因：

- 没有配置 `QWEATHER_API_KEY`
- `QWEATHER_API_HOST` 写错或没有重启服务
- key 无效、接口额度不足、接口被限制
- 城市查询失败或网络访问和风天气失败

## 天气接口约定

`GET /chat/weather?city=北京`

```json
{
  "city": "北京",
  "icon": "☀️",
  "temp": "28°",
  "text": "晴",
  "greeting": "下午好，想听点什么？",
  "source": "demo",
  "obsTime": "",
  "message": "未配置 QWEATHER_API_KEY，当前使用演示天气"
}
```

字段说明：

| 字段 | 说明 |
|:---|:---|
| `source` | `real` 表示和风天气真实数据，`demo` 表示演示降级数据 |
| `obsTime` | 和风天气观测时间，演示数据为空 |
| `message` | 当前数据来源或降级原因，前端可用于提示 |

