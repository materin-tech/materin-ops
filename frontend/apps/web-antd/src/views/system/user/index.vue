<script lang="ts" setup>
import type { SystemRole, SystemUser } from '#/api/system';

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
  changeUserStatus,
  createUser,
  deleteUser,
  pageUsers,
  resetUserPassword,
  updateUser,
} from '#/api/system';
import { allRoles } from '#/api/system';

const { hasAccessByCodes } = useAccess();

const loading = ref(false);
const list = ref<SystemUser[]>([]);
const total = ref(0);
const query = reactive({
  page: 1,
  pageSize: 20,
  status: undefined as undefined | number,
  username: '',
});
const roles = ref<SystemRole[]>([]);

async function load() {
  loading.value = true;
  try {
    const res = await pageUsers({
      page: query.page,
      pageSize: query.pageSize,
      status: query.status,
      username: query.username || undefined,
    });
    list.value = res.list;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
}

function reset() {
  Object.assign(query, { page: 1, status: undefined, username: '' });
  load();
}

// ---------- 新增 / 编辑 ----------
const editOpen = ref(false);
const editing = ref<null | SystemUser>(null);
const form = reactive({
  email: '',
  nickname: '',
  password: '',
  phone: '',
  roleIds: [] as number[],
  status: 1,
  username: '',
});

function openCreate() {
  editing.value = null;
  Object.assign(form, {
    email: '',
    nickname: '',
    password: '',
    phone: '',
    roleIds: [],
    status: 1,
    username: '',
  });
  editOpen.value = true;
}

function openEdit(row: SystemUser) {
  editing.value = row;
  Object.assign(form, {
    email: row.email ?? '',
    nickname: row.nickname ?? '',
    password: '',
    phone: row.phone ?? '',
    roleIds: row.roleIds,
    status: row.status,
    username: row.username,
  });
  editOpen.value = true;
}

async function submit() {
  if (!editing.value && (!form.username.trim() || !form.password.trim())) {
    message.warning($t('system.user.accountPwdRequired'));
    return;
  }
  if (editing.value) {
    await updateUser(editing.value.id, {
      email: form.email,
      nickname: form.nickname,
      phone: form.phone,
      roleIds: form.roleIds,
      status: form.status,
    });
    message.success($t('system.user.updated'));
  } else {
    await createUser({ ...form });
    message.success($t('system.user.created'));
  }
  editOpen.value = false;
  await load();
}

async function doResetPassword(row: SystemUser) {
  await resetUserPassword(row.id, '123456');
  message.success($t('system.user.resetDone', { name: row.username }));
}

async function toggleStatus(row: SystemUser, status: number) {
  await changeUserStatus(row.id, status);
  message.success($t('system.user.statusChanged'));
  await load();
}

async function remove(row: SystemUser) {
  await deleteUser(row.id);
  message.success($t('system.user.deleted'));
  await load();
}

const columns = [
  { title: 'ID', dataIndex: 'id', width: 70 },
  { title: $t('system.user.username'), dataIndex: 'username', width: 140 },
  { title: $t('system.user.nickname'), dataIndex: 'nickname', width: 140 },
  { title: $t('system.user.email'), dataIndex: 'email' },
  { title: $t('system.user.phone'), dataIndex: 'phone', width: 140 },
  { title: $t('system.user.roles'), dataIndex: 'roleNames', width: 160 },
  { title: $t('system.user.status'), dataIndex: 'status', width: 90 },
  { title: $t('system.user.action'), width: 300, key: 'action' },
];

onMounted(async () => {
  roles.value = await allRoles();
  await load();
});
</script>

<template>
  <Page :title="$t('system.user.menu')" :description="$t('system.user.desc')">
    <div class="mb-4 flex flex-wrap items-center gap-2">
      <Input
        v-model:value="query.username"
        placeholder="账号"
        style="width: 180px"
        allow-clear
        @pressEnter="load"
      />
      <Button type="primary" @click="load">{{ $t('system.user.query') }}</Button>
      <Button @click="reset">{{ $t('system.user.reset') }}</Button>
      <div class="flex-1"></div>
      <Button
        v-if="hasAccessByCodes(['system:user:add'])"
        type="primary"
        @click="openCreate"
      >{{ $t('system.user.add') }}</Button>
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
        <template v-if="column.dataIndex === 'roleNames'">
          <Space wrap>
            <Tag v-for="name in record.roleNames" :key="name">{{ name }}</Tag>
          </Space>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <Switch
            :checked="record.status === 1"
            :checked-children="$t('system.user.enable')"
            :un-checked-children="$t('system.user.disable')"
            @change="(v: any) => toggleStatus(record as SystemUser, v ? 1 : 0)"
          />
        </template>
        <template v-else-if="column.key === 'action'">
          <Space>
            <Button
              v-if="hasAccessByCodes(['system:user:edit'])"
              size="small"
              @click="openEdit(record as SystemUser)"
            >{{ $t('system.user.edit') }}
            </Button>
            <Popconfirm
              v-if="hasAccessByCodes(['system:user:edit'])"
              :title="$t('system.user.resetConfirm')"
              @confirm="doResetPassword(record as SystemUser)"
            >
              <Button size="small">{{ $t('system.user.resetPwd') }}</Button>
            </Popconfirm>
            <Popconfirm
              v-if="hasAccessByCodes(['system:user:delete'])"
              :title="$t('system.user.delConfirm')"
              @confirm="remove(record as SystemUser)"
            >
              <Button danger size="small">{{ $t('system.user.del') }}</Button>
            </Popconfirm>
          </Space>
        </template>
      </template>
    </Table>

    <!-- 新增/编辑用户 -->
    <Modal v-model:open="editOpen" :title="editing ? $t('system.user.editUser') : $t('system.user.addUser')">
      <Form layout="vertical">
        <FormItem :label="$t('system.user.username')">
          <Input v-model:value="form.username" :disabled="!!editing" />
        </FormItem>
        <FormItem v-if="!editing" :label="$t('system.user.password')">
          <Input.Password v-model:value="form.password" :placeholder="$t('system.user.passwordPh')" />
        </FormItem>
        <FormItem :label="$t('system.user.nickname')">
          <Input v-model:value="form.nickname" />
        </FormItem>
        <FormItem :label="$t('system.user.email')">
          <Input v-model:value="form.email" />
        </FormItem>
        <FormItem :label="$t('system.user.phone')">
          <Input v-model:value="form.phone" />
        </FormItem>
        <FormItem :label="$t('system.user.bindRoles')">
          <Select
            v-model:value="form.roleIds"
            mode="multiple"
            placeholder="绑定角色"
            :options="roles.map((r) => ({ label: r.name, value: r.id }))"
          />
        </FormItem>
        <FormItem v-if="editing" :label="$t('system.user.status')">
          <Select
            v-model:value="form.status"
            :options="[
              { label: $t('system.user.enable'), value: 1 },
              { label: $t('system.user.disable'), value: 0 },
            ]"
          />
        </FormItem>
      </Form>
      <template #footer>
        <Button @click="editOpen = false">{{ $t('system.user.cancel') }}</Button>
        <Button type="primary" @click="submit">{{ $t('system.user.save') }}</Button>
      </template>
    </Modal>
  </Page>
</template>
