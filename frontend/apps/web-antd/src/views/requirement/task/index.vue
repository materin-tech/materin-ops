<script lang="ts" setup>
import type { Iteration, Project, Story, Task } from '#/api/requirement';

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
  InputNumber,
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
  assignTask,
  changeTaskStatus,
  createTask,
  deleteTask,
  pageIterations,
  pageStories,
  pageTasks,
  projectOptions,
  updateTask,
} from '#/api/requirement';

import { PRIORITY, TASK_STATUS, TASK_TYPE, labelOf, useUserOptions } from '../shared';

const { hasAccessByCodes } = useAccess();

const loading = ref(false);
const list = ref<Task[]>([]);
const total = ref(0);
const query = reactive({
  assigneeId: undefined as undefined | number,
  iterationId: undefined as undefined | number,
  keyword: '',
  page: 1,
  pageSize: 20,
  projectId: undefined as undefined | number,
  status: undefined as undefined | number,
  type: undefined as undefined | number,
});
const projects = ref<Project[]>([]);
const iterations = ref<Iteration[]>([]);
const { loadUsers, userOptions } = useUserOptions();

async function load() {
  loading.value = true;
  try {
    const res = await pageTasks({
      assigneeId: query.assigneeId,
      iterationId: query.iterationId,
      keyword: query.keyword || undefined,
      page: query.page,
      pageSize: query.pageSize,
      projectId: query.projectId,
      status: query.status,
      type: query.type,
    });
    list.value = res.list;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
}

async function onProjectChanged(projectId?: number) {
  query.iterationId = undefined;
  iterations.value = [];
  if (projectId) {
    const res = await pageIterations({ page: 1, pageSize: 100, projectId });
    iterations.value = res.list;
  }
  load();
}

function reset() {
  Object.assign(query, {
    assigneeId: undefined,
    iterationId: undefined,
    keyword: '',
    page: 1,
    projectId: undefined,
    status: undefined,
    type: undefined,
  });
  iterations.value = [];
  load();
}

// ---------- 新增 / 编辑 ----------
const editOpen = ref(false);
const editing = ref<null | Task>(null);
const form = reactive({
  actualHours: undefined as undefined | number,
  assigneeId: undefined as undefined | number,
  description: '',
  iterationId: undefined as undefined | number,
  planEnd: undefined as undefined | string,
  planHours: undefined as undefined | number,
  planStart: undefined as undefined | string,
  priority: 2,
  projectId: undefined as undefined | number,
  storyId: undefined as undefined | number,
  title: '',
  type: 0,
});
const formIterations = ref<Iteration[]>([]);
const formStories = ref<Story[]>([]);

async function loadFormIterations(projectId?: number) {
  formIterations.value = [];
  formStories.value = [];
  form.iterationId = undefined;
  form.storyId = undefined;
  if (projectId) {
    const [iterRes, storyRes] = await Promise.all([
      pageIterations({ page: 1, pageSize: 100, projectId }),
      pageStories({ page: 1, pageSize: 100, productId: undefined, projectId: undefined }),
    ]);
    formIterations.value = iterRes.list;
    formStories.value = storyRes.list;
  }
}

async function openCreate() {
  editing.value = null;
  Object.assign(form, {
    actualHours: undefined,
    assigneeId: undefined,
    description: '',
    iterationId: undefined,
    planEnd: undefined,
    planHours: undefined,
    planStart: undefined,
    priority: 2,
    projectId: undefined,
    storyId: undefined,
    title: '',
    type: 0,
  });
  formIterations.value = [];
  formStories.value = [];
  await loadUsers();
  editOpen.value = true;
}

async function openEdit(row: Task) {
  editing.value = row;
  await loadUsers();
  Object.assign(form, {
    actualHours: row.actualHours ? Number(row.actualHours) : undefined,
    assigneeId: row.assigneeId ?? undefined,
    description: row.description ?? '',
    iterationId: row.iterationId ?? undefined,
    planEnd: row.planEnd ?? undefined,
    planHours: row.planHours ? Number(row.planHours) : undefined,
    planStart: row.planStart ?? undefined,
    priority: row.priority,
    projectId: row.projectId,
    storyId: row.storyId ?? undefined,
    title: row.title,
    type: row.type,
  });
  const [iterRes, storyRes] = await Promise.all([
    pageIterations({ page: 1, pageSize: 100, projectId: row.projectId }),
    pageStories({ page: 1, pageSize: 100 }),
  ]);
  formIterations.value = iterRes.list;
  formStories.value = storyRes.list;
  editOpen.value = true;
}

async function submit() {
  if (!form.projectId || !form.title.trim()) {
    message.warning($t('requirement.task.requiredMsg'));
    return;
  }
  const payload = {
    actualHours: form.actualHours,
    assigneeId: form.assigneeId,
    description: form.description,
    iterationId: form.iterationId,
    planEnd: form.planEnd,
    planHours: form.planHours,
    planStart: form.planStart,
    priority: form.priority,
    storyId: form.storyId,
    title: form.title,
    type: form.type,
  };
  if (editing.value) {
    await updateTask(editing.value.id, payload);
    message.success($t('requirement.task.updated'));
  } else {
    await createTask({ ...payload, projectId: form.projectId });
    message.success($t('requirement.task.created'));
  }
  editOpen.value = false;
  await load();
}

async function remove(row: Task) {
  await deleteTask(row.id);
  message.success($t('requirement.task.deleted'));
  await load();
}

async function doAssign(row: Task, userId: number) {
  if (!userId) {
    message.warning($t('requirement.task.chooseAssigneeMsg'));
    return;
  }
  await assignTask(row.id, userId);
  message.success($t('requirement.task.assigned'));
  await load();
}

// 状态流转
const NEXT_ACTION: Record<number, Array<{ label: string; status: number }>> = {
  0: [
    { label: $t('requirement.task.aStart'), status: 1 },
    { label: $t('requirement.task.aClose'), status: 3 },
  ],
  1: [
    { label: $t('requirement.task.aDone'), status: 2 },
    { label: $t('requirement.task.aBack'), status: 0 },
    { label: $t('requirement.task.aClose'), status: 3 },
  ],
  2: [{ label: $t('requirement.task.aClose'), status: 3 }],
};

async function doChangeStatus(row: Task, status: number) {
  await changeTaskStatus(row.id, status);
  message.success($t('requirement.task.statusChanged'));
  await load();
}

function toDay(value?: null | string) {
  return value ? dayjs(value) : undefined;
}

const detailOpen = ref(false);
const detailRow = ref<null | Task>(null);

function openDetail(row: Task) {
  detailRow.value = row;
  detailOpen.value = true;
}

const columns = [
  { title: 'ID', dataIndex: 'id', width: 70 },
  { title: $t('requirement.task.title'), dataIndex: 'title' },
  { title: $t('requirement.task.project'), dataIndex: 'projectName', width: 130 },
  { title: $t('requirement.task.iteration'), dataIndex: 'iterationName', width: 110 },
  { title: $t('requirement.task.story'), dataIndex: 'storyTitle', width: 160 },
  { title: $t('requirement.task.type'), dataIndex: 'type', width: 80 },
  { title: $t('requirement.task.status'), dataIndex: 'status', width: 90 },
  { title: $t('requirement.task.priority'), dataIndex: 'priority', width: 80 },
  { title: $t('requirement.task.assignee'), dataIndex: 'assigneeName', width: 90 },
  { title: $t('requirement.task.action'), width: 320, key: 'action' },
];

onMounted(async () => {
  projects.value = await projectOptions();
  await loadUsers();
  await load();
});
</script>

<template>
  <Page :title="$t('requirement.task.menu')" :description="$t('requirement.task.desc')">
    <div class="mb-4 flex flex-wrap items-center gap-2">
      <Select
        v-model:value="query.projectId"
        placeholder="项目"
        style="width: 160px"
        allow-clear
        :options="projects.map((p) => ({ label: p.name, value: p.id }))"
        @change="(v: any) => onProjectChanged(v)"
      />
      <Select
        v-model:value="query.iterationId"
        placeholder="迭代"
        style="width: 140px"
        allow-clear
        :options="iterations.map((i) => ({ label: i.name, value: i.id }))"
        @change="load"
      />
      <Select
        v-model:value="query.type"
        placeholder="类型"
        style="width: 100px"
        allow-clear
        :options="TASK_TYPE.map((s) => ({ label: s.label, value: s.value }))"
        @change="load"
      />
      <Select
        v-model:value="query.status"
        placeholder="状态"
        style="width: 110px"
        allow-clear
        :options="TASK_STATUS.map((s) => ({ label: s.label, value: s.value }))"
        @change="load"
      />
      <Input
        v-model:value="query.keyword"
        placeholder="标题关键字"
        style="width: 160px"
        allow-clear
        @pressEnter="load"
      />
      <Button type="primary" @click="load">{{ $t('requirement.task.query') }}</Button>
      <Button @click="reset">{{ $t('requirement.task.reset') }}</Button>
      <div class="flex-1"></div>
      <Button
        v-if="hasAccessByCodes(['requirement:task:add'])"
        type="primary"
        @click="openCreate"
      >{{ $t('requirement.task.add') }}</Button>
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
      :scroll="{ x: 1200 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'type'">
          {{ labelOf(TASK_TYPE, record.type) }}
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <Tag :color="['default', 'blue', 'green', 'default'][record.status]">
            {{ labelOf(TASK_STATUS, record.status) }}
          </Tag>
        </template>
        <template v-else-if="column.dataIndex === 'priority'">
          <Tag :color="['red', 'orange', 'blue', 'default'][record.priority]">
            {{ labelOf(PRIORITY, record.priority) }}
          </Tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <Space wrap>
            <Button size="small" @click="openDetail(record as Task)">{{ $t('requirement.task.detail') }}</Button>
            <template v-if="hasAccessByCodes(['requirement:task:status'])">
              <Button
                v-for="action in NEXT_ACTION[record.status] ?? []"
                :key="action.status"
                size="small"
                @click="doChangeStatus(record as Task, action.status)"
              >
                {{ action.label }}
              </Button>
            </template>
            <template v-if="hasAccessByCodes(['requirement:task:assign'])">
              <Select
                :value="record.assigneeId ?? undefined"
                size="small"
                style="width: 110px"
                :placeholder="$t('requirement.task.assignee')"
                :options="userOptions.map((u) => ({ label: u.nickname || u.username, value: u.id }))"
                @change="(uid: any) => doAssign(record as Task, uid)"
              />
            </template>
            <Button
              v-if="hasAccessByCodes(['requirement:task:edit'])"
              size="small"
              @click="openEdit(record as Task)"
            >{{ $t('requirement.task.edit') }}
            </Button>
            <Popconfirm
              v-if="hasAccessByCodes(['requirement:task:delete'])"
              :title="$t('requirement.task.delConfirm')"
              @confirm="remove(record as Task)"
            >
              <Button danger size="small">{{ $t('requirement.task.delete') }}</Button>
            </Popconfirm>
          </Space>
        </template>
      </template>
    </Table>

    <!-- 新增/编辑任务 -->
    <Modal v-model:open="editOpen" :title="editing ? $t('requirement.task.edit') : $t('requirement.task.add')" width="640">
      <Form layout="vertical">
        <FormItem :label="$t('requirement.task.project')">
          <Select
            v-model:value="form.projectId"
            :placeholder="$t('requirement.task.chooseProject')"
            :disabled="!!editing"
            :options="projects.map((p) => ({ label: p.name, value: p.id }))"
            @change="(v: any) => loadFormIterations(v)"
          />
        </FormItem>
        <FormItem :label="$t('requirement.task.iteration')">
          <Select
            v-model:value="form.iterationId"
            :placeholder="$t('requirement.task.iterationOptional')"
            allow-clear
            :options="formIterations.map((i) => ({ label: i.name, value: i.id }))"
          />
        </FormItem>
        <FormItem :label="$t('requirement.task.story')">
          <Select
            v-model:value="form.storyId"
            :placeholder="$t('requirement.task.storyOptional')"
            allow-clear
            show-search
            :option-filter-prop="'label'"
            :options="formStories.map((s) => ({ label: s.title, value: s.id }))"
          />
        </FormItem>
        <FormItem :label="$t('requirement.task.title')">
          <Input v-model:value="form.title" />
        </FormItem>
        <FormItem :label="$t('requirement.task.description')">
          <RichEditor v-model:value="form.description" :height="200" />
        </FormItem>
        <FormItem :label="$t('requirement.task.typePriority')">
          <Space>
            <Select
              v-model:value="form.type"
              style="width: 120px"
              :options="TASK_TYPE.map((s) => ({ label: s.label, value: s.value }))"
            />
            <Select
              v-model:value="form.priority"
              style="width: 120px"
              :options="PRIORITY.map((s) => ({ label: s.label, value: s.value }))"
            />
          </Space>
        </FormItem>
        <FormItem :label="$t('requirement.task.assignee')">
          <Select
            v-model:value="form.assigneeId"
            :placeholder="$t('requirement.task.assigneeOptional')"
            allow-clear
            :options="userOptions.map((u) => ({ label: u.nickname || u.username, value: u.id }))"
          />
        </FormItem>
        <FormItem :label="$t('requirement.task.hours')">
          <Space>
            <InputNumber v-model:value="form.planHours" :min="0" :step="0.5" />
            <InputNumber v-model:value="form.actualHours" :min="0" :step="0.5" />
          </Space>
        </FormItem>
        <FormItem :label="$t('requirement.task.planDates')">
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
        <Button @click="editOpen = false">{{ $t('requirement.task.cancel') }}</Button>
        <Button type="primary" @click="submit">{{ $t('requirement.task.save') }}</Button>
      </template>
    </Modal>
    <!-- 任务详情 -->
    <Drawer v-model:open="detailOpen" :title="detailRow?.title ?? $t('requirement.task.detailTitle')" width="520">
      <Descriptions :column="1" bordered size="small">
        <DescriptionsItem label="ID">{{ detailRow?.id }}</DescriptionsItem>
        <DescriptionsItem :label="$t('requirement.task.project')">{{ detailRow?.projectName ?? '-' }}</DescriptionsItem>
        <DescriptionsItem :label="$t('requirement.task.iteration')">{{ detailRow?.iterationName ?? '-' }}</DescriptionsItem>
        <DescriptionsItem :label="$t('requirement.task.story')">{{ detailRow?.storyTitle ?? '-' }}</DescriptionsItem>
        <DescriptionsItem :label="$t('requirement.task.type')">{{ labelOf(TASK_TYPE, detailRow?.type) }}</DescriptionsItem>
        <DescriptionsItem :label="$t('requirement.task.status')">{{ labelOf(TASK_STATUS, detailRow?.status) }}</DescriptionsItem>
        <DescriptionsItem :label="$t('requirement.task.priority')">{{ labelOf(PRIORITY, detailRow?.priority) }}</DescriptionsItem>
        <DescriptionsItem :label="$t('requirement.task.assignee')">{{ detailRow?.assigneeName ?? '-' }}</DescriptionsItem>
        <DescriptionsItem :label="$t('requirement.task.hours')">
          {{ detailRow?.planHours ?? '-' }} / {{ detailRow?.actualHours ?? '-' }} 小时
        </DescriptionsItem>
        <DescriptionsItem :label="$t('requirement.task.planDates')">{{ detailRow?.planStart ?? '-' }} ~ {{ detailRow?.planEnd ?? '-' }}</DescriptionsItem>
        <DescriptionsItem :label="$t('requirement.task.description')">{{ detailRow?.description ?? '-' }}</DescriptionsItem>
      </Descriptions>
    </Drawer>
  </Page>
</template>
