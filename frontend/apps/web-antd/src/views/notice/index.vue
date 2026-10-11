<script lang="ts" setup>
import type { NoticeItem } from '#/api/notice';

import { onMounted, reactive, ref } from 'vue';

import { $t } from '#/locales';

import { Page } from '@vben/common-ui';

import {
  Button,
  Empty,
  Input,
  message,
  Popconfirm,
  Select,
  Space,
  TabPane,
  Tabs,
  Tag,
} from 'ant-design-vue';

import {
  fetchMyNotices,
  markAllNoticesRead,
  markNoticeRead,
  removeNotice,
} from '#/api/notice';

const loading = ref(false);
const list = ref<NoticeItem[]>([]);
const total = ref(0);
const unreadCount = ref(0);
const activeTab = ref('all');
const query = reactive({ page: 1, pageSize: 20 });

async function load() {
  loading.value = true;
  try {
    const res = await fetchMyNotices({
      page: query.page,
      pageSize: query.pageSize,
      readFlag:
        activeTab.value === 'unread' ? 0 : activeTab.value === 'read' ? 1 : undefined,
    });
    list.value = res.list;
    total.value = res.total;
    unreadCount.value = res.unreadCount;
  } finally {
    loading.value = false;
  }
}

async function doRead(row: NoticeItem) {
  await markNoticeRead(row.id);
  await load();
}

async function doRemove(row: NoticeItem) {
  await removeNotice(row.id);
  message.success($t('notice.deleted'));
  await load();
}

async function doReadAll() {
  await markAllNoticesRead();
  message.success($t('notice.readAllDone'));
  await load();
}

const LEVEL_COLOR: Record<string, string> = {
  error: 'red',
  info: 'blue',
  warn: 'orange',
};

function fmtDate(value?: null | string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '';
}

onMounted(load);
</script>

<template>
  <Page :title="$t('notice.title')" :description="$t('notice.desc')">
    <div class="mb-4 flex items-center gap-2">
      <Tabs v-model:activeKey="activeTab" @change="() => { query.page = 1; load(); }">
        <TabPane key="all" :tab="$t('notice.all')" />
        <TabPane key="unread" :tab="`${$t('notice.unread')}${unreadCount > 0 ? `(${unreadCount})` : ''}`" />
        <TabPane key="read" :tab="$t('notice.read')" />
      </Tabs>
      <div class="flex-1"></div>
      <Button :disabled="unreadCount === 0" @click="doReadAll">{{ $t('notice.readAll') }}</Button>
    </div>

    <div v-if="list.length === 0 && !loading">
      <Empty :description="$t('notice.empty')" />
    </div>
    <div v-else class="flex flex-col gap-3">
      <div
        v-for="n in list"
        :key="n.id"
        class="flex items-start gap-3 rounded-lg border p-4"
        :class="n.readFlag === 1 ? 'opacity-60' : ''"
      >
        <Tag :color="LEVEL_COLOR[n.level] ?? 'default'">{{ n.level }}</Tag>
        <div class="flex-1">
          <div class="flex items-center gap-2">
            <span class="font-medium">{{ n.title }}</span>
            <Tag v-if="n.readFlag === 0" color="red">{{ $t('notice.unRead') }}</Tag>
            <Tag>{{ n.type }}</Tag>
          </div>
          <div v-if="n.content" class="mt-1 text-sm text-gray-500">{{ n.content }}</div>
          <div class="mt-1 text-xs text-gray-400">{{ fmtDate(n.createdAt) }}</div>
        </div>
        <Space>
          <Button v-if="n.readFlag === 0" size="small" @click="doRead(n)">{{ $t('notice.markRead') }}</Button>
          <Popconfirm :title="$t('notice.delConfirm')" @confirm="doRemove(n)">
            <Button danger size="small">{{ $t('notice.del') }}</Button>
          </Popconfirm>
        </Space>
      </div>
    </div>

    <div v-if="total > query.pageSize" class="mt-4 text-center">
      <Button
        :disabled="query.page * query.pageSize >= total"
        @click="() => { query.page++; load(); }"
      >
        加载更多
      </Button>
      <Input v-model:value="query.page" type="hidden" style="display: none" />
      <Select v-model:value="query.pageSize" style="display: none" />
    </div>
  </Page>
</template>
