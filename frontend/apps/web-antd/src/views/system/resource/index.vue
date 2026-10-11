<script lang="ts" setup>
import type { ResourceItem } from '#/api/system/resource';

import { onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useAccess } from '@vben/access';

import {
  Button,
  Input,
  message,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  Upload,
} from 'ant-design-vue';

import {
  deleteResource,
  downloadUrl,
  pageResources,
  uploadResource,
} from '#/api/system/resource';
import { $t } from '#/locales';

const { hasAccessByCodes } = useAccess();

const loading = ref(false);
const list = ref<ResourceItem[]>([]);
const total = ref(0);
const query = reactive({
  keyword: '',
  page: 1,
  pageSize: 20,
  storageType: undefined as undefined | string,
});
const uploading = ref(false);

async function load() {
  loading.value = true;
  try {
    const res = await pageResources({
      keyword: query.keyword || undefined,
      page: query.page,
      pageSize: query.pageSize,
      storageType: query.storageType,
    });
    list.value = res.list;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
}

async function customRequest(options: any) {
  uploading.value = true;
  try {
    await uploadResource(options.file as File);
    message.success($t('resource.uploaded'));
    await load();
    options.onSuccess?.(options.file);
  } catch (e) {
    options.onError?.(e as Error);
  } finally {
    uploading.value = false;
  }
}

async function remove(row: ResourceItem) {
  await deleteResource(row.id);
  message.success($t('resource.deleted'));
  await load();
}

function fmtSize(size: number) {
  if (!size) return '-';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  if (size < 1024 * 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)} MB`;
  return `${(size / 1024 / 1024 / 1024).toFixed(1)} GB`;
}

function fmtDate(value?: null | string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-';
}

const columns = [
  { title: 'ID', dataIndex: 'id', width: 70 },
  { title: $t('resource.name'), dataIndex: 'originalName' },
  { title: $t('resource.type'), dataIndex: 'contentType', width: 160 },
  { title: $t('resource.size'), dataIndex: 'size', width: 110 },
  { title: $t('resource.storage'), dataIndex: 'storageType', width: 100 },
  { title: $t('resource.time'), dataIndex: 'createdAt', width: 170 },
  { title: $t('resource.action'), width: 170, key: 'action' },
];

onMounted(load);
</script>

<template>
  <Page :title="$t('resource.title')" :description="$t('resource.desc')">
    <div class="mb-4 flex flex-wrap items-center gap-2">
      <Input
        v-model:value="query.keyword"
        :placeholder="$t('resource.keywordPh')"
        style="width: 200px"
        allow-clear
        @press-enter="load"
      />
      <Select
        v-model:value="query.storageType"
        :placeholder="$t('resource.storage')"
        style="width: 130px"
        allow-clear
        :options="['local', 'obs', 'minio'].map((s) => ({ label: s, value: s }))"
        @change="load"
      />
      <Button type="primary" @click="load">{{ $t('resource.query') }}</Button>
      <div class="flex-1"></div>
      <Upload
        v-if="hasAccessByCodes(['resource:upload'])"
        :custom-request="customRequest"
        :show-upload-list="false"
        multiple
      >
        <Button :loading="uploading" type="primary">
          {{ $t('resource.upload') }}
        </Button>
      </Upload>
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
        <template v-if="column.dataIndex === 'storageType'">
          <Tag :color="record.storageType === 'local' ? 'blue' : 'purple'">
            {{ record.storageType }}
          </Tag>
        </template>
        <template v-else-if="column.dataIndex === 'size'">
          {{ fmtSize(record.size) }}
        </template>
        <template v-else-if="column.dataIndex === 'createdAt'">
          {{ fmtDate(record.createdAt) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <Space>
            <a :href="downloadUrl((record as ResourceItem).id)" target="_blank">
              <Button size="small">{{ $t('resource.download') }}</Button>
            </a>
            <Popconfirm
              v-if="hasAccessByCodes(['resource:delete'])"
              :title="$t('resource.delConfirm')"
              @confirm="remove(record as ResourceItem)"
            >
              <Button danger size="small">{{ $t('resource.delete') }}</Button>
            </Popconfirm>
          </Space>
        </template>
      </template>
    </Table>
  </Page>
</template>
