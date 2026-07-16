-- =====================================================
-- DJ音乐电台 — 音乐中心模块 数据库初始化
-- 数据库: dj_music_radio
-- =====================================================

-- 1. 歌曲表
DROP TABLE IF EXISTS play_history;
DROP TABLE IF EXISTS user_favorite;
DROP TABLE IF EXISTS playlist_song;
DROP TABLE IF EXISTS playlist;
DROP TABLE IF EXISTS song;

CREATE TABLE song (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(200)  NOT NULL COMMENT '歌曲标题',
    artist      VARCHAR(200)  NOT NULL COMMENT '歌手名',
    album       VARCHAR(200)  DEFAULT ''    COMMENT '专辑名',
    genre       VARCHAR(50)   DEFAULT ''    COMMENT '流派',
    duration    INT           DEFAULT 0     COMMENT '时长(秒)',
    cover_url   VARCHAR(500)  DEFAULT ''    COMMENT '封面图片URL',
    source      VARCHAR(20)   DEFAULT 'PRESET' COMMENT '来源: PRESET/NETEASE',
    source_id   VARCHAR(100)  DEFAULT ''    COMMENT '来源平台歌曲ID',
    file_path   VARCHAR(500)  DEFAULT ''    COMMENT '本地音频文件路径',
    lyric       TEXT          DEFAULT NULL  COMMENT '歌词(LRC格式)',
    play_count  INT           DEFAULT 0     COMMENT '播放次数',
    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='歌曲表';

-- 2. 歌单表
CREATE TABLE playlist (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(200)  NOT NULL COMMENT '歌单名称',
    description VARCHAR(500)  DEFAULT ''    COMMENT '描述',
    user_id     BIGINT        DEFAULT 1     COMMENT '所属用户ID',
    cover_url   VARCHAR(500)  DEFAULT ''    COMMENT '封面URL',
    song_count  INT           DEFAULT 0     COMMENT '歌曲数量',
    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='歌单表';

-- 3. 歌单-歌曲关联表
CREATE TABLE playlist_song (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    playlist_id BIGINT        NOT NULL COMMENT '歌单ID',
    song_id     BIGINT        NOT NULL COMMENT '歌曲ID',
    sort_order  INT           DEFAULT 0     COMMENT '排序序号',
    added_at    DATETIME      DEFAULT CURRENT_TIMESTAMP,
    KEY idx_playlist (playlist_id),
    KEY idx_song (song_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='歌单-歌曲关联表';

-- 4. 用户收藏表
CREATE TABLE user_favorite (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT        NOT NULL COMMENT '用户ID',
    song_id     BIGINT        NOT NULL COMMENT '歌曲ID',
    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_song (user_id, song_id),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏表';

-- 5. 播放历史表
CREATE TABLE play_history (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT        NOT NULL COMMENT '用户ID',
    song_id     BIGINT        NOT NULL COMMENT '歌曲ID',
    played_at   DATETIME      DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_time (user_id, played_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='播放历史表';

-- =====================================================
-- 种子数据：已移除预置测试歌曲
-- 歌曲通过网易云歌单导入获取
-- =====================================================
