<script setup>
import { showConfirmDialog, showToast } from 'vant';
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import {
  fetchMyNotices,
  logout,
  pageTasks,
  projectOptions,
  unreadCount,
} from '../api';
import { pageTickets } from '../api';

const router = useRouter();
const user = JSON.parse(localStorage.getItem('h5_user') || '{}');
const nickname = computed(() => user.nickname || user.username || '用户');
const hour = new Date().getHours();
const greeting = hour < 6 ? '凌晨好' : hour < 12 ? '早安' : hour < 14 ? '午安' : hour < 18 ? '下午好' : '晚上好';

const loading = ref(true);
const stats = ref({ tasks: 0, tickets: 0, notices: 0, projects: 0 });
const myTickets = ref([]);
const notices = ref([]);

async function load() {
  loading.value = true;
  try {
    const userId = user.userId;
    const [taskRes, ticketRes, noticeRes, projects] = await Promise.all([
      pageTasks({ assigneeId: userId, page: 1, pageSize: 50 }),
      pageTickets({ assigneeId: userId, page: 1, pageSize: 50 }),
      fetchMyNotices({ page: 1, pageSize: 5 }),
      projectOptions(),
    ]);
    myTickets.value = ticketRes.list.filter((t) => t.status < 4).slice(0, 5);
    notices.value = noticeRes.list.filter((n) => n.readFlag === 0);
    stats.value = {
      tasks: taskRes.list.filter((t) => t.status === 0 || t.status === 1).length,
      tickets: ticketRes.list.filter((t) => t.status < 4).length,
      notices: noticeRes.unreadCount,
      projects: projects.length,
    };
  } finally {
    loading.value = false;
  }
}

async function doLogout() {
  await showConfirmDialog({ title: '退出登录', message: '确定退出当前账号？' });
  await logout();
  localStorage.removeItem('h5_token');
  localStorage.removeItem('h5_user');
  router.replace('/login');
}

const STATUS = ['待受理', '处理中', '待确认', '已解决', '已关闭'];
const STATUS_COLOR = ['default', 'primary', 'warning', 'success', 'default'];

onMounted(load);
</script>

<template>
  <div class="page">
    <van-nav-bar title="工作台" fixed>
      <template #right>
        <van-icon name="revoke" size="20" @click="doLogout" />
      </template>
    </van-nav-bar>

    <div class="banner">
      <div class="greet">{{ greeting }}，{{ nickname }} ✨</div>
      <div class="date">
        {{ new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }) }}
        <span v-if="stats.notices > 0"> · 🔔 {{ stats.notices }} 条未读</span>
      </div>
    </div>

    <van-row gutter="10" class="stat-row">
      <van-col span="8">
        <div class="stat-card" @click="router.push('/ticket')">
          <div class="num">{{ stats.tickets }}</div>
          <div class="label">我的工单</div>
        </div>
      </van-col>
      <van-col span="8">
        <div class="stat-card" @click="router.push('/ticket/create')">
          <div class="num">+ 提单</div>
          <div class="label">快速报障</div>
        </div>
      </van-col>
      <van-col span="8">
        <div class="stat-card" @click="router.push('/notice')">
          <div class="num">{{ stats.notices }}</div>
          <div class="label">未读消息</div>
        </div>
      </van-col>
    </van-row>

    <van-cell-group inset title="快捷入口" class="mb">
      <van-grid :column-num="4" :border="false">
        <van-grid-item icon="apps-o" text="产品管理" @click="router.push('/product')" />
        <van-grid-item icon="orders-o" text="工单列表" @click="router.push('/ticket')" />
        <van-grid-item icon="edit" text="提交工单" @click="router.push('/ticket/create')" />
        <van-grid-item icon="bell" text="我的消息" @click="router.push('/notice')" />
      </van-grid>
    </van-cell-group>

    <van-cell-group inset title="我的工单" class="mb">
      <van-cell
        v-for="t in myTickets"
        :key="t.id"
        :title="t.title"
        :label="t.ticketNo"
        is-link
        @click="router.push(`/ticket/${t.id}`)"
      >
        <template #value>
          <van-tag :type="STATUS_COLOR[t.status]">{{ STATUS[t.status] }}</van-tag>
        </template>
      </van-cell>
      <van-cell v-if="myTickets.length === 0 && !loading" title="暂无进行中工单 🎉" />
    </van-cell-group>

    <van-tabbar fixed placeholder route>
      <van-tabbar-item icon="wap-home-o" to="/">工作台</van-tabbar-item>
      <van-tabbar-item icon="apps-o" to="/product">产品</van-tabbar-item>
      <van-tabbar-item icon="orders-o" to="/ticket">工单</van-tabbar-item>
      <van-tabbar-item icon="bell" to="/notice" :badge="stats.notices || ''">消息</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<style scoped>
.banner { margin: 60px 12px 12px; padding: 20px; border-radius: 16px; background: linear-gradient(135deg, #1677ff, #5b8ff9); color: #fff; }
.greet { font-size: 18px; font-weight: 600; }
.date { margin-top: 6px; font-size: 13px; opacity: 0.85; }
.stat-row { margin: 0 4px; }
.stat-card { background: #fff; border-radius: 12px; padding: 14px 0; text-align: center; box-shadow: 0 1px 4px rgba(0,0,0,0.04); }
.stat-card .num { font-size: 22px; font-weight: 700; color: #1677ff; }
.stat-card .label { font-size: 12px; color: #999; margin-top: 2px; }
.mb { margin-bottom: 12px; }
</style>
