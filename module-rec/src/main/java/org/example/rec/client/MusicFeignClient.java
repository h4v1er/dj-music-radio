package org.example.rec.client;

import org.example.rec.dto.ResultDTO;
import org.example.rec.dto.SongDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * OpenFeign 客户端 — 调用音乐服务 module-music (:8082)
 * 按 API 规范声明接口，含回退工厂防止音乐服务不可用时崩溃
 */
@FeignClient(
        name = "module-music",
        fallbackFactory = MusicFeignFallback.class
)
public interface MusicFeignClient {

    /** 获取歌曲详情（含流派、歌手等信息） */
    @GetMapping("/music/song/{id}")
    ResultDTO<SongDTO> getSongById(@PathVariable("id") Integer id);

    /** 按关键词搜索歌曲（用于同流派/同歌手推荐） */
    @GetMapping("/music/song/search")
    ResultDTO<List<SongDTO>> searchSongs(@RequestParam("kw") String keyword);
}
