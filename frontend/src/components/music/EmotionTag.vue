<script setup>
/**
 * 情绪标签徽章组件
 * 5 个情绪家族对应 5 种颜色系
 */
import { computed } from 'vue'

const props = defineProps({
  emotion: { type: String, default: '' },
  size: { type: String, default: 'sm' } // sm | md
})

const familyMap = {
  '温柔缱绻': 'tender', '甜蜜浪漫': 'tender', '温暖治愈': 'tender', '静谧安宁': 'tender',
  '忧郁感伤': 'deep', '孤独寂寥': 'deep', '深沉内敛': 'deep', '追忆怀旧': 'deep',
  '热烈奔放': 'energy', '热血激昂': 'energy', '洒脱不羁': 'energy', '俏皮灵动': 'energy',
  '颓废迷离': 'dark', '悲愤抗争': 'dark', '空灵梦幻': 'dark', '荒凉苍茫': 'dark',
  '市井烟火': 'story', '乡愁思念': 'story', '青春悸动': 'story', '触动感怀': 'story'
}

const emojiMap = {
  '温柔缱绻': '🌸', '甜蜜浪漫': '💕', '温暖治愈': '☀️', '静谧安宁': '🌙',
  '忧郁感伤': '🌧', '孤独寂寥': '🌌', '深沉内敛': '🌊', '追忆怀旧': '📷',
  '热烈奔放': '🔥', '热血激昂': '⚡', '洒脱不羁': '🕊', '俏皮灵动': '🎪',
  '颓废迷离': '🥀', '悲愤抗争': '⚔', '空灵梦幻': '🫧', '荒凉苍茫': '🏜',
  '市井烟火': '🏘', '乡愁思念': '🏠', '青春悸动': '🎒', '触动感怀': '💧'
}

const family = computed(() => familyMap[props.emotion] || 'default')
const emoji = computed(() => emojiMap[props.emotion] || '🎵')
</script>

<template>
  <span v-if="emotion" class="emotion-tag" :class="[`family-${family}`, `size-${size}`]"
        :title="emotion">
    <span class="tag-emoji">{{ emoji }}</span>
    <span v-if="size === 'md'" class="tag-text">{{ emotion }}</span>
  </span>
</template>

<style scoped>
.emotion-tag {
  display: inline-flex; align-items: center; gap: 2px;
  border-radius: 10px; font-size: 11px;
  white-space: nowrap; transition: all 0.15s;
}
.size-sm { padding: 1px 6px; font-size: 10px; }
.size-md { padding: 2px 8px; font-size: 12px; }

.tag-emoji { font-size: inherit; }
.tag-text { font-weight: 500; }

/* 柔和抒情 — 暖粉/橙 */
.family-tender { background: rgba(245, 166, 35, 0.15); color: #f5a623; border: 1px solid rgba(245, 166, 35, 0.3); }
/* 深沉内省 — 蓝紫 */
.family-deep { background: rgba(100, 140, 220, 0.15); color: #7ea8f0; border: 1px solid rgba(100, 140, 220, 0.3); }
/* 高能释放 — 红色 */
.family-energy { background: rgba(233, 69, 96, 0.15); color: #e94560; border: 1px solid rgba(233, 69, 96, 0.3); }
/* 复杂暗黑 — 灰紫 */
.family-dark { background: rgba(140, 120, 180, 0.15); color: #b09cd0; border: 1px solid rgba(140, 120, 180, 0.3); }
/* 叙事人文 — 绿色 */
.family-story { background: rgba(78, 204, 163, 0.15); color: #4ecca3; border: 1px solid rgba(78, 204, 163, 0.3); }
</style>
