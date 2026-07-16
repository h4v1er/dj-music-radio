package org.example.music.controller;

import org.example.music.dto.Result;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;

/**
 * 网易云音乐代理 — 转发请求到本地 NeteaseCloudMusicApi (:3000)
 */
@RestController
@RequestMapping("/music/netease")
public class NeteaseController {

    private static final String NETEASE_API = "http://localhost:3000";
    private final RestTemplate rest = new RestTemplate();

    /** 搜索歌曲 */
    @GetMapping("/search")
    public Result<Object> search(@RequestParam String keywords,
                                 @RequestParam(defaultValue = "30") int limit) {
        String url = NETEASE_API + "/search?keywords=" + keywords + "&limit=" + limit;
        Map resp = rest.getForObject(url, Map.class);
        return Result.ok(resp != null ? resp.get("result") : null);
    }

    /** 获取歌曲播放URL */
    @GetMapping("/url")
    public Result<Object> getUrl(@RequestParam String id) {
        String url = NETEASE_API + "/song/url?id=" + id;
        Map resp = rest.getForObject(url, Map.class);
        return Result.ok(resp != null ? resp.get("data") : null);
    }

    /** 获取歌曲详情 */
    @GetMapping("/detail")
    public Result<Object> getDetail(@RequestParam String ids) {
        String url = NETEASE_API + "/song/detail?ids=" + ids;
        Map resp = rest.getForObject(url, Map.class);
        return Result.ok(resp != null ? resp.get("songs") : null);
    }

    /** 获取歌词 */
    @GetMapping("/lyric")
    public Result<Object> getLyric(@RequestParam String id) {
        String url = NETEASE_API + "/lyric?id=" + id;
        Map resp = rest.getForObject(url, Map.class);
        return Result.ok(resp != null ? resp : null);
    }

    /**
     * 代理网易云封面图片 — 绕过 Referer 防盗链
     * 前端请求：/music/netease/cover?url=xxx
     */
    @GetMapping("/cover")
    public void proxyCover(@RequestParam String url, HttpServletResponse response) {
        try {
            URI uri = URI.create(url);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Referer", "https://music.163.com/");
            conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

            String contentType = conn.getContentType();
            if (contentType != null) {
                response.setContentType(contentType);
            }
            response.setHeader("Cache-Control", "public, max-age=86400");

            try (InputStream in = conn.getInputStream();
                 OutputStream out = response.getOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
            }
            conn.disconnect();
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /** 健康检查 */
    @GetMapping("/ping")
    public Result<String> ping() {
        try {
            rest.getForObject(NETEASE_API + "/search?keywords=test&limit=1", Map.class);
            return Result.ok("网易云API连接正常");
        } catch (Exception e) {
            return Result.fail("网易云API不可用: " + e.getMessage());
        }
    }
}
