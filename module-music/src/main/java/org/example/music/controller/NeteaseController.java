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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
     * 获取网易云歌单详情（含歌曲列表 + 封面）
     * 前端请求：/music/netease/playlist?id=12345678
     */
    @GetMapping("/playlist")
    public Result<Object> getPlaylist(@RequestParam String id) {
        try {
            // 获取歌单基本信息 + 歌曲列表
            String url = NETEASE_API + "/playlist/detail?id=" + id;
            Map resp = rest.getForObject(url, Map.class);
            if (resp == null || resp.get("playlist") == null) {
                return Result.fail("歌单不存在或无法访问");
            }
            Map playlist = (Map) resp.get("playlist");
            // 实际 API 返回结构: { playlist: { name, coverImgUrl, tracks, tags, ... }, privileges: [...] }
            // 歌曲列表在 playlist.tracks 中
            List<Map> songs = (List<Map>) playlist.get("tracks");

            // 精简返回数据
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("id", playlist.get("id"));
            result.put("name", playlist.get("name"));
            result.put("coverImgUrl", playlist.get("coverImgUrl"));
            result.put("trackCount", playlist.get("trackCount"));
            result.put("tags", playlist.get("tags"));   // 风格标签，如 [\"华语\", \"摇滚\"]

            // 从歌单标签推断流派，用于导入时标注歌曲风格
            List<String> playlistTags = (List<String>) playlist.get("tags");
            String genreHint = (playlistTags != null && !playlistTags.isEmpty())
                ? String.join("/", playlistTags) : "网易云";

            if (songs != null) {
                List<Map<String, Object>> songList = new java.util.ArrayList<>();
                for (Map s : songs) {
                    Map<String, Object> song = new java.util.LinkedHashMap<>();
                    song.put("id", s.get("id"));
                    song.put("name", s.get("name"));
                    song.put("duration", s.get("dt") != null ? (int) s.get("dt") / 1000 : 0);
                    song.put("genre", genreHint);  // 继承歌单风格标签

                    // 歌手
                    List<Map> ar = (List<Map>) s.get("ar");
                    if (ar != null && !ar.isEmpty()) {
                        song.put("artist", ar.get(0).get("name"));
                    } else {
                        song.put("artist", "未知歌手");
                    }

                    // 专辑 + 封面
                    Map al = (Map) s.get("al");
                    if (al != null) {
                        song.put("album", al.get("name"));
                        song.put("coverUrl", al.get("picUrl"));
                    } else {
                        song.put("album", "");
                        song.put("coverUrl", "");
                    }

                    songList.add(song);
                }
                result.put("songs", songList);
            }

            return Result.ok(result);
        } catch (Exception e) {
            return Result.fail("获取歌单失败: " + e.getMessage());
        }
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
