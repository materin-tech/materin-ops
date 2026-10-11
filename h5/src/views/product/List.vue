<script setup>
import { showConfirmDialog, showSuccessToast } from 'vant';
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import { deleteProduct, listProducts } from '../../api';

const router = useRouter();
const list = ref([]);
const loading = ref(false);
const keyword = ref('');
const page = ref(1);
const finished = ref(false);

async function load() {
  loading.value = true;
  try {
    const res = await listProducts({ keyword: keyword.value || undefined });
    list.value = res;
    finished.value = true; // 产品量级小，一次加载
  } finally {
    loading.value = false;
  }
}

async function remove(item) {
  await showConfirmDialog({ title: '删除产品', message: `确认删除「${item.name}」？` });
  try {
    await deleteProduct(item.id);
    showSuccessToast('已删除');
    await load();
  } catch (e) {
    showFailToast?.(e.message);
  }
}

const STATUS = { 0: '停用', 1: '启用' };

onMounted(load);
import { showFailToast } from 'vant';
</script>

<template>
  <div class="page">
    <van-nav-bar title="产品管理" fixed />
    <van-search v-model="keyword" placeholder="搜索产品名称/编码" style="margin-top: 46px" @search="load" />
    <van-list v-model:loading="loading" :finished="finished" finished-text="">
      <van-cell
        v-for="p in list"
        :key="p.id"
        :title="p.name"
        :label="p.code"
        is-link
        @click="router.push(`/product/${p.id}`)"
      >
        <template #value>
          <van-tag :type="p.status === 1 ? 'success' : 'default'">{{ STATUS[p.status] || '启用' }}</van-tag>
        </template>
      </van-cell>
      <van-empty v-if="list.length === 0 && !loading" description="暂无产品" />
    </van-list>
    <van-button class="fab" type="primary" round icon="plus" @click="router.push('/product/edit')" />
  </div>
</template>

<style scoped>
.fab { position: fixed; right: 20px; bottom: 80px; width: 56px; height: 56px; box-shadow: 0 4px 12px rgba(22,119,255,0.4); }
</style>
