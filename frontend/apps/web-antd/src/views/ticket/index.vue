<script lang="ts" setup>
import type { Ticket, TicketAction } from '#/api/ticket';

import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';
import { useAccess } from '@vben/access';

import {
  Button,
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
  Timeline,
  TimelineItem,
} from 'ant-design-vue';

import {
  acceptTicket,
  changeTicketStatus,
  closeTicket,
  commentTicket,
  createTicket,
  deleteTicket,
  delegateTicket,
  pageTickets,
  updateTicket,
  ticketActions,
} from '#/api/ticket';
import { $t } from '#/locales';
import { PRIORITY, labelOf, useUserOptions } from '#/views/requirement/shared';

const router = useRouter();
const { hasAccessByCodes } = useAccess();
const { loadUsers, userOptions } = useUserOptions();

const loading = ref(false);
const list = ref<Ticket[]>([]);
const total = ref(0);
const query = reactive({
  keyword: '',
  page: 1,
  pageSize: 20,
  priority: undefined as undefined | number,
  status: undefined as undefined | number,
  type: undefined as undefined | number,
});

const TICKET_STATUS = [
  { label: $t('ticket.st.pending'), value: 0 },
  { label: $t('ticket.st.processing'), value: 1 },
  { label: $t('ticket.st.confirming'), value: 2 },
  { label: $t('ticket.st.resolved'), value: 3 },
  { label: $t('ticket.st.closed'), value: 4 },
];
const TICKET_TYPE = [
  { label: $t('ticket.tp.bug'), value: 0 },
  { label: $t('ticket.tp.consult'), value: 1 },
  { label: $t('ticket.tp.change'), value: 2 },
  { label: $t('ticket.tp.other'), value: 3 },
];

async function load() {
  loading.value = true;
  try {
    const res = await pageTickets({
      keyword: query.keyword || undefined,
      page: query.page,
      pageSize: query.pageSize,
      priority: query.priority,
      status: query.status,
      type: query.type,
    });
    list.value = res.list;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
}

function reset() {
  Object.assign(query, { keyword: '', page: 1, priority: undefined, status: undefined, type: undefined });
  load();
}

// ---------- 详情抽屉 ----------
const detailOpen = ref(false);
const detail = ref<null | Ticket>(null);
const actions = ref<TicketAction[]>([]);

async function openDetail(row: Ticket) {
  const [d, a] = await Promise.all([import('#/api/ticket').then((m) => m.ticketDetail(row.id)), ticketActions(row.id)]);
  detail.value = d;
  actions.value = a;
  detailOpen.value = true;
}

const ACTION_LABEL: Record<number, string> = {
  0: $t('ticket.act.create'),
  1: $t('ticket.act.accept'),
  2: $t('ticket.act.reassign'),
  3: $t('ticket.act.comment'),
  4: $t('ticket.act.status'),
  5: $t('ticket.act.resolve'),
  6: $t('ticket.act.close'),
};

function fmt(v?: null | string) {
  return v ? v.replace('T', ' ').slice(0, 19) : '-';
}

function isBreached(t: Ticket) {
  const now = Date.now();
  if (t.status >= 3) {
    return !!t.resolveDeadline && !!t.resolvedAt && new Date(t.resolvedAt).getTime() > new Date(t.resolveDeadline).getTime();
  }
  if (t.status === 0) {
    return !!t.responseDeadline && now > new Date(t.responseDeadline).getTime();
  }
  return !!t.resolveDeadline && now > new Date(t.resolveDeadline).getTime();
}

// ---------- 创建 / 编辑（CRUD） ----------
const createOpen = ref(false);
const editingTicket = ref<null | Ticket>(null);
const createForm = reactive({
  assigneeId: undefined as undefined | number,
  description: '',
  priority: 2,
  title: '',
  type: 0,
});

function openEdit(row: Ticket) {
  editingTicket.value = row;
  Object.assign(createForm, {
    assigneeId: row.assigneeId ?? undefined,
    description: row.description ?? '',
    priority: row.priority,
    title: row.title,
    type: row.type,
  });
  createOpen.value = true;
}

async function submitCreate() {
  if (!createForm.title.trim()) {
    message.warning($t('ticket.titleRequired'));
    return;
  }
  if (editingTicket.value) {
    await updateTicket(editingTicket.value.id, { ...createForm });
    message.success($t('requirement.story.updated'));
  } else {
    await createTicket({ ...createForm });
    message.success($t('ticket.created'));
  }
  createOpen.value = false;
  await load();
}

async function doDelete(row: Ticket) {
  await deleteTicket(row.id);
  message.success($t('requirement.story.deleted'));
  await load();
}

// ---------- 委派 ----------
const delegateOpen = ref(false);
const delegateRow = ref<null | Ticket>(null);
const delegateUserId = ref<number>();

function openDelegate(row: Ticket) {
  delegateRow.value = row;
  delegateUserId.value = row.assigneeId ?? undefined;
  delegateOpen.value = true;
}

async function submitDelegate() {
  if (!delegateUserId.value || !delegateRow.value) {
    message.warning($t('requirement.task.chooseAssigneeMsg'));
    return;
  }
  await delegateTicket(delegateRow.value.id, delegateUserId.value);
  message.success($t('requirement.task.assigned'));
  delegateOpen.value = false;
  await load();
}

// ---------- 操作 ----------
const commentOpen = ref(false);
const commentRow = ref<null | Ticket>(null);
const commentContent = ref('');

async function doAccept(row: Ticket) {
  await acceptTicket(row.id);
  message.success($t('ticket.accepted'));
  await load();
  if (detail.value?.id === row.id) await openDetail(row);
}

async function doStatus(row: Ticket, status: number) {
  await changeTicketStatus(row.id, status);
  message.success($t('requirement.task.statusChanged'));
  await load();
  if (detail.value?.id === row.id) await openDetail(row);
}

async function doClose(row: Ticket) {
  await closeTicket(row.id);
  message.success($t('ticket.closed'));
  await load();
}

function openComment(row: Ticket) {
  commentRow.value = row;
  commentContent.value = '';
  commentOpen.value = true;
}

async function submitComment() {
  if (!commentContent.value.trim() || !commentRow.value) {
    message.warning($t('ticket.contentRequired'));
    return;
  }
  await commentTicket(commentRow.value.id, commentContent.value);
  message.success($t('ticket.commented'));
  commentOpen.value = false;
  await load();
}

const STATUS_COLOR = ['default', 'blue', 'orange', 'green', 'default'];

const columns = [
  { title: $t('ticket.no'), dataIndex: 'ticketNo', width: 170 },
  { title: $t('ticket.titleCol'), dataIndex: 'title' },
  { title: $t('requirement.task.type'), dataIndex: 'type', width: 90 },
  { title: $t('requirement.task.status'), dataIndex: 'status', width: 100 },
  { title: $t('requirement.task.priority'), dataIndex: 'priority', width: 90 },
  { title: 'SLA', width: 110, key: 'sla' },
  { title: $t('ticket.createdAt'), dataIndex: 'createdAt', width: 170 },
  { title: $t('requirement.task.action'), width: 330, key: 'action' },
];

onMounted(async () => {
  await loadUsers();
  await load();
});
</script>

<template>
  <Page :title="$t('ticket.menu')" :description="$t('ticket.desc')">
    <div class="mb-4 flex flex-wrap items-center gap-2">
      <Select
        v-model:value="query.status"
        :placeholder="$t('requirement.task.status')"
        style="width: 120px"
        allow-clear
        :options="TICKET_STATUS.map((s) => ({ label: s.label, value: s.value }))"
        @change="load"
      />
      <Select
        v-model:value="query.priority"
        :placeholder="$t('requirement.task.priority')"
        style="width: 110px"
        allow-clear
        :options="PRIORITY.map((s) => ({ label: s.label, value: s.value }))"
        @change="load"
      />
      <Select
        v-model:value="query.type"
        :placeholder="$t('requirement.task.type')"
        style="width: 110px"
        allow-clear
        :options="TICKET_TYPE.map((s) => ({ label: s.label, value: s.value }))"
        @change="load"
      />
      <Input
        v-model:value="query.keyword"
        :placeholder="$t('ticket.keywordPh')"
        style="width: 200px"
        allow-clear
        @press-enter="load"
      />
      <Button type="primary" @click="load">{{ $t('requirement.task.query') }}</Button>
      <Button @click="reset">{{ $t('requirement.task.reset') }}</Button>
      <div class="flex-1"></div>
      <Button type="primary" @click="router.push({ name: 'TicketCreate' })">
        {{ $t('ticket.create') }}
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
        <template v-if="column.dataIndex === 'status'">
          <Tag :color="STATUS_COLOR[record.status]">
            {{ labelOf(TICKET_STATUS, record.status) }}
          </Tag>
        </template>
        <template v-else-if="column.dataIndex === 'priority'">
          <Tag :color="['red', 'orange', 'blue', 'default'][record.priority]">
            {{ labelOf(PRIORITY, record.priority) }}
          </Tag>
        </template>
        <template v-else-if="column.dataIndex === 'type'">
          {{ labelOf(TICKET_TYPE, record.type) }}
        </template>
        <template v-else-if="column.key === 'sla'">
          <Tag v-if="isBreached(record as Ticket)" color="red">
            {{ $t('ticket.slaBreached') }}
          </Tag>
          <Tag v-else color="green">SLA</Tag>
        </template>
        <template v-else-if="column.dataIndex === 'createdAt'">
          {{ fmt(record.createdAt) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <Space wrap>
            <Button size="small" @click="openDetail(record as Ticket)">
              {{ $t('requirement.story.detail') }}
            </Button>
            <Button
              v-if="(record as Ticket).status !== 4 && hasAccessByCodes(['ticket:edit'])"
              size="small"
              @click="openEdit(record as Ticket)"
            >
              {{ $t('ticket.edit') }}
            </Button>
            <Button
              v-if="(record as Ticket).status === 0 && hasAccessByCodes(['ticket:handle'])"
              size="small"
              type="primary"
              @click="doAccept(record as Ticket)"
            >
              {{ $t('ticket.accept') }}
            </Button>
            <Button
              v-if="
                ((record as Ticket).status === 0 || (record as Ticket).status === 1) &&
                hasAccessByCodes(['ticket:handle'])
              "
              size="small"
              @click="openDelegate(record as Ticket)"
            >
              {{ $t('ticket.delegate') }}
            </Button>
            <template v-if="(record as Ticket).status === 1 && hasAccessByCodes(['ticket:handle'])">
              <Button size="small" @click="doStatus(record as Ticket, 2)">
                {{ $t('ticket.toConfirm') }}
              </Button>
            </template>
            <Button
              v-if="(record as Ticket).status === 2 && hasAccessByCodes(['ticket:handle'])"
              size="small"
              type="primary"
              @click="doStatus(record as Ticket, 3)"
            >
              {{ $t('ticket.confirmResolve') }}
            </Button>
            <Button
              v-if="(record as Ticket).status === 2 && hasAccessByCodes(['ticket:handle'])"
              size="small"
              @click="doStatus(record as Ticket, 1)"
            >
              {{ $t('ticket.backToProcess') }}
            </Button>
            <Button size="small" @click="openComment(record as Ticket)">
              {{ $t('ticket.comment') }}
            </Button>
            <Popconfirm
              v-if="(record as Ticket).status !== 0 && (record as Ticket).status !== 4 && hasAccessByCodes(['ticket:close'])"
              :title="$t('ticket.closeConfirm')"
              @confirm="doClose(record as Ticket)"
            >
              <Button danger size="small">{{ $t('ticket.close') }}</Button>
            </Popconfirm>
            <Popconfirm
              v-if="hasAccessByCodes(['ticket:delete'])"
              :title="$t('ticket.deleteConfirm')"
              @confirm="doDelete(record as Ticket)"
            >
              <Button danger size="small">{{ $t('ticket.delete') }}</Button>
            </Popconfirm>
          </Space>
        </template>
      </template>
    </Table>

    <!-- 创建/编辑工单 -->
    <Modal v-model:open="createOpen" :title="editingTicket ? $t('ticket.edit') : $t('ticket.create')" width="640">
      <Form layout="vertical">
        <FormItem :label="$t('ticket.titleCol')">
          <Input v-model:value="createForm.title" />
        </FormItem>
        <FormItem :label="$t('requirement.task.description')">
          <RichEditor v-model:value="createForm.description" :height="220" />
        </FormItem>
        <FormItem :label="$t('requirement.task.type')">
          <Select
            v-model:value="createForm.type"
            :options="TICKET_TYPE.map((s) => ({ label: s.label, value: s.value }))"
          />
        </FormItem>
        <FormItem :label="$t('requirement.task.priority')">
          <Select
            v-model:value="createForm.priority"
            :options="PRIORITY.map((s) => ({ label: s.label, value: s.value }))"
          />
        </FormItem>
        <FormItem :label="$t('ticket.assignee')">
          <Select
            v-model:value="createForm.assigneeId"
            :placeholder="$t('ticket.assigneePh')"
            allow-clear
            :options="userOptions.map((u) => ({ label: u.nickname || u.username, value: u.id }))"
          />
        </FormItem>
      </Form>
      <template #footer>
        <Button @click="createOpen = false">{{ $t('requirement.story.cancel') }}</Button>
        <Button type="primary" @click="submitCreate">{{ $t('requirement.story.save') }}</Button>
      </template>
    </Modal>

    <!-- 详情抽屉 -->
    <Drawer v-model:open="detailOpen" :title="detail?.ticketNo" width="620">
      <template v-if="detail">
        <div class="mb-3 text-lg font-medium">{{ detail.title }}</div>
        <div class="mb-4 flex flex-wrap gap-2">
          <Tag :color="STATUS_COLOR[detail.status]">{{ labelOf(TICKET_STATUS, detail.status) }}</Tag>
          <Tag :color="['red', 'orange', 'blue', 'default'][detail.priority]">
            {{ labelOf(PRIORITY, detail.priority) }}
          </Tag>
          <Tag>{{ labelOf(TICKET_TYPE, detail.type) }}</Tag>
          <Tag v-if="isBreached(detail)" color="red">{{ $t('ticket.slaBreached') }}</Tag>
        </div>
        <div class="mb-3 text-xs text-gray-400">
          {{ $t('ticket.responseDeadline') }}: {{ fmt(detail.responseDeadline) }} ·
          {{ $t('ticket.resolveDeadline') }}: {{ fmt(detail.resolveDeadline) }} ·
          {{ $t('ticket.firstResponse') }}: {{ fmt(detail.firstResponseAt) }} ·
          {{ $t('ticket.resolvedAt') }}: {{ fmt(detail.resolvedAt) }}
        </div>
        <div class="rich-view" v-html="detail.description"></div>
        <div class="mt-6 mb-2 font-medium">{{ $t('ticket.timeline') }}</div>
        <Timeline>
          <TimelineItem v-for="a in actions" :key="a.id">
            <span class="font-medium">{{ ACTION_LABEL[a.action] ?? a.action }}</span>
            <span v-if="a.content" class="ml-2 text-gray-500">{{ a.content }}</span>
            <div class="text-xs text-gray-400">{{ fmt(a.createdAt) }} · #{{ a.userId }}</div>
          </TimelineItem>
        </Timeline>
      </template>
    </Drawer>

    <!-- 委派 -->
    <Modal v-model:open="delegateOpen" :title="$t('ticket.delegate')">
      <Select
        v-model:value="delegateUserId"
        :placeholder="$t('ticket.assigneePh')"
        style="width: 100%"
        :options="userOptions.map((u) => ({ label: u.nickname || u.username, value: u.id }))"
      />
      <template #footer>
        <Button @click="delegateOpen = false">{{ $t('requirement.story.cancel') }}</Button>
        <Button type="primary" @click="submitDelegate">
          {{ $t('requirement.story.save') }}
        </Button>
      </template>
    </Modal>

    <!-- 备注 -->
    <Modal v-model:open="commentOpen" :title="$t('ticket.comment')">
      <RichEditor v-model:value="commentContent" :height="180" />
      <template #footer>
        <Button @click="commentOpen = false">{{ $t('requirement.story.cancel') }}</Button>
        <Button type="primary" @click="submitComment">{{ $t('requirement.story.save') }}</Button>
      </template>
    </Modal>
  </Page>
</template>
