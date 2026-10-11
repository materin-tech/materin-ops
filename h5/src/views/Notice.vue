<script setup>
import { showSuccessToast } from 'vant';
import { onMounted, ref } from 'vue';

import { fetchMyNotices, markNoticeRead } from '../api';

const list = ref([]);
const loading = ref(false);
const page = ref(1);
const finished = ref(false);

async function load() {
  loading.value = true;
  try {
    const res = await fetchMyNotices({ page: page.value, pageSize: 15 });
    list.value = page.value === 1 ? res.list : list.value.concat(res.list);
    finished.value = list.value.length >= Number(res.total);
    page.value++;
  } finally {
    loading.value = false;
  }
}

async function read(n) {
  await markNoticeRead(n.id);
  showSuccessToast('已标为已读');
  page.value = 1;
  finished.value = false;
  await load();
}

function fmt(v) {
  return v ? String(v).replace('T', ' ').slice(0, 16) : '';
}

onMounted(load);
</script>

<template>
  <div class="page">
    <van-nav-bar title="我的消息" fixed />
    <van-list
      v-model:loading="loading"
      :finished="finished"
      finished-text="没有更多了"
      style="margin-top: 50px"
      @load="load"
    >
      <van-swipe-cell v-for="n in list" :key="n.id">
        <van-cell :title="n.title" :label="n.content || ''">
          <template #value>
            <van-tag v-if="n.readFlag === 0" type="danger">未读</van-tag>
          </template>
          <template #label>
            <div class="n-content">{{ n.content }}</div>
            <div class="n-time">{{ fmt(n.createdAt) }}</div>
          </template>
        </van-cell>
        <template #right>
          <van-button square type="primary" text="已读" style="height: 100%" @click="read(n)" />
        </template>
      </van-swipe-cell>
      <van-empty v-if="list.length === 0 && !loading" description="暂无消息" />
    </van-list>
  </div>
</template>

<style scoped>
.n-content { color: #666; font-size: 13px; margin-top: 2px; }
.n-time { color: #bbb; font-size: 12px; margin-top: 2px; }
</style>
