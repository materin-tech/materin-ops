<script lang="ts" setup>
import type { Iteration, Project } from '#/api/requirement';

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
  message,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
} from 'ant-design-vue';

import { $t } from '#/locales';
import dayjs from 'dayjs';

import {
  changeIterationStatus,
  createIteration,
  deleteIteration,
  pageIterations,
  projectOptions,
  updateIteration,
} from '#/api/requirement';

import { ITERATION_STATUS, labelOf } from '../shared';

const { hasAccessByCodes } = useAccess();

const loading = ref(false);
const list = ref<Iteration[]>([]);
const total = ref(0);
const query = reactive({
  page: 1,
  pageSize: 20,
  projectId: undefined as undefined | number,
  status: undefined as undefined | number,
});
const projects = ref<Project[]>([]);

async function load() {
  loading.value = true;
  try {
    const res = await pageIterations({
      page: query.page,
      pageSize: query.pageSize,
      projectId: query.projectId,
      status: query.status,
    });
    list.value = res.list;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
}

function reset() {
  Object.assign(query, { page: 1, projectId: undefined, status: undefined });
  load();
}

// ---------- 新增 / 编辑 ----------
const editOpen = ref(false);
const editing = ref<null | Iteration>(null);
const form = reactive({
  name: '',
  planEnd: undefined as undefined | string,
  planStart: undefined as undefined | string,
  projectId: undefined as undefined | number,
});

function toDay(value?: null | string) {
  return value ? dayjs(value) : undefined;
}

async function openCreate() {
  editing.value = null;
  Object.assign(form, { name: '', planEnd: undefined, planStart: undefined, projectId: undefined });
  editOpen.value = true;
}

async function openEdit(row: Iteration) {
  editing.value = row;
  Object.assign(form, {
    name: row.name,
    planEnd: row.planEnd ?? undefined,
    planStart: row.planStart ?? undefined,
    projectId: row.projectId,
  });
  editOpen.value = true;
}

async function submit() {
  if (!form.projectId || !form.name.trim()) {
    message.warning($t('requirement.iteration.requiredMsg'));
    return;
  }
  if (editing.value) {
    await updateIteration(editing.value.id, {
      name: form.name,
      planEnd: form.planEnd,
      planStart: form.planStart,
    });
    message.success($t('requirement.iteration.updated'));
  } else {
    await createIteration({ ...form });
    message.success($t('requirement.iteration.created'));
  }
  editOpen.value = false;
  await load();
}

async function remove(row: Iteration) {
  await deleteIteration(row.id);
  message.success($t('requirement.iteration.deleted'));
  await load();
}

// 状态流转：未开始→进行中→已结束
const NEXT_ACTION: Record<number, Array<{ label: string; status: number }>> = {
  0: [{ label: $t('requirement.iteration.aStart'), status: 1 }],
  1: [{ label: $t('requirement.iteration.aEnd'), status: 2 }],
};

async function doChangeStatus(row: Iteration, status: number) {
  await changeIterationStatus(row.id, status);
  message.success($t('requirement.iteration.statusChanged'));
  await load();
}

const detailOpen = ref(false);
const detailRow = ref<null | Iteration>(null);

function openDetail(row: Iteration) {
  detailRow.value = row;
  detailOpen.value = true;
}

const columns = [
  { title: 'ID', dataIndex: 'id', width: 70 },
  { title: $t('requirement.iteration.name'), dataIndex: 'name' },
  { title: $t('requirement.iteration.project'), dataIndex: 'projectName', width: 180 },
  { title: $t('requirement.iteration.status'), dataIndex: 'status', width: 90 },
  { title: $t('requirement.iteration.planDates'), width: 200, key: 'plan' },
  { title: $t('requirement.iteration.actualDates'), width: 200, key: 'actual' },
  { title: $t('requirement.iteration.action'), width: 260, key: 'action' },
];

onMounted(async () => {
  projects.value = await projectOptions();
  await load();
});
</script>

<template>
  <Page :title="$t('requirement.iteration.menu')" :description="$t('requirement.iteration.desc')">
    <div class="mb-4 flex flex-wrap items-center gap-2">
      <Select
        v-model:value="query.projectId"
        placeholder="项目"
        style="width: 180px"
        allow-clear
        :options="projects.map((p) => ({ label: p.name, value: p.id }))"
        @change="load"
      />
      <Select
        v-model:value="query.status"
        placeholder="状态"
        style="width: 120px"
        allow-clear
        :options="ITERATION_STATUS.map((s) => ({ label: s.label, value: s.value }))"
        @change="load"
      />
      <Button type="primary" @click="load">{{ $t('requirement.iteration.query') }}</Button>
      <Button @click="reset">{{ $t('requirement.iteration.reset') }}</Button>
      <div class="flex-1"></div>
      <Button
        v-if="hasAccessByCodes(['requirement:iteration:add'])"
        type="primary"
        @click="openCreate"
      >{{ $t('requirement.iteration.add') }}</Button>
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
          <Tag :color="['default', 'green', 'blue'][record.status]">
            {{ labelOf(ITERATION_STATUS, record.status) }}
          </Tag>
        </template>
        <template v-else-if="column.key === 'plan'">
          {{ record.planStart ?? '-' }} ~ {{ record.planEnd ?? '-' }}
        </template>
        <template v-else-if="column.key === 'actual'">
          {{ record.actualStart ?? '-' }} ~ {{ record.actualEnd ?? '-' }}
        </template>
        <template v-else-if="column.key === 'action'">
          <Space>
            <Button size="small" @click="openDetail(record as Iteration)">{{ $t('requirement.iteration.detail') }}</Button>            <template v-if="hasAccessByCodes(['requirement:iteration:status'])">
              <Button
                v-for="action in NEXT_ACTION[record.status] ?? []"
                :key="action.status"
                size="small"
                @click="doChangeStatus(record as Iteration, action.status)"
              >
                {{ action.label }}
              </Button>
            </template>
            <Button
              v-if="hasAccessByCodes(['requirement:iteration:edit'])"
              size="small"
              @click="openEdit(record as Iteration)"
            >{{ $t('requirement.iteration.edit') }}
            </Button>
            <Popconfirm
              v-if="hasAccessByCodes(['requirement:iteration:delete'])"
              :title="$t('requirement.iteration.delConfirm')"
              @confirm="remove(record as Iteration)"
            >
              <Button danger size="small">{{ $t('requirement.iteration.delete') }}</Button>
            </Popconfirm>
          </Space>
        </template>
      </template>
    </Table>

    <!-- 新增/编辑迭代 -->
    <Modal v-model:open="editOpen" :title="editing ? $t('requirement.iteration.edit') : $t('requirement.iteration.add')">
      <Form layout="vertical">
        <FormItem :label="$t('requirement.iteration.project')">
          <Select
            v-model:value="form.projectId"
            :placeholder="$t('requirement.iteration.chooseProject')"
            :disabled="!!editing"
            :options="projects.map((p) => ({ label: p.name, value: p.id }))"
          />
        </FormItem>
        <FormItem :label="$t('requirement.iteration.name')">
          <Input v-model:value="form.name" :placeholder="$t('requirement.iteration.namePh')" />
        </FormItem>
        <FormItem :label="$t('requirement.iteration.planDates')">
          <Space>
            <DatePicker
              :value="toDay(form.planStart)"
              @change="(_, ds) => (form.planStart = ds || undefined)"
            />
            <DatePicker
              :value="toDay(form.planEnd)"
              @change="(_, ds) => (form.planEnd = ds || undefined)"
            />
          </Space>
        </FormItem>
      </Form>
      <template #footer>
        <Button @click="editOpen = false">{{ $t('requirement.iteration.cancel') }}</Button>
        <Button type="primary" @click="submit">{{ $t('requirement.iteration.save') }}</Button>
      </template>
    </Modal>
    <!-- 迭代详情 -->
    <Drawer v-model:open="detailOpen" :title="detailRow?.name ?? $t('requirement.iteration.detailTitle')" width="480">
      <Descriptions :column="1" bordered size="small">
        <DescriptionsItem label="ID">{{ detailRow?.id }}</DescriptionsItem>
        <DescriptionsItem :label="$t('requirement.iteration.project')">{{ detailRow?.projectName ?? '-' }}</DescriptionsItem>
        <DescriptionsItem :label="$t('requirement.iteration.status')">{{ labelOf(ITERATION_STATUS, detailRow?.status) }}</DescriptionsItem>
        <DescriptionsItem :label="$t('requirement.iteration.planDates')">{{ detailRow?.planStart ?? '-' }} ~ {{ detailRow?.planEnd ?? '-' }}</DescriptionsItem>
        <DescriptionsItem :label="$t('requirement.iteration.actualDates')">{{ detailRow?.actualStart ?? '-' }} ~ {{ detailRow?.actualEnd ?? '-' }}</DescriptionsItem>
      </Descriptions>
    </Drawer>
  </Page>
</template>
