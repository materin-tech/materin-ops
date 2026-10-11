<script lang="ts" setup>
import type { Product, ProductModuleNode, Story, Task } from '#/api/requirement';

import { onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useAccess } from '@vben/access';

import {
  Button,
  Descriptions,
  DescriptionsItem,
  Drawer,
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

import {
  changeStoryStatus,
  createStory,
  deleteStory,
  pageStories,
  productModuleTree,
  productOptions,
  tasksByStory,
  updateStory,
} from '#/api/requirement';

import { PRIORITY, STORY_STATUS, TASK_STATUS, TASK_TYPE, labelOf, useUserOptions } from '../shared';

const { hasAccessByCodes } = useAccess();

const loading = ref(false);
const list = ref<Story[]>([]);
const total = ref(0);
const query = reactive({
  keyword: '',
  page: 1,
  pageSize: 20,
  priority: undefined as undefined | number,
  productId: undefined as undefined | number,
  status: undefined as undefined | number,
});
const products = ref<Product[]>([]);
const { loadUsers, userOptions } = useUserOptions();

async function load() {
  loading.value = true;
  try {
    const res = await pageStories({
      keyword: query.keyword || undefined,
      page: query.page,
      pageSize: query.pageSize,
      priority: query.priority,
      productId: query.productId,
      status: query.status,
    });
    list.value = res.list;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
}

function reset() {
  Object.assign(query, {
    keyword: '',
    page: 1,
    priority: undefined,
    productId: undefined,
    status: undefined,
  });
  load();
}

// ---------- 新增 / 编辑 ----------
const editOpen = ref(false);
const editing = ref<null | Story>(null);
const form = reactive({
  description: '',
  moduleId: undefined as undefined | number,
  ownerId: undefined as undefined | number,
  priority: 2,
  productId: undefined as undefined | number,
  title: '',
});
const moduleTree = ref<ProductModuleNode[]>([]);
const moduleOptions = ref<Array<{ depth: number; id: number; name: string }>>([]);

async function onProductChanged(productId?: number) {
  moduleTree.value = [];
  moduleOptions.value = [];
  form.moduleId = undefined;
  if (productId) {
    const tree = await productModuleTree(productId);
    moduleTree.value = tree;
    moduleOptions.value = flatten(tree);
  }
}

function flatten(
  nodes: ProductModuleNode[],
  depth = 0,
  acc: Array<{ depth: number; id: number; name: string }> = [],
) {
  for (const n of nodes) {
    acc.push({ depth, id: n.id, name: `${'　'.repeat(depth)}${n.name}` });
    if (n.children?.length) flatten(n.children, depth + 1, acc);
  }
  return acc;
}

async function openCreate() {
  editing.value = null;
  Object.assign(form, {
    description: '',
    moduleId: undefined,
    ownerId: undefined,
    priority: 2,
    productId: undefined,
    title: '',
  });
  await loadUsers();
  editOpen.value = true;
}

async function openEdit(row: Story) {
  editing.value = row;
  await loadUsers();
  Object.assign(form, {
    description: row.description ?? '',
    moduleId: row.moduleId ?? undefined,
    ownerId: row.ownerId ?? undefined,
    priority: row.priority,
    productId: row.productId,
    title: row.title,
  });
  await onProductChanged(row.productId);
  editOpen.value = true;
}

async function submit() {
  if (!form.productId || !form.title.trim()) {
    message.warning($t('requirement.story.requiredMsg'));
    return;
  }
  if (editing.value) {
    await updateStory(editing.value.id, {
      description: form.description,
      moduleId: form.moduleId,
      ownerId: form.ownerId,
      priority: form.priority,
      title: form.title,
    });
    message.success($t('requirement.story.updated'));
  } else {
    await createStory({ ...form });
    message.success($t('requirement.story.created'));
  }
  editOpen.value = false;
  await load();
}

async function remove(row: Story) {
  await deleteStory(row.id);
  message.success($t('requirement.story.deleted'));
  await load();
}

// ---------- 详情 ----------
const detailOpen = ref(false);
const detailRow = ref<null | Story>(null);
const detailTasks = ref<Task[]>([]);

async function openDetail(row: Story) {
  detailRow.value = row;
  detailTasks.value = await tasksByStory(row.id);
  detailOpen.value = true;
}

// 状态流转：草稿→评审中→已确认→已关闭（评审中可打回草稿）
const NEXT_ACTION: Record<number, Array<{ label: string; status: number }>> = {
  0: [{ label: $t('requirement.story.aSubmit'), status: 1 }],
  1: [
    { label: $t('requirement.story.aPass'), status: 2 },
    { label: $t('requirement.story.aReject'), status: 0 },
  ],
  2: [{ label: $t('requirement.story.aClose'), status: 3 }],
};

async function doChangeStatus(row: Story, status: number) {
  await changeStoryStatus(row.id, status);
  message.success($t('requirement.story.statusChanged'));
  await load();
}

const columns = [
  { title: 'ID', dataIndex: 'id', width: 70 },
  { title: $t('requirement.story.title'), dataIndex: 'title' },
  { title: $t('requirement.story.product'), dataIndex: 'productName', width: 140 },
  { title: $t('requirement.story.module'), dataIndex: 'moduleTitle', width: 120 },
  { title: $t('requirement.story.status'), dataIndex: 'status', width: 100 },
  { title: $t('requirement.story.priority'), dataIndex: 'priority', width: 90 },
  { title: $t('requirement.story.owner'), dataIndex: 'ownerName', width: 100 },
  { title: $t('requirement.story.action'), width: 300, key: 'action' },
];

const pagination = reactive({
  current: 1,
  onChange: (p: number, ps: number) => {
    query.page = p;
    query.pageSize = ps;
    load();
  },
  pageSize: 20,
  showSizeChanger: true,
  total: 0,
});

onMounted(async () => {
  products.value = await productOptions();
  await load();
});
</script>

<template>
  <Page :title="$t('requirement.story.menu')" :description="$t('requirement.story.desc')">
    <div class="mb-4 flex flex-wrap items-center gap-2">
      <Select
        v-model:value="query.productId"
        :placeholder="$t('requirement.story.productPh')"
        style="width: 160px"
        allow-clear
        :options="products.map((p) => ({ label: p.name, value: p.id }))"
        @change="load"
      />
      <Select
        v-model:value="query.status"
        :placeholder="$t('requirement.story.statusPh')"
        style="width: 120px"
        allow-clear
        :options="STORY_STATUS.map((s) => ({ label: s.label, value: s.value }))"
        @change="load"
      />
      <Select
        v-model:value="query.priority"
        :placeholder="$t('requirement.story.priorityPh')"
        style="width: 110px"
        allow-clear
        :options="PRIORITY.map((s) => ({ label: s.label, value: s.value }))"
        @change="load"
      />
      <Input
        v-model:value="query.keyword"
        :placeholder="$t('requirement.story.keywordPh')"
        style="width: 180px"
        allow-clear
        @pressEnter="load"
      />
      <Button type="primary" @click="load">{{ $t('requirement.story.query') }}</Button>
      <Button @click="reset">{{ $t('requirement.story.reset') }}</Button>
      <div class="flex-1"></div>
      <Button
        v-if="hasAccessByCodes(['requirement:story:add'])"
        type="primary"
        @click="openCreate"
      >{{ $t('requirement.story.add') }}</Button>
    </div>

    <Table
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="{ ...pagination, total }"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'status'">
          <Tag
            :color="['default', 'orange', 'green', 'default'][record.status]"
          >
            {{ labelOf(STORY_STATUS, record.status) }}
          </Tag>
        </template>
        <template v-else-if="column.dataIndex === 'priority'">
          <Tag :color="['red', 'orange', 'blue', 'default'][record.priority]">
            {{ labelOf(PRIORITY, record.priority) }}
          </Tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <Space wrap>
            <Button size="small" @click="openDetail(record as Story)">{{ $t('requirement.story.detail') }}</Button>
            <template
              v-if="hasAccessByCodes(['requirement:story:status'])"
            >
              <Button
                v-for="action in NEXT_ACTION[record.status] ?? []"
                :key="action.status"
                size="small"
                @click="doChangeStatus(record as Story, action.status)"
              >
                {{ action.label }}
              </Button>
            </template>
            <Button
              v-if="hasAccessByCodes(['requirement:story:edit'])"
              size="small"
              @click="openEdit(record as Story)"
            >{{ $t('requirement.story.edit') }}
            </Button>
            <Popconfirm
              v-if="hasAccessByCodes(['requirement:story:delete'])"
              title="确认删除该需求？"
              @confirm="remove(record as Story)"
            >
              <Button danger size="small">{{ $t('requirement.story.delete') }}</Button>
            </Popconfirm>
          </Space>
        </template>
      </template>
    </Table>

    <!-- 新增/编辑需求 -->
    <Modal v-model:open="editOpen" :title="editing ? $t('requirement.story.edit') : $t('requirement.story.add')" width="640">
      <Form layout="vertical">
        <FormItem :label="$t('requirement.story.product')">
          <Select
            v-model:value="form.productId"
            placeholder="选择产品"
            :disabled="!!editing"
            :options="products.map((p) => ({ label: p.name, value: p.id }))"
            @change="(v: any) => onProductChanged(v)"
          />
        </FormItem>
        <FormItem :label="$t('requirement.story.module')">
          <Select
            v-model:value="form.moduleId"
            :placeholder="$t('requirement.story.moduleOptional')"
            allow-clear
            :options="moduleOptions.map((m) => ({ label: m.name, value: m.id }))"
          />
        </FormItem>
        <FormItem :label="$t('requirement.story.title')">
          <Input v-model:value="form.title" :placeholder="$t('requirement.story.titlePh')" />
        </FormItem>
        <FormItem :label="$t('requirement.story.description')">
          <RichEditor v-model:value="form.description" :height="240" />
        </FormItem>
        <FormItem :label="$t('requirement.story.priority')">
          <Select
            v-model:value="form.priority"
            :options="PRIORITY.map((s) => ({ label: s.label, value: s.value }))"
          />
        </FormItem>
        <FormItem :label="$t('requirement.story.owner')">
          <Select
            v-model:value="form.ownerId"
            :placeholder="$t('requirement.story.ownerOptional')"
            allow-clear
            :options="userOptions.map((u) => ({ label: u.nickname || u.username, value: u.id }))"
          />
        </FormItem>
      </Form>
      <template #footer>
        <Button @click="editOpen = false">{{ $t('requirement.story.cancel') }}</Button>
        <Button type="primary" @click="submit">{{ $t('requirement.story.save') }}</Button>
      </template>
    </Modal>

    <!-- 需求详情 -->
    <Drawer v-model:open="detailOpen" :title="detailRow?.title ?? $t('requirement.story.detailTitle')" width="560">
      <Descriptions :column="1" bordered size="small">
        <DescriptionsItem label="ID">{{ detailRow?.id }}</DescriptionsItem>
        <DescriptionsItem :label="$t('requirement.story.product')">{{ detailRow?.productName ?? '-' }}</DescriptionsItem>
        <DescriptionsItem :label="$t('requirement.story.module')">{{ detailRow?.moduleTitle ?? '-' }}</DescriptionsItem>
        <DescriptionsItem :label="$t('requirement.story.status')">
          {{ labelOf(STORY_STATUS, detailRow?.status) }}
        </DescriptionsItem>
        <DescriptionsItem :label="$t('requirement.story.priority')">{{ labelOf(PRIORITY, detailRow?.priority) }}</DescriptionsItem>
        <DescriptionsItem :label="$t('requirement.story.owner')">{{ detailRow?.ownerName ?? '-' }}</DescriptionsItem>
        <DescriptionsItem :label="$t('requirement.story.description')">{{ detailRow?.description ?? '-' }}</DescriptionsItem>
      </Descriptions>
      <div class="mt-4 mb-2 font-medium">{{ $t('requirement.story.relatedTasks') }}（{{ detailTasks.length }}）</div>
      <Table
        :columns="[
          { title: $t('requirement.story.title'), dataIndex: 'title' },
          { title: $t('requirement.story.type'), dataIndex: 'type', width: 80 },
          { title: $t('requirement.story.status'), dataIndex: 'status', width: 90 },
          { title: $t('requirement.story.assignee'), dataIndex: 'assigneeName', width: 100 },
        ]"
        :data-source="detailTasks"
        :pagination="false"
        row-key="id"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'type'">
            {{ labelOf(TASK_TYPE, record.type) }}
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            {{ labelOf(TASK_STATUS, record.status) }}
          </template>
        </template>
      </Table>
    </Drawer>
  </Page>
</template>
