package org.example.music.taxonomy;

/**
 * 音乐情绪分类体系 — 20 种情绪 × 5 大家族
 * 每种情绪包含：中文名、英文key、效价(valence)、唤醒度(arousal)、触发关键词
 *
 * 效价: -100(极度消极) ~ 100(极度积极)
 * 唤醒度: 0(极度舒缓) ~ 100(极度亢奋)
 */
public enum MusicEmotion {

    // ═══════════ 家族1: 柔和抒情 ═══════════
    GENTLE_TENDER("温柔缱绻", "gentle", -30, 20,
            "温柔", "柔软", "缠绵", "细语", "耳语", "微风", "暖", "轻抚",
            "呢喃", "依偎", "温存", "轻轻", "慢慢", "细腻", "柔软的心", "春风"),
    SWEET_ROMANTIC("甜蜜浪漫", "sweet", 60, 35,
            "甜蜜", "恋爱", "浪漫", "心动", "告白", "喜欢你", "爱上", "玫瑰",
            "巧克力", "约会", "牵手", "初恋", "粉色", "甜", "想你", "在一起", "我爱你", "婚礼", "永远"),
    WARM_HEALING("温暖治愈", "healing", 40, 15,
            "阳光", "希望", "黎明", "重生", "治愈", "彩虹", "明天会", "拥抱",
            "向前走", "没关系", "一切都会", "加油", "站起来", "新的开始", "光芒",
            "微笑", "花开", "春天", "勇敢", "相信", "未来"),
    PEACEFUL("静谧安宁", "peaceful", 10, 5,
            "安静", "宁静", "安详", "入睡", "月光", "星空", "慢慢", "轻轻",
            "晚安", "沉睡", "平和", "平静", "安宁", "夜深", "入梦", "安眠", "呼吸", "静"),

    // ═══════════ 家族2: 深沉内省 ═══════════
    MELANCHOLIC("忧郁感伤", "melancholy", -50, 10,
            "忧伤", "难过", "眼泪", "下雨", "秋天", "落叶", "心碎", "哭了",
            "悲伤", "痛", "心酸", "哽咽", "凋零", "凋谢", "枯萎", "伤痕", "阴天", "伤", "破碎", "苍白的"),
    LONELY("孤独寂寥", "lonely", -60, 5,
            "孤单", "寂寞", "一个人", "空荡", "失眠", "深夜", "无人", "独自",
            "空房间", "守着", "冷冷清清", "沉默", "没人", "荒芜", "独白", "只有我", "影子", "孤独的"),
    DEEP_RESERVED("深沉内敛", "deep", -10, 30,
            "深沉", "内敛", "克制", "隐忍", "不说", "沉静", "厚重", "忍",
            "暗涌", "压抑", "深藏", "埋", "不说出口", "隐藏", "坚强"),
    NOSTALGIC("追忆怀旧", "nostalgic", 10, 20,
            "回忆", "从前", "小时候", "青春", "那年", "老照片", "毕业", "曾经",
            "怀念", "过去", "旧时光", "往事", "那年夏天", "多年以后", "回不去",
            "童年", "校服", "同桌", "日记", "发黄"),

    // ═══════════ 家族3: 高能释放 ═══════════
    PASSIONATE("热烈奔放", "passion", 80, 90,
            "燃烧", "狂热", "疯狂", "放肆", "呐喊", "奔跑", "火焰", "热烈",
            "不羁", "释放", "爆发", "冲出", "火热", "炽热", "灵魂"),
    HEROIC("热血激昂", "heroic", 75, 85,
            "热血", "战斗", "梦想", "冲刺", "胜利", "不退", "坚持", "向着",
            "拼搏", "逆风", "不服", "绝不", "奔跑", "战场", "荣耀", "光芒万丈", "冲破", "冠军"),
    FREE_SPIRITED("洒脱不羁", "free", 65, 70,
            "自由", "洒脱", "不羁", "流浪", "远方", "挣脱", "无所谓", "随风",
            "放荡", "浪迹", "天涯", "走天涯", "说走就走", "任性", "不管", "去吧", "放下一切"),
    PLAYFUL("俏皮灵动", "playful", 55, 55,
            "俏皮", "调皮", "可爱", "鬼马", "蹦蹦跳跳", "玩笑", "古灵精怪",
            "灵动", "搞怪", "嘻嘻", "哈", "啦啦", "顽皮", "小跳", "欢快"),

    // ═══════════ 家族4: 复杂暗黑 ═══════════
    DECADENT("颓废迷离", "decadent", -25, 15,
            "颓废", "迷离", "醉酒", "霓虹", "梦境", "恍惚", "烟雾", "麻醉",
            "沉醉", "酒", "灯红酒绿", "纸醉金迷", "堕落", "虚无", "溃烂", "幻象", "迷失"),
    GRIEF_DEFIANT("悲愤抗争", "defiant", -70, 70,
            "愤怒", "抗争", "不公", "挣扎", "打破", "黑暗", "不屈", "咆哮",
            "怒吼", "反抗", "不屈服", "呐喊", "撕裂", "摧毁", "控诉", "质问"),
    ETHEREAL("空灵梦幻", "ethereal", 20, 10,
            "空灵", "梦幻", "仙境", "飘渺", "云端", "天使", "翅膀", "飞翔",
            "星空", "极光", "幻境", "盘旋", "灵魂出窍", "缥缈", "涟漪"),
    BLEAK("荒凉苍茫", "bleak", -40, 25,
            "荒凉", "苍茫", "大漠", "风沙", "旷野", "天涯", "孤烟", "戈壁",
            "沙漠", "荒芜", "苍凉", "落日", "苍茫大地", "一望无际", "萧瑟"),

    // ═══════════ 家族5: 叙事人文 ═══════════
    FOLK_EARTHY("市井烟火", "folky", 30, 25,
            "街头", "烟火", "人间", "家常", "小城", "集市", "邻里", "巷子",
            "街角", "菜市场", "炊烟", "弄堂", "早点摊", "公交车", "老街", "日子", "柴米油盐"),
    HOMESICK("乡愁思念", "homesick", -15, 10,
            "故乡", "家乡", "妈妈", "老房子", "过年", "回家", "饭菜", "炊烟",
            "童年", "外婆", "爷爷奶奶", "家门", "团圆", "想家", "乡音", "根", "故土"),
    YOUTHFUL("青春悸动", "youth", 35, 40,
            "青春", "校园", "教室", "操场", "暗恋", "同桌", "纸条", "夏天",
            "十七岁", "十八岁", "校服", "课本", "篮球场", "单车", "年少", "懵懂", "青涩"),
    MOVED("触动感怀", "moved", 25, 30,
            "感动", "触动", "泪目", "共鸣", "故事", "人生", "懂得", "理解",
            "温暖", "治愈", "难忘", "刻骨铭心", "铭心", "热泪盈眶", "共情");

    // ── 字段 ──
    private final String chineseName;
    private final String englishKey;
    private final int valence;      // -100 ~ 100
    private final int arousal;      // 0 ~ 100
    private final String[] keywords;

    MusicEmotion(String chineseName, String englishKey, int valence, int arousal, String... keywords) {
        this.chineseName = chineseName;
        this.englishKey = englishKey;
        this.valence = valence;
        this.arousal = arousal;
        this.keywords = keywords;
    }

    // ── Getters ──
    public String getChineseName() { return chineseName; }
    public String getEnglishKey() { return englishKey; }
    public int getValence() { return valence; }
    public int getArousal() { return arousal; }
    public String[] getKeywords() { return keywords; }

    /**
     * 根据中文名查找枚举
     */
    public static MusicEmotion fromChineseName(String name) {
        for (MusicEmotion e : values()) {
            if (e.chineseName.equals(name)) return e;
        }
        return null;
    }

    /**
     * 获取情绪家族（归类）
     */
    public String getFamily() {
        switch (this) {
            case GENTLE_TENDER: case SWEET_ROMANTIC: case WARM_HEALING: case PEACEFUL:
                return "柔和抒情";
            case MELANCHOLIC: case LONELY: case DEEP_RESERVED: case NOSTALGIC:
                return "深沉内省";
            case PASSIONATE: case HEROIC: case FREE_SPIRITED: case PLAYFUL:
                return "高能释放";
            case DECADENT: case GRIEF_DEFIANT: case ETHEREAL: case BLEAK:
                return "复杂暗黑";
            case FOLK_EARTHY: case HOMESICK: case YOUTHFUL: case MOVED:
                return "叙事人文";
            default: return "其他";
        }
    }

    /**
     * 获取适合展示的场景描述
     */
    public String getSuitableScenes() {
        if (arousal < 20) return "深夜,独处,阅读,睡前";
        if (arousal < 40) return "午后,散步,咖啡馆,雨天";
        if (arousal < 60) return "通勤,工作,聚会,驾驶";
        if (arousal < 80) return "运动,派对,旅行,兜风";
        return "运动,现场,狂欢,释放";
    }

    @Override
    public String toString() {
        return chineseName;
    }
}
