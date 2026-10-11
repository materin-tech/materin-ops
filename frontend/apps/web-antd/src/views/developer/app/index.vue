<script lang="ts" setup>
import type { DevApp } from '#/api/developer';

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
  SelectOption,
  Space,
  Table,
  Tag,
  Typography,
} from 'ant-design-vue';

import { $t } from '#/locales';

import {
  createApp,
  deleteApp,
  pageApps,
  resetAppSecret,
  updateApp,
} from '#/api/developer';

const { hasAccessByCodes } = useAccess();

const loading = ref(false);
const list = ref<DevApp[]>([]);
const total = ref(0);
const query = reactive({
  keyword: '',
  page: 1,
  pageSize: 20,
});

async function load() {
  loading.value = true;
  try {
    const res = await pageApps({
      keyword: query.keyword || undefined,
      page: query.page,
      pageSize: query.pageSize,
    });
    list.value = res.list;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
}

// ---------- 新增 / 编辑 ----------
const editOpen = ref(false);
const editing = ref<null | DevApp>(null);
const created = ref<null | DevApp>(null);
const form = reactive({
  description: '',
  name: '',
  status: 1,
});

function openCreate() {
  editing.value = null;
  created.value = null;
  Object.assign(form, { description: '', name: '', status: 1 });
  editOpen.value = true;
}

function openEdit(row: DevApp) {
  editing.value = row;
  created.value = null;
  Object.assign(form, {
    description: row.description ?? '',
    name: row.name,
    status: row.status,
  });
  editOpen.value = true;
}

async function submit() {
  if (!form.name.trim()) {
    message.warning($t('developer.app.nameRequired'));
    return;
  }
  if (editing.value) {
    await updateApp(editing.value.id, { ...form });
    message.success($t('developer.app.updated'));
  } else {
    created.value = await createApp({ ...form });
    message.success($t('developer.app.created'));
  }
  if (editing.value) {
    editOpen.value = false;
  }
  await load();
}

async function doResetSecret(row: DevApp) {
  const fresh = await resetAppSecret(row.id);
  Modal.info({
    content: `${$t('developer.app.newSecret')}${fresh.appSecret}`,
    title: $t('developer.app.resetTitle'),
  });
  await load();
}

async function remove(row: DevApp) {
  await deleteApp(row.id);
  message.success($t('developer.app.deleted'));
  await load();
}

const columns = [
  { title: 'ID', dataIndex: 'id', width: 70 },
  { title: $t('developer.app.appName'), dataIndex: 'name', width: 180 },
  { title: 'AppKey', dataIndex: 'appKey', width: 220 },
  { title: 'AppSecret', dataIndex: 'appSecret', width: 240 },
  { title: $t('developer.app.status'), dataIndex: 'status', width: 90 },
  { title: $t('developer.app.descLabel'), dataIndex: 'description' },
  { title: $t('developer.app.action'), width: 220, key: 'action' },
];

onMounted(load);
</script>

<template>
  <Page :title="$t('developer.app.menu')" :description="$t('developer.app.desc')">
    <div class="mb-4 flex items-center gap-2">
      <Input
        v-model:value="query.keyword"
        placeholder="名称/AppKey"
        style="width: 200px"
        allow-clear
        @pressEnter="load"
      />
      <Button type="primary" @click="load">{{ $t('developer.app.query') }}</Button>
      <div class="flex-1"></div>
      <Button
        v-if="hasAccessByCodes(['developer:app:add'])"
        type="primary"
        @click="openCreate"
      >{{ $t('developer.app.add') }}</Button>
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
        <template v-if="column.dataIndex === 'status'">
          <Tag :color="record.status === 1 ? 'green' : 'default'">
            {{ record.status === 1 ? $t('developer.app.enabled') : $t('developer.app.disabled') }}
          </Tag>
        </template>
        <template v-else-if="column.dataIndex === 'appSecret'">
          <Typography.Text copyable :content="record.appSecret">
            {{ record.appSecret.slice(0, 10) }}…
          </Typography.Text>
        </template>
        <template v-else-if="column.key === 'action'">
          <Space>
            <Button
              v-if="hasAccessByCodes(['developer:app:edit'])"
              size="small"
              @click="openEdit(record as DevApp)"
            >{{ $t('developer.app.edit') }}
            </Button>
            <Popconfirm
              v-if="hasAccessByCodes(['developer:app:edit'])"
              :title="$t('developer.app.resetConfirm')"
              @confirm="doResetSecret(record as DevApp)"
            >
              <Button size="small">{{ $t('developer.app.resetSecret') }}</Button>
            </Popconfirm>
            <Popconfirm
              v-if="hasAccessByCodes(['developer:app:delete'])"
              :title="$t('developer.app.delConfirm')"
              @confirm="remove(record as DevApp)"
            >
              <Button danger size="small">{{ $t('developer.app.del') }}</Button>
            </Popconfirm>
          </Space>
        </template>
      </template>
    </Table>

    <!-- 新增/编辑应用 -->
    <Modal
      v-model:open="editOpen"
      :title="editing ? $t('developer.app.editApp') : $t('developer.app.addApp')"
      :close-on-click-modal="false"
    >
      <template v-if="created">
        <div class="mb-4 rounded border border-green-300 bg-green-50 p-3">
          <div class="mb-1 font-medium">{{ $t('developer.app.saved') }}</div>
          <div>AppKey：{{ created.appKey }}</div>
          <div>AppSecret：{{ created.appSecret }}</div>
        </div>
        <div class="text-center">
          <Button type="primary" @click="editOpen = false">{{ $t('developer.app.iSaved') }}</Button>
        </div>
      </template>
      <Form v-else layout="vertical">
        <FormItem :label="$t('developer.app.appName')">
          <Input v-model:value="form.name" :placeholder="$t('developer.app.namePh')" />
        </FormItem>
        <FormItem :label="$t('developer.app.descLabel')">
          <Textarea v-model:value="form.description" :rows="3" />
        </FormItem>
        <FormItem :label="$t('developer.app.status')">
          <Select v-model:value="form.status">
            <SelectOption :value="1">{{ $t('developer.app.enabled') }}</SelectOption>
            <SelectOption :value="0">{{ $t('developer.app.disabled') }}</SelectOption>
          </Select>
        </FormItem>
      </Form>
      <template #footer>
        <Button @click="editOpen = false">{{ $t('developer.app.cancel') }}</Button>
        <Button type="primary" @click="submit">{{ $t('developer.app.save') }}</Button>
      </template>
    </Modal>
  </Page>
</template>
