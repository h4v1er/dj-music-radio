-- =====================================================
-- DJ音乐电台 — 情绪智能升级 DDL
-- 数据库: dj_music_radio
-- =====================================================

-- 1. 歌曲情绪画像表
DROP TABLE IF EXISTS song_emotion;
CREATE TABLE song_emotion (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    song_id         BIGINT NOT NULL UNIQUE COMMENT '关联歌曲ID',
    primary_emotion VARCHAR(32) NOT NULL    COMMENT '主情绪标签',
    secondary_emotion VARCHAR(32) DEFAULT '' COMMENT '次情绪标签',
    valence         INT DEFAULT 0            COMMENT '效价 -100~100（正=积极/负=消极）',
    arousal         INT DEFAULT 50           COMMENT '唤醒度 0~100（低=舒缓/高=亢奋）',
    emotion_intensity INT DEFAULT 50         COMMENT '情绪强度 0~100',
    mood_tags       VARCHAR(500) DEFAULT ''  COMMENT '氛围标签（逗号分隔）',
    lyric_theme     VARCHAR(64) DEFAULT ''   COMMENT '歌词主题：爱情/自然/社会/哲思/叙事/励志/乡愁',
    lyric_keywords  TEXT                     COMMENT '匹配到的关键词 JSON 数组',
    suitable_scenes VARCHAR(500) DEFAULT ''  COMMENT '适合场景（逗号分隔）',
    analyzed        TINYINT DEFAULT 1        COMMENT '是否已完成分析',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (song_id) REFERENCES song(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='歌曲情绪画像';

-- 2. 情绪词典表
DROP TABLE IF EXISTS emotion_keyword;
CREATE TABLE emotion_keyword (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    keyword     VARCHAR(32) NOT NULL    COMMENT '中文关键词',
    emotion_tag VARCHAR(32) NOT NULL    COMMENT '对应情绪标签（中文名）',
    weight      DECIMAL(3,2) DEFAULT 0.50 COMMENT '权重 0.00~1.00',
    UNIQUE KEY uk_kw_emotion (keyword, emotion_tag),
    INDEX idx_keyword (keyword),
    INDEX idx_emotion (emotion_tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='情绪词典';

-- 3. 用户品味画像表
DROP TABLE IF EXISTS user_taste;
CREATE TABLE user_taste (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL DEFAULT 1   COMMENT '用户ID',
    emotion_prefs   TEXT                         COMMENT '情绪偏好分布 JSON {emotion: weight}',
    top_emotions    VARCHAR(200) DEFAULT ''      COMMENT 'Top3 情绪',
    taste_desc      VARCHAR(500) DEFAULT ''      COMMENT '品味一句话描述',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户品味画像';

-- 4. 扩展 song 表（添加情绪冗余字段）
ALTER TABLE song ADD COLUMN IF NOT EXISTS emotion_tags VARCHAR(300) DEFAULT '' COMMENT '情绪标签（冗余，方便列表查询）';
ALTER TABLE song ADD COLUMN IF NOT EXISTS emotion_analyzed TINYINT DEFAULT 0 COMMENT '是否已完成情绪分析';
