<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import { pageTickets } from '../../api';

const router = useRouter();
const active = ref(0);
const list = ref([]);
const page = ref(1);
const finished = ref(false);
const loading = ref(false);
const TABS = ['全部', '待受理', '处理中', '待确认', '已解决', '已关闭'];

async function load() {
  loading.value = true;
  try {
    const res = await pageTickets({
      page: page.value,
      pageSize: 15,
      status: active.value === 0 ? undefined : active.value - 1,
    });
    list.value = page.value === 1 ? res.list : list.value.concat(res.list);
    finished.value = list.value.length >= Number(res.total);
    page.value++;
  } finally {
    loading.value = false;
  }
}

function onTab() {
  page.value = 1;
  finished.value = false;
  list.value = [];
  load();
}

const STATUS = ['待受理', '处理中', '待确认', '已解决', '已关闭'];
const STATUS_COLOR = ['default', 'primary', 'warning', 'success', 'default'];
const PRIORITY = ['P1', 'P2', 'P3', 'P4'];
const PRIORITY_COLOR = ['danger', 'warning', 'primary', 'default'];
</script>

<template>
  <div class="page">
    <van-nav-bar title="工单列表" fixed />
    <van-tabs v-model:active="active" sticky :offset-top="46" @change="onTab">
      <van-tab v-for="(t, i) in TABS" :key="t" :title="t" />
    </van-tabs>
    <van-list
      v-model:loading="loading"
      :finished="finished"
      finished-text="没有更多了"
      @load="load"
    >
      <van-cell
        v-for="t in list"
        :key="t.id"
        is-link
        @click="router.push(`/ticket/${t.id}`)"
      >
        <template #title>
          <div class="t-title">{{ t.title }}</div>
          <div class="t-sub">
            <span class="no">{{ t.ticketNo }}</span>
            <van-tag :type="PRIORITY_COLOR[t.priority]" class="ml">{{ PRIORITY[t.priority] }}</van-tag>
            <van-tag :type="STATUS_COLOR[t.status]" class="ml">{{ STATUS[t.status] }}</van-tag>
          </div>
        </template>
      </van-cell>
    </van-list>
    <van-button
      class="fab"
      type="primary"
      round
      icon="plus"
      @click="router.push('/ticket/create')"
    />
  </div>
</template>

<style scoped>
.t-title { font-weight: 500; }
.t-sub { display: flex; align-items: center; gap: 6px; margin-top: 4px; }
.no { color: #999; font-size: 12px; font-family: monospace; }
.ml { margin-left: 0; }
.fab { position: fixed; right: 20px; bottom: 80px; width: 56px; height: 56px; box-shadow: 0 4px 12px rgba(22,119,255,0.4); }
</style>
