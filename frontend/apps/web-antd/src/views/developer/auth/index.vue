<script lang="ts" setup>
import type { DevApp, DevApi } from '#/api/developer';

import { onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useAccess } from '@vben/access';

import { Button, Card, message, Select, Space, Tag, Tree } from 'ant-design-vue';

import { $t } from '#/locales';

import { appAuthDetail, pageApps, saveAppAuth } from '#/api/developer';

const { hasAccessByCodes } = useAccess();

const loading = ref(false);
const saving = ref(false);
const apps = ref<DevApp[]>([]);
const currentAppId = ref<number>(0);
const apis = ref<DevApi[]>([]);
const authorizedIds = ref<number[]>([]);
const checkedKeys = ref<number[]>([]);
const expandedKeys = ref<number[]>([]);
const treeData = ref<any[]>([]);

async function loadApps() {
  const res = await pageApps({ page: 1, pageSize: 100 });
  apps.value = res.list;
  if (apps.value.length > 0 && currentAppId.value === 0) {
    currentAppId.value = apps.value[0]!.id;
  }
  if (currentAppId.value) {
    await loadAuth();
  }
}

async function loadAuth() {
  if (!currentAppId.value) return;
  loading.value = true;
  try {
    const detail = await appAuthDetail(currentAppId.value);
    apis.value = detail.apis;
    authorizedIds.value = detail.authorizedApiIds;
    checkedKeys.value = [...detail.authorizedApiIds];
    treeData.value = groupByTag(detail.apis);
    expandedKeys.value = treeData.value.map((n) => n.id);
  } finally {
    loading.value = false;
  }
}

/** 按接口分组构树（组节点用负数 ID 避免与接口 ID 冲突） */
function groupByTag(list: DevApi[]) {
  const groups = new Map<string, DevApi[]>();
  for (const api of list) {
    const tag = api.tag || 'default';
    if (!groups.has(tag)) groups.set(tag, []);
    groups.get(tag)!.push(api);
  }
  let seq = -1;
  return [...groups.entries()].map(([tag, children]) => ({
    id: seq--,
    key: `group-${tag}`,
    name: `${tag}（${children.length}）`,
    children: children.map((c) => ({ ...c, name: `${c.method} ${c.path}` })),
  }));
}

function collectAllApiIds(nodes: any[], acc: number[] = []): number[] {
  for (const n of nodes) {
    if (n.id > 0) acc.push(n.id);
    if (n.children?.length) collectAllApiIds(n.children, acc);
  }
  return acc;
}

async function save() {
  saving.value = true;
  try {
    await saveAppAuth(currentAppId.value, checkedKeys.value.filter((id) => id > 0));
    message.success($t('developer.auth.saved'));
    await loadAuth();
  } finally {
    saving.value = false;
  }
}

const METHOD_COLOR: Record<string, string> = {
  DELETE: 'red',
  GET: 'blue',
  PATCH: 'purple',
  POST: 'green',
  PUT: 'orange',
};

onMounted(loadApps);
</script>

<template>
  <Page :title="$t('developer.auth.menu')" :description="$t('developer.auth.desc')">
    <Card :loading="loading">
      <div class="mb-4 flex items-center gap-2">
        <span>{{ $t('developer.auth.app') }}</span>
        <Select
          v-model:value="currentAppId"
          style="width: 260px"
          :placeholder="$t('developer.auth.chooseApp')"
          :options="apps.map((a) => ({ label: a.name, value: a.id }))"
          @change="loadAuth"
        />
        <div class="flex-1"></div>
        <Space>
          <Button
            size="small"
            @click="checkedKeys = collectAllApiIds(treeData)"
          >{{ $t('developer.auth.selectAll') }}
          </Button>
          <Button size="small" @click="checkedKeys = []">{{ $t('developer.auth.clear') }}</Button>
          <Button
            v-if="hasAccessByCodes(['developer:auth:grant'])"
            :loading="saving"
            type="primary"
            @click="save"
          >{{ $t('developer.auth.save') }}
          </Button>
        </Space>
      </div>

      <Tree
        v-if="treeData.length > 0"
        v-model:checkedKeys="checkedKeys"
        v-model:expandedKeys="expandedKeys"
        checkable
        :tree-data="treeData"
        :field-names="{ children: 'children', title: 'name', key: 'id' }"
      >
        <template #title="{ dataRef }">
          <Space>
            <Tag v-if="dataRef.id > 0" :color="METHOD_COLOR[dataRef.method] ?? 'default'">
              {{ dataRef.method }}
            </Tag>
            <span>{{ dataRef.name }}</span>
            <Tag v-if="dataRef.id > 0" :color="dataRef.status === 1 ? 'green' : 'default'">
              {{ dataRef.status === 1 ? $t('developer.auth.open') : $t('developer.auth.notOpen') }}
            </Tag>
          </Space>
        </template>
      </Tree>
      <div v-else class="text-gray-400">
        {{ $t('developer.auth.noApis') }}
      </div>
    </Card>
  </Page>
</template>
