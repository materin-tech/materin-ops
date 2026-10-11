<script lang="ts" setup>
// 工作台：当前用户待处理内容总览（任务/工单/消息/项目）
import type { Iteration, Task } from '#/api/requirement';
import type { Ticket } from '#/api/ticket';

import { computed, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useUserStore } from '@vben/stores';

import {
  Button,
  Card,
  Empty,
  Tag,
} from 'ant-design-vue';
import {
  pageIterations,
  pageTasks,
  projectOptions,
} from '#/api/requirement';
import { fetchMyNotices, markNoticeRead } from '#/api/notice';

import type { NoticeItem } from '#/api/notice';
import dayjs from 'dayjs';
import { pageTickets } from '#/api/ticket';
import { PRIORITY, TASK_STATUS, labelOf } from '#/views/requirement/shared';

const userStore = useUserStore();
const userId = computed(() => userStore.userInfo?.userId as number | undefined);
const nickname = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.realName || '');
const hour = new Date().getHours();
const greeting = hour < 6 ? '凌晨好' : hour < 12 ? '早安' : hour < 14 ? '午安' : hour < 18 ? '下午好' : '晚上好';

const loading = ref(true);
const stats = ref({ tasks: 0, tickets: 0, slaBreached: 0, notices: 0, projects: 0 });
const myTasks = ref<Task[]>([]);
const myTickets = ref<Ticket[]>([]);
const notices = ref<NoticeItem[]>([]);
const activeIterations = ref<Iteration[]>([]);

function isSlaBreached(t: Ticket) {
  const now = Date.now();
  if (t.status >= 3) {
    return !!t.resolveDeadline && !!t.resolvedAt && new Date(t.resolvedAt).getTime() > new Date(t.resolveDeadline).getTime();
  }
  if (t.status === 0) {
    return !!t.responseDeadline && now > new Date(t.responseDeadline).getTime();
  }
  return !!t.resolveDeadline && now > new Date(t.resolveDeadline).getTime();
}

function fmt(v?: null | string) {
  return v ? v.replace('T', ' ').slice(0, 16) : '';
}

async function load() {
  if (!userId.value) return;
  loading.value = true;
  try {
    const [taskRes, ticketRes, noticeRes, projects, iterRes] = await Promise.all([
      pageTasks({ assigneeId: userId.value, page: 1, pageSize: 50 }),
      pageTickets({ assigneeId: userId.value, page: 1, pageSize: 50 }),
      fetchMyNotices({ page: 1, pageSize: 5 }),
      projectOptions(),
      pageIterations({ page: 1, pageSize: 50, status: 1 }),
    ]);
    myTasks.value = taskRes.list.filter((t) => t.status === 0 || t.status === 1).slice(0, 6);
    const open = ticketRes.list.filter((t) => t.status < 4);
    myTickets.value = open.slice(0, 6);
    stats.value = {
      tasks: taskRes.list.filter((t) => t.status === 0 || t.status === 1).length,
      tickets: open.length,
      slaBreached: open.filter(isSlaBreached).length,
      notices: noticeRes.unreadCount,
      projects: projects.length,
    };
    notices.value = noticeRes.list.filter((n) => n.readFlag === 0);
    activeIterations.value = iterRes.list.slice(0, 5);
  } finally {
    loading.value = false;
  }
}

async function doRead(id: number) {
  await markNoticeRead(id);
  await load();
}

const TASK_STATUS_COLOR = ['default', 'blue', 'green', 'default'];
const TICKET_STATUS_LABEL = ['待受理', '处理中', '待确认', '已解决', '已关闭'];
const TICKET_STATUS_COLOR = ['default', 'blue', 'orange', 'green', 'default'];

onMounted(load);
</script>

<template>
  <Page title="工作台" description="你的待处理内容，一屏尽览">
    <!-- 问候横幅 -->
    <div class="mb-4 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-500 p-5 text-white shadow-md">
      <div class="flex items-center justify-between">
        <div>
          <div class="text-lg font-semibold">
            {{ greeting }}，{{ nickname || '朋友' }}！今天也要元气满满 ✨
          </div>
          <div class="mt-1 text-sm text-white/80">
            {{ dayjs().format('YYYY年MM月DD日 dddd') }}
            <span v-if="stats.notices > 0" class="ml-3">
              🔔 你有 <b>{{ stats.notices }}</b> 条未读消息
            </span>
          </div>
        </div>
        <Button ghost @click="$router.push({ name: 'MyNotices' })">
          查看消息
        </Button>
      </div>
    </div>

    <!-- 统计卡 -->
    <div class="mb-4 grid grid-cols-2 gap-4 lg:grid-cols-4">
      <Card
        class="cursor-pointer !border-0 shadow-sm transition hover:shadow-md"
        @click="$router.push({ name: 'RequirementTask' })"
      >
        <div class="flex items-center gap-3">
          <div class="flex size-11 items-center justify-center rounded-lg bg-blue-50 text-xl dark:bg-blue-950">📋</div>
          <div>
            <div class="text-2xl font-bold">{{ stats.tasks }}</div>
            <div class="text-xs text-gray-400">待办任务</div>
          </div>
        </div>
      </Card>
      <Card
        class="cursor-pointer !border-0 shadow-sm transition hover:shadow-md"
        @click="$router.push({ name: 'TicketList' })"
      >
        <div class="flex items-center gap-3">
          <div class="flex size-11 items-center justify-center rounded-lg bg-orange-50 text-xl dark:bg-orange-950">🎫</div>
          <div>
            <div class="text-2xl font-bold">
              {{ stats.tickets }}
              <span v-if="stats.slaBreached > 0" class="ml-1 text-sm font-medium text-red-500">
                {{ stats.slaBreached }} 单超期
              </span>
            </div>
            <div class="text-xs text-gray-400">我的进行中工单</div>
          </div>
          <div v-if="stats.slaBreached > 0" class="ml-auto size-2 animate-pulse rounded-full bg-red-500"></div>
        </div>
      </Card>
      <Card
        class="cursor-pointer !border-0 shadow-sm transition hover:shadow-md"
        @click="$router.push({ name: 'MyNotices' })"
      >
        <div class="flex items-center gap-3">
          <div class="flex size-11 items-center justify-center rounded-lg bg-green-50 text-xl dark:bg-green-950">🔔</div>
          <div>
            <div class="text-2xl font-bold">{{ stats.notices }}</div>
            <div class="text-xs text-gray-400">未读消息</div>
          </div>
        </div>
      </Card>
      <Card class="cursor-pointer !border-0 shadow-sm transition hover:shadow-md">
        <div class="flex items-center gap-3">
          <div class="flex size-11 items-center justify-center rounded-lg bg-violet-50 text-xl dark:bg-violet-950">📁</div>
          <div>
            <div class="text-2xl font-bold">{{ stats.projects }}</div>
            <div class="text-xs text-gray-400">参与项目</div>
          </div>
        </div>
      </Card>
    </div>

    <!-- 内容双栏 -->
    <div class="grid grid-cols-1 gap-4 lg:grid-cols-2">
      <!-- 我的任务 -->
      <Card :loading="loading" title="我的任务" class="!border-0 shadow-sm">
        <template #extra>
          <Button size="small" type="link" @click="$router.push({ name: 'RequirementTask' })">全部 →</Button>
        </template>
        <Empty v-if="myTasks.length === 0" description="暂无待办任务 🎉" />
        <div v-else class="flex flex-col divide-y">
          <div
            v-for="t in myTasks"
            :key="t.id"
            class="flex cursor-pointer items-center gap-3 py-2.5 transition hover:bg-gray-50 dark:hover:bg-gray-800/50"
            @click="$router.push({ name: 'RequirementTask' })"
          >
            <span class="size-1.5 rounded-full" :class="t.status === 1 ? 'bg-blue-500' : 'bg-gray-300'"></span>
            <span class="flex-1 truncate text-sm">{{ t.title }}</span>
            <Tag class="!m-0" :color="['red', 'orange', 'blue', 'default'][t.priority]">
              {{ labelOf(PRIORITY, t.priority) }}
            </Tag>
            <Tag class="!m-0" :color="TASK_STATUS_COLOR[t.status]">
              {{ labelOf(TASK_STATUS, t.status) }}
            </Tag>
          </div>
        </div>
      </Card>

      <!-- 我的工单 -->
      <Card :loading="loading" title="我的工单" class="!border-0 shadow-sm">
        <template #extra>
          <Button size="small" type="link" @click="$router.push({ name: 'TicketList' })">全部 →</Button>
        </template>
        <Empty v-if="myTickets.length === 0" description="暂无进行中工单 🎉" />
        <div v-else class="flex flex-col divide-y">
          <div
            v-for="t in myTickets"
            :key="t.id"
            class="flex cursor-pointer items-center gap-3 py-2.5 transition hover:bg-gray-50 dark:hover:bg-gray-800/50"
            @click="$router.push({ name: 'TicketList' })"
          >
            <span class="font-mono text-xs text-gray-400">{{ t.ticketNo }}</span>
            <span class="flex-1 truncate text-sm">{{ t.title }}</span>
            <Tag v-if="isSlaBreached(t)" color="red">SLA 超期</Tag>
            <Tag :color="TICKET_STATUS_COLOR[t.status]">
              {{ TICKET_STATUS_LABEL[t.status] }}
            </Tag>
          </div>
        </div>
      </Card>

      <!-- 最新消息 -->
      <Card :loading="loading" title="未读消息" class="!border-0 shadow-sm">
        <template #extra>
          <Button size="small" type="link" @click="$router.push({ name: 'MyNotices' })">全部 →</Button>
        </template>
        <Empty v-if="notices.length === 0" description="没有未读消息 🎉" />
        <div v-else class="flex flex-col divide-y">
          <div
            v-for="n in notices"
            :key="n.id"
            class="flex items-center gap-3 py-2.5"
          >
            <span class="size-1.5 rounded-full bg-red-500"></span>
            <span class="flex-1 truncate text-sm">{{ n.title }}</span>
            <span class="text-xs text-gray-400">{{ fmt(n.createdAt) }}</span>
            <Button size="small" type="link" @click="doRead(n.id)">已读</Button>
          </div>
        </div>
      </Card>

      <!-- 进行中迭代 -->
      <Card :loading="loading" title="进行中的迭代" class="!border-0 shadow-sm">
        <template #extra>
          <Button size="small" type="link" @click="$router.push({ name: 'RequirementIteration' })">全部 →</Button>
        </template>
        <Empty v-if="activeIterations.length === 0" description="暂无进行中的迭代" />
        <div v-else class="flex flex-col divide-y">
          <div
            v-for="i in activeIterations"
            :key="i.id"
            class="flex cursor-pointer items-center gap-3 py-2.5 transition hover:bg-gray-50 dark:hover:bg-gray-800/50"
            @click="$router.push({ name: 'RequirementIteration' })"
          >
            <span class="flex-1 truncate text-sm">
              <b class="mr-2">{{ i.projectName }}</b>{{ i.name }}
            </span>
            <span class="text-xs text-gray-400">
              {{ i.planStart ?? '-' }} ~ {{ i.planEnd ?? '-' }}
            </span>
            <Tag color="blue" class="!m-0">进行中</Tag>
          </div>
        </div>
      </Card>
    </div>
  </Page>
</template>
