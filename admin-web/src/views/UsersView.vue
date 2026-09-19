<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Search, UserFilled } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { userApi } from '@/api/services'
import StatusPill from '@/components/StatusPill.vue'
import type { UserItem } from '@/types'

const loading = ref(true)
const keyword = ref('')
const rows = ref<UserItem[]>([])
const filtered = computed(() => rows.value.filter((item) => item.nickname.toLowerCase().includes(keyword.value.toLowerCase()) || item.maskedPhone.includes(keyword.value)))
const champions = computed(() => rows.value.filter((x) => x.championCount > 0).length)
const active = computed(() => rows.value.filter((x) => x.participationCount > 0).length)
onMounted(async () => { try { rows.value = await userApi.list() } finally { loading.value = false } })
</script>

<template>
  <div class="page-shell">
    <div class="page-heading"><div><span class="eyebrow">LEARNERS</span><h1>学员档案</h1><p>微信学员与测试学员、参与次数和获奖情况集中管理，数据按唯一身份隔离。</p></div></div>
    <section class="summary-strip"><div><UserFilled /><span>全部学员<b>{{ rows.length }}</b></span></div><i/><div><span>参与过答题<b>{{ active }}</b></span></div><i/><div><span>获得过冠军<b>{{ champions }}</b></span></div><p>手机号仅保存加密密文，后台只展示脱敏结果</p></section>
    <section class="surface users-panel">
      <header><div><h3>全部学员</h3><span>最近登录优先</span></div><el-input v-model="keyword" :prefix-icon="Search" clearable placeholder="搜索昵称或手机号" /></header>
      <el-table v-loading="loading" :data="filtered" style="width:100%">
        <el-table-column label="学员" min-width="190"><template #default="scope"><div class="learner"><el-avatar :size="42" :src="scope.row.avatarUrl || ''">{{ scope.row.nickname.slice(0,1) }}</el-avatar><div><b>{{ scope.row.nickname }}</b><small>加入于 {{ dayjs(scope.row.createdAt).format('YYYY-MM-DD') }}</small></div></div></template></el-table-column>
        <el-table-column prop="maskedPhone" label="手机号" width="140" />
        <el-table-column label="参与 / 获奖" width="180"><template #default="scope"><div class="honors"><span>答题 <b>{{ scope.row.participationCount }}</b></span><span>冠 <b>{{ scope.row.championCount }}</b></span><span>亚 <b>{{ scope.row.runnerUpCount }}</b></span></div></template></el-table-column>
        <el-table-column label="最近登录" width="170"><template #default="scope"><span class="muted">{{ scope.row.lastLoginAt ? dayjs(scope.row.lastLoginAt).format('YYYY-MM-DD HH:mm') : '尚未记录' }}</span></template></el-table-column>
        <el-table-column label="状态" width="100"><template #default="scope"><StatusPill :value="scope.row.status === 'ACTIVE' ? 'ENABLED' : scope.row.status" /></template></el-table-column>
        <template #empty><el-empty description="暂无学员数据" /></template>
      </el-table>
    </section>
  </div>
</template>

<style scoped>
.summary-strip{margin-bottom:20px;padding:23px 26px;display:flex;align-items:center;gap:22px;color:#fff;border-radius:var(--radius-lg);background:var(--pine-700)}.summary-strip>div{display:flex;align-items:center;gap:12px}.summary-strip svg{width:27px;color:#cfbf92}.summary-strip span{display:grid;gap:4px;color:rgba(255,255,255,.57);font-size:11px}.summary-strip b{color:#fff;font-family:var(--font-serif);font-size:25px}.summary-strip>i{width:1px;height:37px;background:rgba(255,255,255,.13)}.summary-strip p{margin-left:auto;color:rgba(255,255,255,.45);font-size:11px}.users-panel{padding:12px 19px 20px;overflow:hidden}.users-panel header{padding:10px 4px 18px;display:flex;align-items:center;justify-content:space-between}.users-panel h3{margin:0;font-family:var(--font-serif);font-size:20px}.users-panel header span{color:var(--ink-500);font-size:11px}.users-panel header .el-input{width:260px}.learner{padding:7px 0;display:flex;align-items:center;gap:11px}.learner div{display:grid;gap:3px}.learner small{color:var(--ink-500);font-size:10px}.honors{display:flex;gap:12px}.honors span{color:var(--ink-500);font-size:11px}.honors b{color:var(--ink-800)}
@media(max-width:760px){.summary-strip{display:grid;grid-template-columns:1fr 1fr}.summary-strip>i{display:none}.summary-strip p{grid-column:1/-1;margin:0}.users-panel header{align-items:flex-start;flex-direction:column;gap:14px}.users-panel header .el-input{width:100%}}
</style>
