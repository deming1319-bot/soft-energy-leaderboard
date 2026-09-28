<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Lock, User } from '@element-plus/icons-vue'
import BrandMark from '@/components/BrandMark.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

async function submit() {
  if (!form.username || !form.password) return
  loading.value = true
  try {
    await auth.login(form.username.trim(), form.password)
    await router.replace(typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard')
  } finally { loading.value = false }
}
</script>

<template>
  <main class="login-page">
    <section class="visual-panel">
      <BrandMark light />
      <div class="visual-panel__art" aria-hidden="true"><span class="moon" /><span class="mountain one" /><span class="mountain two" /><i class="seal">知<br>行</i></div>
      <div class="visual-panel__copy">
        <p>修习 · 答题 · 共证</p>
        <h1>以问启思，<br><em>以行证知。</em></h1>
        <blockquote>“学而不思则罔，思而不学则殆。”</blockquote>
      </div>
      <footer>问道修习平台 · 管理端</footer>
    </section>
    <section class="form-panel">
      <div class="login-box">
        <div class="eyebrow">ADMINISTRATION</div>
        <h2>欢迎回来</h2>
        <p class="intro">登录后管理题目发布、学员答题与荣誉榜单。</p>
        <el-form label-position="top" size="large" @submit.prevent="submit">
          <el-form-item label="管理员账号"><el-input v-model="form.username" :prefix-icon="User" autocomplete="username" placeholder="请输入账号" /></el-form-item>
          <el-form-item label="登录密码"><el-input v-model="form.password" :prefix-icon="Lock" type="password" autocomplete="current-password" show-password placeholder="请输入密码" @keyup.enter="submit" /></el-form-item>
          <el-button class="login-submit" type="primary" :loading="loading" @click="submit">进入管理台</el-button>
        </el-form>
        <div class="security-note"><span>✓</span> 独立管理员身份，与学员微信账号完全隔离</div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.login-page { min-height: 100vh; display: grid; grid-template-columns: minmax(420px, 1.05fr) minmax(420px, .95fr); background: var(--paper-50); }
.visual-panel { position: relative; padding: 46px clamp(44px, 6vw, 90px); display: flex; flex-direction: column; overflow: hidden; color: #fff; background: #25322b; }
.visual-panel::before { content: ''; position: absolute; inset: 0; opacity: .32; background-image: linear-gradient(rgba(255,255,255,.03) 1px, transparent 1px), linear-gradient(90deg,rgba(255,255,255,.03) 1px, transparent 1px); background-size: 38px 38px; mask-image: linear-gradient(to bottom, transparent, #000 20%, #000); }
.visual-panel__art { position: absolute; inset: 14% -8% 0 10%; }
.moon { position: absolute; right: 15%; top: 2%; width: 210px; height: 210px; border-radius: 50%; background: #d8cba9; opacity: .85; box-shadow: 0 0 80px rgba(216,203,169,.16); }
.mountain { position: absolute; right: -4%; bottom: 8%; width: 78%; height: 58%; background: #33483d; clip-path: polygon(0 100%, 22% 40%, 37% 67%, 60% 5%, 100% 100%); }
.mountain.two { left: -8%; bottom: -7%; width: 88%; height: 54%; background: #3c5145; opacity: .88; clip-path: polygon(0 100%, 15% 64%, 34% 84%, 54% 24%, 72% 73%, 85% 46%, 100% 100%); }
.seal { position: absolute; right: 9%; top: 39%; padding: 9px 7px; color: #f8dcd4; border: 1px solid rgba(225,119,99,.7); font-family: var(--font-serif); font-size: 16px; font-style: normal; line-height: 1.25; }
.visual-panel__copy { position: relative; z-index: 2; margin: auto 0; }
.visual-panel__copy > p { color: #c5b88f; font-size: 12px; font-weight: 700; letter-spacing: .45em; }
.visual-panel h1 { margin: 22px 0 34px; font-family: var(--font-serif); font-size: clamp(45px, 5vw, 72px); font-weight: 500; line-height: 1.28; letter-spacing: .06em; }
.visual-panel h1 em { color: #d8cba9; font-style: normal; }
blockquote { margin: 0; padding-left: 18px; border-left: 2px solid var(--cinnabar-500); color: rgba(255,255,255,.63); font-family: var(--font-serif); font-size: 15px; letter-spacing: .08em; }
.visual-panel footer { position: relative; z-index: 2; color: rgba(255,255,255,.4); font-size: 11px; letter-spacing: .12em; }
.form-panel { padding: 50px clamp(40px, 7vw, 110px); display: grid; place-items: center; background: radial-gradient(circle at 80% 10%, rgba(193,171,131,.12), transparent 30%), var(--paper-50); }
.login-box { width: min(430px, 100%); }
.login-box h2 { margin: 12px 0 8px; font-family: var(--font-serif); font-size: 36px; letter-spacing: .08em; }
.intro { margin: 0 0 38px; color: var(--ink-500); font-size: 14px; line-height: 1.7; }
.login-submit { width: 100%; min-height: 48px; margin-top: 6px; font-size: 15px; }
.security-note { margin-top: 25px; padding-top: 21px; color: var(--ink-500); border-top: 1px solid var(--paper-300); font-size: 12px; text-align: center; }
.security-note span { color: var(--pine-600); }
@media (max-width: 880px) { .login-page { grid-template-columns: 1fr; } .visual-panel { min-height: 260px; padding: 32px; } .visual-panel__copy { margin-top: 50px; } .visual-panel h1 { margin-block: 10px 16px; font-size: 38px; } .visual-panel footer, blockquote { display: none; } .moon { width: 140px; height: 140px; } .form-panel { min-height: calc(100vh - 260px); padding: 42px 25px; } }
</style>
