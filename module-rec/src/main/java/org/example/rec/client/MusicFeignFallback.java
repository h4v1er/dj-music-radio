package org.example.rec.client;

import org.example.rec.dto.ResultDTO;
import org.example.rec.dto.SongDTO;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 音乐服务调用失败时的回退工厂
 * 返回空数据而不是抛异常，保证推荐服务独立可用
 */
@Component
public class MusicFeignFallback implements FallbackFactory<MusicFeignClient> {

    @Override
    public MusicFeignClient create(Throwable cause) {
        // 记录失败原因，方便排查
        System.err.println("[推荐服务] 调用音乐服务失败: " + cause.getMessage());

        return new MusicFeignClient() {

            @Override
            public ResultDTO<SongDTO> getSongById(Integer id) {
                ResultDTO<SongDTO> result = new ResultDTO<>();
                result.setCode(-1);
                result.setMsg("音乐服务不可用");
                result.setData(null);
                return result;
            }

            @Override
            public ResultDTO<List<SongDTO>> searchSongs(String keyword) {
                ResultDTO<List<SongDTO>> result = new ResultDTO<>();
                result.setCode(-1);
                result.setMsg("音乐服务不可用");
                result.setData(Collections.emptyList());
                return result;
            }
        };
    }
}
