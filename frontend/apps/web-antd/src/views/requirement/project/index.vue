<script lang="ts" setup>
import type { Project } from '#/api/requirement';

import { onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useAccess } from '@vben/access';

import {
  Button,
  Descriptions,
  DescriptionsItem,
  Drawer,

  DatePicker,
  Form,
  FormItem,
  Input,
  message,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
} from 'ant-design-vue';

import { $t } from '#/locales';
import RichEditor from '#/components/rich-editor/RichEditor.vue';
import dayjs from 'dayjs';

import {
  createProject,
  deleteProject,
  pageProjects,
  productOptions,
  projectProducts,
  updateProject,
} from '#/api/requirement';

import { PROJECT_STATUS, labelOf } from '../shared';

import type { Product } from '#/api/requirement';

const { hasAccessByCodes } = useAccess();

const loading = ref(false);
const list = ref<Project[]>([]);
const total = ref(0);
const query = reactive({
  keyword: '',
  page: 1,
  pageSize: 20,
  status: undefined as undefined | number,
});
const products = ref<Product[]>([]);

async function load() {
  loading.value = true;
  try {
    const res = await pageProjects({
      keyword: query.keyword || undefined,
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
  Object.assign(query, { keyword: '', page: 1, status: undefined });
  load();
}

// ---------- 新增 / 编辑 ----------
const editOpen = ref(false);
const editing = ref<null | Project>(null);
const form = reactive({
  code: '',
  description: '',
  endDate: undefined as undefined | string,
  name: '',
  productIds: [] as number[],
  startDate: undefined as undefined | string,
  status: 1,
});

function toDay(value?: null | string) {
  return value ? dayjs(value) : undefined;
}

async function openCreate() {
  editing.value = null;
  Object.assign(form, {
    code: '',
    description: '',
    endDate: undefined,
    name: '',
    productIds: [],
    startDate: undefined,
    status: 1,
  });
  editOpen.value = true;
}

async function openEdit(row: Project) {
  editing.value = row;
  const ids = await projectProducts(row.id);
  Object.assign(form, {
    code: row.code,
    description: row.description ?? '',
    endDate: row.endDate ?? undefined,
    name: row.name,
    productIds: ids,
    startDate: row.startDate ?? undefined,
    status: row.status,
  });
  editOpen.value = true;
}

async function submit() {
  if (!form.name.trim() || (!editing.value && !form.code.trim())) {
    message.warning($t('requirement.project.requiredMsg'));
    return;
  }
  const payload = {
    description: form.description,
    endDate: form.endDate,
    name: form.name,
    productIds: form.productIds,
    startDate: form.startDate,
    status: form.status,
  };
  if (editing.value) {
    await updateProject(editing.value.id, payload);
    message.success($t('requirement.project.updated'));
  } else {
    await createProject({ ...payload, code: form.code });
    message.success($t('requirement.project.created'));
  }
  editOpen.value = false;
  await load();
}

async function remove(row: Project) {
  await deleteProject(row.id);
  message.success($t('requirement.project.deleted'));
  await load();
}

const detailOpen = ref(false);
const detailRow = ref<null | Project>(null);

function openDetail(row: Project) {
  detailRow.value = row;
  detailOpen.value = true;
}

const columns = [
  { title: 'ID', dataIndex: 'id', width: 70 },
  { title: $t('requirement.project.name'), dataIndex: 'name' },
  { title: $t('requirement.project.code'), dataIndex: 'code', width: 140 },
  { title: $t('requirement.project.products'), dataIndex: 'productNames', width: 220 },
  { title: $t('requirement.project.status'), dataIndex: 'status', width: 90 },
  { title: $t('requirement.project.dates'), width: 200, key: 'dates' },
  { title: $t('requirement.project.action'), width: 200, key: 'action' },
];

const pagination = {
  onChange: (p: number, ps: number) => {
    query.page = p;
    query.pageSize = ps;
    load();
  },
  showSizeChanger: true,
};

onMounted(async () => {
  products.value = await productOptions();
  await load();
});
</script>

<template>
  <Page :title="$t('requirement.project.menu')" :description="$t('requirement.project.desc')">
    <div class="mb-4 flex flex-wrap items-center gap-2">
      <Select
        v-model:value="query.status"
        placeholder="状态"
        style="width: 120px"
        allow-clear
        :options="PROJECT_STATUS.map((s) => ({ label: s.label, value: s.value }))"
        @change="load"
      />
      <Input
        v-model:value="query.keyword"
        placeholder="名称/编码"
        style="width: 180px"
        allow-clear
        @pressEnter="load"
      />
      <Button type="primary" @click="load">{{ $t('requirement.project.query') }}</Button>
      <Button @click="reset">{{ $t('requirement.project.reset') }}</Button>
      <div class="flex-1"></div>
      <Button
        v-if="hasAccessByCodes(['requirement:project:add'])"
        type="primary"
        @click="openCreate"
      >{{ $t('requirement.project.add') }}</Button>
    </div>

    <Table
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="{ ...pagination, current: query.page, pageSize: query.pageSize, total }"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'status'">
          <Tag :color="['default', 'green', 'blue'][record.status]">
            {{ labelOf(PROJECT_STATUS, record.status) }}
          </Tag>
        </template>
        <template v-else-if="column.dataIndex === 'productNames'">
          <Space wrap>
            <Tag v-for="name in record.productNames" :key="name">{{ name }}</Tag>
          </Space>
        </template>
        <template v-else-if="column.key === 'dates'">
          {{ record.startDate ?? '-' }} ~ {{ record.endDate ?? '-' }}
        </template>
        <template v-else-if="column.key === 'action'">
          <Space>
            <Button size="small" @click="openDetail(record as Project)">{{ $t('requirement.project.detail') }}</Button>            <Button
              v-if="hasAccessByCodes(['requirement:project:edit'])"
              size="small"
              @click="openEdit(record as Project)"
            >{{ $t('requirement.project.edit') }}
            </Button>
            <Popconfirm
              v-if="hasAccessByCodes(['requirement:project:delete'])"
              :title="$t('requirement.project.delConfirm')"
              @confirm="remove(record as Project)"
            >
              <Button danger size="small">{{ $t('requirement.project.delete') }}</Button>
            </Popconfirm>
          </Space>
        </template>
      </template>
    </Table>

    <!-- 新增/编辑项目 -->
    <Modal v-model:open="editOpen" :title="editing ? $t('requirement.project.edit') : $t('requirement.project.add')" width="640">
      <Form layout="vertical">
        <FormItem :label="$t('requirement.project.name')">
          <Input v-model:value="form.name" />
        </FormItem>
        <FormItem :label="$t('requirement.project.code')">
          <Input v-model:value="form.code" :disabled="!!editing" :placeholder="$t('requirement.project.codePh')" />
        </FormItem>
        <FormItem :label="$t('requirement.project.description')">
          <RichEditor v-model:value="form.description" :height="200" />
        </FormItem>
        <FormItem :label="$t('requirement.project.products')">
          <Select
            v-model:value="form.productIds"
            mode="multiple"
            :placeholder="$t('requirement.project.chooseProduct')"
            :options="products.map((p) => ({ label: p.name, value: p.id }))"
          />
        </FormItem>
        <FormItem :label="$t('requirement.project.dates')">
          <Space>
            <DatePicker
              :value="toDay(form.startDate)"
              @change="(_, ds) => (form.startDate = ds || undefined)"
            />
            <DatePicker
              :value="toDay(form.endDate)"
              @change="(_, ds) => (form.endDate = ds || undefined)"
            />
          </Space>
        </FormItem>
        <FormItem :label="$t('requirement.project.status')">
          <Select
            v-model:value="form.status"
            :options="PROJECT_STATUS.map((s) => ({ label: s.label, value: s.value }))"
          />
        </FormItem>
      </Form>
      <template #footer>
        <Button @click="editOpen = false">{{ $t('requirement.project.cancel') }}</Button>
        <Button type="primary" @click="submit">{{ $t('requirement.project.save') }}</Button>
      </template>
    </Modal>
    <!-- 项目详情 -->
    <Drawer v-model:open="detailOpen" :title="detailRow?.name ?? $t('requirement.project.detailTitle')" width="480">
      <Descriptions :column="1" bordered size="small">
        <DescriptionsItem label="ID">{{ detailRow?.id }}</DescriptionsItem>
        <DescriptionsItem label="编码">{{ detailRow?.code }}</DescriptionsItem>
        <DescriptionsItem :label="$t('requirement.project.status')">{{ labelOf(PROJECT_STATUS, detailRow?.status) }}</DescriptionsItem>
        <DescriptionsItem :label="$t('requirement.project.products')">
          <Space wrap>
            <Tag v-for="name in detailRow?.productNames" :key="name">{{ name }}</Tag>
          </Space>
        </DescriptionsItem>
        <DescriptionsItem :label="$t('requirement.project.dates')">{{ detailRow?.startDate ?? '-' }} ~ {{ detailRow?.endDate ?? '-' }}</DescriptionsItem>
        <DescriptionsItem :label="$t('requirement.project.description')">{{ detailRow?.description ?? '-' }}</DescriptionsItem>
      </Descriptions>
    </Drawer>
  </Page>
</template>
