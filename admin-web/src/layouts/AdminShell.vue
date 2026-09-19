<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChatDotSquare, DataAnalysis, Document, Fold, List, Monitor, Operation, User, Expand, ArrowDown, SwitchButton } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import BrandMark from '@/components/BrandMark.vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const collapsed = ref(false)
const mobileOpen = ref(false)
const items = [
  { to: '/dashboard', label: '总览', caption: '今日数据', icon: DataAnalysis },
  { to: '/activities', label: '题目管理', caption: '发布与统计', icon: Document },
  { to: '/test-center', label: '测试中心', caption: '模拟学员答题', icon: Monitor },
  { to: '/users', label: '学员', caption: '参与档案', icon: User },
  { to: '/reports', label: '反馈与举报', caption: '受理与处置', icon: ChatDotSquare },
  { to: '/audit', label: '操作记录', caption: '安全审计', icon: List },
]
const title = computed(() => items.find((item) => route.path.startsWith(item.to))?.label || '修习管理')
const today = new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }).format(new Date())

async function logout() {
  await ElMessageBox.confirm('退出后需要重新登录管理台。', '确认退出', { confirmButtonText: '退出登录', cancelButtonText: '暂不退出', type: 'warning' })
  auth.logout()
  await router.replace('/login')
}
</script>

<template>
  <div class="admin-layout" :class="{ 'is-collapsed': collapsed }">
    <div class="mobile-mask" :class="{ show: mobileOpen }" @click="mobileOpen = false" />
    <aside class="sidebar" :class="{ 'mobile-open': mobileOpen }">
      <BrandMark :compact="collapsed" />
      <div class="sidebar__rule"><span>修身 · 明心 · 笃行</span></div>
      <nav>
        <RouterLink v-for="item in items" :key="item.to" :to="item.to" :class="{ active: route.path.startsWith(item.to) }" @click="mobileOpen = false">
          <component :is="item.icon" class="nav-icon" />
          <span v-if="!collapsed" class="nav-copy"><b>{{ item.label }}</b><small>{{ item.caption }}</small></span>
        </RouterLink>
      </nav>
      <div class="sidebar__footer">
        <div v-if="!collapsed" class="sidebar__saying"><i>“</i><span>知者行之始，<br>行者知之成。</span></div>
        <button class="collapse-btn" type="button" @click="collapsed = !collapsed"><component :is="collapsed ? Expand : Fold" /></button>
      </div>
    </aside>

    <main class="main-area">
      <header class="topbar">
        <button class="mobile-menu" type="button" aria-label="打开菜单" @click="mobileOpen = true"><Operation /></button>
        <div><span class="topbar__eyebrow">问道管理台</span><h2>{{ title }}</h2></div>
        <div class="topbar__right">
          <span class="today">{{ today }}</span>
          <el-dropdown trigger="click">
            <button class="account" type="button"><span class="account__avatar">管</span><span class="account__copy"><b>{{ auth.user?.displayName || '管理员' }}</b><small>系统管理员</small></span><ArrowDown /></button>
            <template #dropdown><el-dropdown-menu><el-dropdown-item :icon="SwitchButton" @click="logout">退出登录</el-dropdown-item></el-dropdown-menu></template>
          </el-dropdown>
        </div>
      </header>
      <section class="workspace"><RouterView /></section>
    </main>
  </div>
</template>

<style scoped>
.admin-layout { --side-width: 248px; min-height: 100vh; }
.sidebar { position: fixed; inset: 0 auto 0 0; z-index: 20; width: var(--side-width); padding: 28px 22px 22px; display: flex; flex-direction: column; background: #f9f7f1; border-right: 1px solid rgba(75,67,56,.12); transition: width .25s ease, transform .25s ease; }
.sidebar__rule { margin: 25px 0 18px; height: 32px; display: flex; align-items: center; color: var(--gold-600); font-family: var(--font-serif); font-size: 11px; letter-spacing: .2em; white-space: nowrap; }
.sidebar__rule::before, .sidebar__rule::after { content: ''; flex: 1; height: 1px; background: var(--paper-300); }
.sidebar__rule span { padding: 0 8px; }
nav { display: grid; gap: 7px; }
nav a { min-height: 60px; padding: 10px 12px; display: flex; align-items: center; gap: 13px; border-radius: 13px; color: var(--ink-650); transition: .2s ease; }
nav a:hover { color: var(--ink-950); background: rgba(235,230,220,.64); }
nav a.active { color: var(--cinnabar-700); background: #f1e5df; box-shadow: inset 3px 0 var(--cinnabar-600); }
.nav-icon { width: 19px; flex: 0 0 auto; }
.nav-copy { display: grid; gap: 3px; }
.nav-copy b { font-size: 14px; }
.nav-copy small { color: var(--ink-500); font-size: 10px; }
.sidebar__footer { margin-top: auto; }
.sidebar__saying { margin-bottom: 17px; padding: 16px; display: flex; gap: 8px; color: var(--ink-650); background: var(--paper-100); border-radius: 14px; font-family: var(--font-serif); font-size: 13px; line-height: 1.7; }
.sidebar__saying i { color: var(--cinnabar-500); font-size: 26px; font-style: normal; line-height: 1; }
.collapse-btn, .mobile-menu { border: 0; cursor: pointer; color: var(--ink-650); background: transparent; }
.collapse-btn { width: 100%; height: 39px; border: var(--border-subtle); border-radius: 10px; }
.collapse-btn svg { width: 18px; }
.main-area { min-height: 100vh; margin-left: var(--side-width); transition: margin-left .25s ease; }
.topbar { height: 84px; padding: 0 clamp(24px, 3vw, 48px); display: flex; align-items: center; justify-content: space-between; background: rgba(245,242,235,.86); border-bottom: 1px solid rgba(79,71,60,.09); backdrop-filter: blur(16px); position: sticky; top: 0; z-index: 12; }
.topbar__eyebrow { display: none; color: var(--cinnabar-600); font-size: 10px; letter-spacing: .18em; }
.topbar h2 { margin: 0; font-family: var(--font-serif); font-size: 21px; letter-spacing: .08em; }
.topbar__right { display: flex; align-items: center; gap: 24px; }
.today { color: var(--ink-500); font-family: var(--font-serif); font-size: 13px; }
.account { padding: 4px; display: flex; align-items: center; gap: 10px; border: 0; cursor: pointer; background: transparent; color: var(--ink-650); }
.account__avatar { width: 37px; height: 37px; display: grid; place-items: center; border-radius: 11px; color: #fff; background: var(--pine-700); font-family: var(--font-serif); }
.account__copy { display: grid; gap: 2px; text-align: left; }
.account__copy b { color: var(--ink-950); font-size: 13px; }
.account__copy small { color: var(--ink-500); font-size: 10px; }
.account > svg { width: 12px; }
.workspace { padding: clamp(28px, 3vw, 48px); }
.mobile-menu { display: none; width: 38px; }
.mobile-menu svg { width: 20px; }
.mobile-mask { display: none; }
.is-collapsed { --side-width: 84px; }
.is-collapsed .sidebar { padding-inline: 20px; }
.is-collapsed .sidebar__rule span { display: none; }
.is-collapsed nav a { justify-content: center; padding-inline: 8px; }
@media (max-width: 900px) {
  .admin-layout { --side-width: 0px; }
  .sidebar { width: 248px; transform: translateX(-100%); box-shadow: var(--shadow-float); }
  .sidebar.mobile-open { transform: translateX(0); }
  .mobile-mask { position: fixed; inset: 0; z-index: 19; background: rgba(28,31,29,.3); backdrop-filter: blur(2px); }
  .mobile-mask.show { display: block; }
  .mobile-menu { display: grid; place-items: center; }
  .topbar { gap: 12px; height: 76px; }
  .topbar > div:nth-child(2) { margin-right: auto; }
  .today, .account__copy, .account > svg { display: none; }
}
@media (max-width: 560px) { .workspace { padding: 24px 16px 90px; } .topbar { padding: 0 16px; } .topbar__eyebrow { display: block; } }
</style>
