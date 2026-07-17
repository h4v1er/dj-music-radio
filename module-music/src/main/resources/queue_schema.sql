-- =====================================================
-- DJ音乐电台 — 用户播放队列增量表结构
-- 适用于已有 dj_music_radio 数据库，不会删除现有数据
-- =====================================================

CREATE TABLE IF NOT EXISTS user_play_queue (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT        NOT NULL COMMENT '用户ID',
    song_id     BIGINT        DEFAULT NULL COMMENT '本地歌曲ID；网易云可为空或为平台ID',
    source      VARCHAR(20)   DEFAULT 'PROJECT' COMMENT '来源: PROJECT/NETEASE/PRESET',
    source_id   VARCHAR(100)  DEFAULT '' COMMENT '来源平台歌曲ID',
    title       VARCHAR(200)  NOT NULL COMMENT '歌曲标题快照',
    artist      VARCHAR(200)  DEFAULT '' COMMENT '歌手快照',
    album       VARCHAR(200)  DEFAULT '' COMMENT '专辑快照',
    genre       VARCHAR(50)   DEFAULT '' COMMENT '流派快照',
    cover_url   VARCHAR(500)  DEFAULT '' COMMENT '封面快照',
    file_path   VARCHAR(500)  DEFAULT '' COMMENT '播放地址快照',
    duration    INT           DEFAULT 0 COMMENT '时长(秒)',
    sort_order  INT           DEFAULT 0 COMMENT '队列顺序',
    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_sort (user_id, sort_order),
    KEY idx_user_source (user_id, source, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户播放队列表';

CREATE TABLE IF NOT EXISTS user_player_state (
    user_id     BIGINT PRIMARY KEY COMMENT '用户ID',
    play_mode   VARCHAR(20)   DEFAULT 'order' COMMENT '播放模式: order/shuffle/repeat',
    current_key VARCHAR(160)  DEFAULT '' COMMENT '当前歌曲稳定标识',
    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户播放器状态表';
