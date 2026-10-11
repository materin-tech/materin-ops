<script lang="ts" setup>
import type { DevApi } from '#/api/developer';

import { onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useAccess } from '@vben/access';

import {
  Button,
  Form,
  FormItem,
  Input,
  message,
  Modal,
  Popconfirm,
  Select,
  Space,
  Switch,
  Table,
  Tag,
} from 'ant-design-vue';

import { $t } from '#/locales';

import {
  createApi,
  deleteApi,
  pageApis,
  syncApis,
  updateApi,
} from '#/api/developer';

const { hasAccessByCodes } = useAccess();

const loading = ref(false);
const syncing = ref(false);
const list = ref<DevApi[]>([]);
const total = ref(0);
const query = reactive({
  keyword: '',
  method: undefined as undefined | string,
  page: 1,
  pageSize: 20,
  status: undefined as undefined | number,
});

async function load() {
  loading.value = true;
  try {
    const res = await pageApis({
      keyword: query.keyword || undefined,
      method: query.method,
      page: query.page,
      pageSize: query.pageSize,
      status: query.status,
    });
    list.value = res.list;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
}

function reset() {
  Object.assign(query, { keyword: '', method: undefined, page: 1, status: undefined });
  load();
}

async function doSync() {
  syncing.value = true;
  try {
    const res = await syncApis();
    message.success($t('developer.api.synced', { c: res.created, u: res.updated }));
    await load();
  } finally {
    syncing.value = false;
  }
}

async function toggleOpen(row: DevApi, open: boolean) {
  await updateApi(row.id, { status: open ? 1 : 0 });
  message.success(open ? $t('developer.api.opened') : $t('developer.api.closedMsg'));
  await load();
}

async function remove(row: DevApi) {
  await deleteApi(row.id);
  message.success('已删除');
  await load();
}

// ---------- 手工登记 ----------
const editOpen = ref(false);
const editing = ref<null | DevApi>(null);
const form = reactive({
  method: 'GET',
  name: '',
  path: '',
  tag: 'manual',
});

function openCreate() {
  editing.value = null;
  Object.assign(form, { method: 'GET', name: '', path: '', tag: 'manual' });
  editOpen.value = true;
}

function openEdit(row: DevApi) {
  editing.value = row;
  Object.assign(form, {
    method: row.method,
    name: row.name ?? '',
    path: row.path,
    tag: row.tag ?? '',
  });
  editOpen.value = true;
}

async function submit() {
  if (!form.path.trim()) {
    message.warning($t('developer.api.pathRequired'));
    return;
  }
  if (editing.value) {
    await updateApi(editing.value.id, { name: form.name, tag: form.tag });
    message.success($t('developer.api.updated'));
  } else {
    await createApi({ ...form });
    message.success($t('developer.api.registered'));
  }
  editOpen.value = false;
  await load();
}

const METHOD_COLOR: Record<string, string> = {
  DELETE: 'red',
  GET: 'blue',
  PATCH: 'purple',
  POST: 'green',
  PUT: 'orange',
};

const columns = [
  { title: 'ID', dataIndex: 'id', width: 70 },
  { title: '方法', dataIndex: 'method', width: 90 },
  { title: $t('developer.api.path'), dataIndex: 'path' },
  { title: $t('developer.api.name'), dataIndex: 'name', width: 200 },
  { title: $t('developer.api.tag'), dataIndex: 'tag', width: 140 },
  { title: $t('developer.api.source'), dataIndex: 'source', width: 90 },
  { title: '开放', dataIndex: 'status', width: 90 },
  { title: $t('developer.api.action'), width: 160, key: 'action' },
];

onMounted(load);
</script>

<template>
  <Page :title="$t('developer.api.menu')" :description="$t('developer.api.desc')">
    <div class="mb-4 flex flex-wrap items-center gap-2">
      <Select
        v-model:value="query.method"
        placeholder="方法"
        style="width: 100px"
        allow-clear
        :options="['GET', 'POST', 'PUT', 'DELETE', 'PATCH'].map((m) => ({ label: m, value: m }))"
        @change="load"
      />
      <Select
        v-model:value="query.status"
        placeholder="开放状态"
        style="width: 120px"
        allow-clear
        :options="[
          { label: '开放', value: 1 },
          { label: '关闭', value: 0 },
        ]"
        @change="load"
      />
      <Input
        v-model:value="query.keyword"
        placeholder="路径/名称"
        style="width: 180px"
        allow-clear
        @pressEnter="load"
      />
      <Button type="primary" @click="load">{{ $t('developer.api.query') }}</Button>
      <Button @click="reset">{{ $t('developer.api.reset') }}</Button>
      <div class="flex-1"></div>
      <Button
        v-if="hasAccessByCodes(['developer:api:sync'])"
        :loading="syncing"
        type="primary"
        @click="doSync"
      >{{ $t('developer.api.sync') }}
      </Button>
      <Button
        v-if="hasAccessByCodes(['developer:api:edit'])"
        @click="openCreate"
      >{{ $t('developer.api.manual') }}
      </Button>
    </div>

    <Table
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="{
        current: query.page,
        onChange: (p: number, ps: number) => {
          query.page = p;
          query.pageSize = ps;
          load();
        },
        pageSize: query.pageSize,
        showSizeChanger: true,
        total,
      }"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'method'">
          <Tag :color="METHOD_COLOR[record.method] ?? 'default'">{{ record.method }}</Tag>
        </template>
        <template v-else-if="column.dataIndex === 'source'">
          <Tag>{{ record.source === 'sync' ? $t('developer.api.srcSync') : $t('developer.api.srcManual') }}</Tag>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <Switch
            :checked="record.status === 1"
            checked-children="开放"
            un-checked-children="关闭"
            @change="(v: any) => toggleOpen(record as DevApi, !!v)"
          />
        </template>
        <template v-else-if="column.key === 'action'">
          <Space>
            <Button
              v-if="hasAccessByCodes(['developer:api:edit'])"
              size="small"
              @click="openEdit(record as DevApi)"
            >{{ $t('developer.api.edit') }}
            </Button>
            <Popconfirm
              v-if="hasAccessByCodes(['developer:api:edit'])"
              :title="$t('developer.api.delConfirm')"
              @confirm="remove(record as DevApi)"
            >
              <Button danger size="small">{{ $t('developer.api.del') }}</Button>
            </Popconfirm>
          </Space>
        </template>
      </template>
    </Table>

    <!-- 手工登记/编辑接口 -->
    <Modal v-model:open="editOpen" :title="editing ? $t('developer.api.editTitle') : $t('developer.api.manualTitle')">
      <Form layout="vertical">
        <FormItem :label="$t('developer.api.httpMethod')">
          <Select
            v-model:value="form.method"
            :disabled="!!editing"
            :options="['GET', 'POST', 'PUT', 'DELETE', 'PATCH'].map((m) => ({ label: m, value: m }))"
          />
        </FormItem>
        <FormItem :label="$t('developer.api.path')">
          <Input v-model:value="form.path" :disabled="!!editing" placeholder="/requirement/products" />
        </FormItem>
        <FormItem :label="$t('developer.api.name')">
          <Input v-model:value="form.name" />
        </FormItem>
        <FormItem :label="$t('developer.api.tag')">
          <Input v-model:value="form.tag" :disabled="!!editing && editing.source === 'sync'" />
        </FormItem>
      </Form>
      <template #footer>
        <Button @click="editOpen = false">{{ $t('developer.api.cancel') }}</Button>
        <Button type="primary" @click="submit">{{ $t('developer.api.save') }}</Button>
      </template>
    </Modal>
  </Page>
</template>
