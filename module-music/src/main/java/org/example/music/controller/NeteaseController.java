package org.example.music.controller;

import org.example.music.dto.Result;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import jakarta.servlet.http.HttpServletRequest;
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

    /** 获取歌曲播放URL — 多音质等级 fallback，尽可能获取完整歌曲 */
    @GetMapping("/url")
    public Result<Object> getUrl(@RequestParam String id) {
        // 从高到低尝试不同音质等级，取第一个返回有效 URL 的
        String[] levels = {"lossless", "exhigh", "higher", "standard"};
        for (String level : levels) {
            try {
                String url = NETEASE_API + "/song/url/v1?id=" + id + "&level=" + level;
                Map resp = rest.getForObject(url, Map.class);
                if (resp != null && resp.get("data") != null) {
                    List<Map> data = (List<Map>) resp.get("data");
                    if (data != null && !data.isEmpty()) {
                        Object urlObj = data.get(0).get("url");
                        if (urlObj != null && !urlObj.toString().isEmpty()) {
                            return Result.ok(data);
                        }
                    }
                }
            } catch (Exception e) {
                // 该等级获取失败，继续尝试下一等级
            }
        }
        // 所有等级都失败，回退到原始接口
        try {
            String url = NETEASE_API + "/song/url?id=" + id;
            Map resp = rest.getForObject(url, Map.class);
            return Result.ok(resp != null ? resp.get("data") : null);
        } catch (Exception e) {
            return Result.fail("无法获取播放地址");
        }
    }

    /**
     * 代理网易云音频流。
     *
     * 前端直接播放网易云 CDN URL 时，不同浏览器/网络出口可能表现不一致。
     * 通过后端代理后，浏览器只访问本项目服务，实际 CDN 拉取统一由远端服务完成。
     */
    @GetMapping("/stream")
    public void stream(@RequestParam String id,
                       HttpServletRequest request,
                       HttpServletResponse response) {
        HttpURLConnection conn = null;
        try {
            String audioUrl = resolvePlayableUrl(id);
            if (audioUrl == null || audioUrl.isBlank()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            conn = (HttpURLConnection) URI.create(audioUrl).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("Referer", "https://music.163.com/");
            conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

            String range = request.getHeader("Range");
            if (range != null && !range.isBlank()) {
                conn.setRequestProperty("Range", range);
            }

            int upstreamStatus = conn.getResponseCode();
            if (upstreamStatus == HttpURLConnection.HTTP_PARTIAL) {
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                String contentRange = conn.getHeaderField("Content-Range");
                if (contentRange != null) {
                    response.setHeader("Content-Range", contentRange);
                }
            } else if (upstreamStatus >= 400) {
                response.setStatus(upstreamStatus);
                return;
            }

            String contentType = conn.getContentType();
            response.setContentType(contentType != null ? contentType : "audio/mpeg");
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("Cache-Control", "no-store");

            String contentLength = conn.getHeaderField("Content-Length");
            if (contentLength != null) {
                response.setHeader("Content-Length", contentLength);
            }

            try (InputStream in = conn.getInputStream();
                 OutputStream out = response.getOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
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

    /** 批量获取歌词 — 导入时使用，减少网络往返 */
    @GetMapping("/lyric/batch")
    public Result<Map<String, Object>> batchLyric(@RequestParam String ids) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String id : ids.split(",")) {
            String trimmed = id.trim();
            if (trimmed.isEmpty()) continue;
            try {
                String url = NETEASE_API + "/lyric?id=" + trimmed;
                Map resp = rest.getForObject(url, Map.class);
                result.put(trimmed, resp != null ? resp : null);
            } catch (Exception e) {
                result.put(trimmed, Map.of("error", e.getMessage()));
            }
        }
        return Result.ok(result);
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

    private String resolvePlayableUrl(String id) {
        String[] levels = {"lossless", "exhigh", "higher", "standard"};
        for (String level : levels) {
            try {
                String url = NETEASE_API + "/song/url/v1?id=" + id + "&level=" + level;
                Map resp = rest.getForObject(url, Map.class);
                String playableUrl = extractFirstUrl(resp);
                if (playableUrl != null) {
                    return playableUrl;
                }
            } catch (Exception e) {
                // 该音质失败时继续降级尝试
            }
        }
        try {
            String url = NETEASE_API + "/song/url?id=" + id;
            Map resp = rest.getForObject(url, Map.class);
            return extractFirstUrl(resp);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractFirstUrl(Map resp) {
        if (resp == null || resp.get("data") == null) {
            return null;
        }
        List<Map> data = (List<Map>) resp.get("data");
        if (data == null || data.isEmpty()) {
            return null;
        }
        Object urlObj = data.get(0).get("url");
        String url = urlObj != null ? urlObj.toString() : "";
        return url.isBlank() ? null : url;
    }
}
