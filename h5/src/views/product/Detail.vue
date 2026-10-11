<script setup>
import { showConfirmDialog, showSuccessToast } from 'vant';
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { deleteProduct, productDetail, productModuleTree } from '../../api';

const route = useRoute();
const router = useRouter();
const item = ref(null);
const modules = ref([]);

async function load() {
  item.value = await productDetail(route.params.id);
  try {
    modules.value = await productModuleTree(route.params.id);
  } catch {
    modules.value = [];
  }
}

async function remove() {
  await showConfirmDialog({ title: '删除产品', message: `确认删除「${item.value.name}」？` });
  try {
    await deleteProduct(route.params.id);
    showSuccessToast('已删除');
    router.replace('/product');
  } catch (e) {
    showFailToast?.(e.message || '删除失败');
  }
}

import { showFailToast } from 'vant';
onMounted(load);
</script>

<template>
  <div class="page" v-if="item">
    <van-nav-bar title="产品详情" left-arrow fixed @click-left="router.back()">
      <template #right>
        <span style="color: #ee0a24" @click="remove">删除</span>
      </template>
    </van-nav-bar>
    <div style="margin-top: 56px">
      <van-cell-group inset class="mt">
        <van-cell title="名称" :value="item.name" />
        <van-cell title="编码" :value="item.code" />
        <van-cell title="状态">
          <template #value>
            <van-tag :type="item.status === 1 ? 'success' : 'default'">
              {{ item.status === 1 ? '启用' : '停用' }}
            </van-tag>
          </template>
        </van-cell>
        <van-cell title="描述" :label="item.description || '-'" />
      </van-cell-group>

      <van-cell-group inset title="模块树" class="mt mb">
        <van-cell v-if="modules.length === 0" title="暂无模块（可在 PC 端维护）" />
        <van-tree-select v-else :items="flatten(modules)" />
      </van-cell-group>

      <div class="p-4">
        <van-button block round type="primary" @click="router.push(`/product/edit/${item.id}`)">
          编辑产品
        </van-button>
      </div>
    </div>
  </div>
</template>

<script>
// 模块树拍平为 vant tree-select 结构
function flatten(nodes, acc = []) {
  for (const n of nodes) {
    acc.push({ text: n.name, id: n.id, children: n.children?.length ? flatten(n.children) : undefined });
  }
  return acc;
}
export default { name: 'ProductDetail' };
</script>

<style scoped>
.mt { margin-top: 12px; }
.mb { margin-bottom: 20px; }
</style>
