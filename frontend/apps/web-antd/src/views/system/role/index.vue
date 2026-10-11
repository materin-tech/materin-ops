<script lang="ts" setup>
import type { SystemRole } from '#/api/system';

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
  Space,
  Switch,
  Table,
  Textarea,
  Tree,
} from 'ant-design-vue';

import { $t } from '#/locales';

import {
  assignRoleMenus,
  createRole,
  deleteRole,
  pageRoles,
  updateRole,
} from '#/api/system';
import { menuTree } from '#/api/system';

import type { MenuNode } from '#/api/system';

const { hasAccessByCodes } = useAccess();

const loading = ref(false);
const list = ref<SystemRole[]>([]);
const total = ref(0);
const query = reactive({
  code: '',
  name: '',
  page: 1,
  pageSize: 20,
});

async function load() {
  loading.value = true;
  try {
    const res = await pageRoles({
      code: query.code || undefined,
      name: query.name || undefined,
      page: query.page,
      pageSize: query.pageSize,
    });
    list.value = res.list;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
}

function reset() {
  Object.assign(query, { code: '', name: '', page: 1 });
  load();
}

// ---------- 新增 / 编辑 ----------
const editOpen = ref(false);
const editing = ref<null | SystemRole>(null);
const form = reactive({
  code: '',
  name: '',
  remark: '',
  sort: 0,
  status: 1,
});

function openCreate() {
  editing.value = null;
  Object.assign(form, { code: '', name: '', remark: '', sort: 0, status: 1 });
  editOpen.value = true;
}

function openEdit(row: SystemRole) {
  editing.value = row;
  Object.assign(form, {
    code: row.code,
    name: row.name,
    remark: row.remark ?? '',
    sort: row.sort,
    status: row.status,
  });
  editOpen.value = true;
}

async function submit() {
  if (!form.name.trim() || (!editing.value && !form.code.trim())) {
    message.warning($t('system.role.nameCodeRequired'));
    return;
  }
  if (editing.value) {
    await updateRole(editing.value.id, {
      name: form.name,
      remark: form.remark,
      sort: form.sort,
      status: form.status,
    });
    message.success($t('system.role.updated'));
  } else {
    await createRole({ ...form });
    message.success($t('system.role.created'));
  }
  editOpen.value = false;
  await load();
}

// ---------- 菜单授权 ----------
const menuOpen = ref(false);
const currentRole = ref<null | SystemRole>(null);
const treeData = ref<MenuNode[]>([]);
const checkedKeys = ref<number[]>([]);
const expandedKeys = ref<number[]>([]);

async function openMenus(row: SystemRole) {
  currentRole.value = row;
  treeData.value = await menuTree();
  checkedKeys.value = row.menuIds;
  menuOpen.value = true;
}

function collectIds(nodes: MenuNode[], acc: number[] = []): number[] {
  for (const n of nodes) {
    acc.push(n.id);
    if (n.children?.length) collectIds(n.children, acc);
  }
  return acc;
}

async function submitMenus() {
  if (!currentRole.value) return;
  await assignRoleMenus(currentRole.value.id, checkedKeys.value);
  message.success($t('system.role.authSaved'));
  menuOpen.value = false;
  await load();
}

async function toggleStatus(row: SystemRole, status: number) {
  await updateRole(row.id, { status });
  message.success($t('system.role.statusChanged'));
  await load();
}

async function remove(row: SystemRole) {
  await deleteRole(row.id);
  message.success($t('system.role.deleted'));
  await load();
}

const columns = [
  { title: 'ID', dataIndex: 'id', width: 70 },
  { title: $t('system.role.name'), dataIndex: 'name', width: 160 },
  { title: $t('system.role.code'), dataIndex: 'code', width: 160 },
  { title: $t('system.role.remark'), dataIndex: 'remark' },
  { title: $t('system.role.status'), dataIndex: 'status', width: 90 },
  { title: $t('system.role.action'), width: 280, key: 'action' },
];

onMounted(load);
</script>

<template>
  <Page :title="$t('system.role.menu')" :description="$t('system.role.desc')">
    <div class="mb-4 flex flex-wrap items-center gap-2">
      <Input v-model:value="query.name" placeholder="角色名称" style="width: 160px" allow-clear @pressEnter="load" />
      <Input v-model:value="query.code" placeholder="编码" style="width: 160px" allow-clear @pressEnter="load" />
      <Button type="primary" @click="load">{{ $t('system.role.query') }}</Button>
      <Button @click="reset">{{ $t('system.role.reset') }}</Button>
      <div class="flex-1"></div>
      <Button
        v-if="hasAccessByCodes(['system:role:add'])"
        type="primary"
        @click="openCreate"
      >{{ $t('system.role.add') }}</Button>
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
          <Switch
            :checked="record.status === 1"
            :checked-children="$t('system.user.enable')"
            :un-checked-children="$t('system.user.disable')"
            @change="(v: any) => toggleStatus(record as SystemRole, v ? 1 : 0)"
          />
        </template>
        <template v-else-if="column.key === 'action'">
          <Space>
            <Button size="small" @click="openMenus(record as SystemRole)">{{ $t('system.role.menuAuth') }}</Button>
            <Button
              v-if="hasAccessByCodes(['system:role:edit'])"
              size="small"
              @click="openEdit(record as SystemRole)"
            >{{ $t('system.role.edit') }}
            </Button>
            <Popconfirm
              v-if="hasAccessByCodes(['system:role:delete'])"
              :title="$t('system.role.delConfirm')"
              @confirm="remove(record as SystemRole)"
            >
              <Button danger size="small">{{ $t('system.role.del') }}</Button>
            </Popconfirm>
          </Space>
        </template>
      </template>
    </Table>

    <!-- 新增/编辑角色 -->
    <Modal v-model:open="editOpen" :title="editing ? $t('system.role.editRole') : $t('system.role.addRole')">
      <Form layout="vertical">
        <FormItem :label="$t('system.role.name')">
          <Input v-model:value="form.name" />
        </FormItem>
        <FormItem :label="$t('system.role.code')">
          <Input v-model:value="form.code" :disabled="!!editing" />
        </FormItem>
        <FormItem :label="$t('system.role.sort')">
          <Input v-model:value="form.sort" type="number" />
        </FormItem>
        <FormItem :label="$t('system.role.remark')">
          <Textarea v-model:value="form.remark" :rows="2" />
        </FormItem>
        <FormItem v-if="editing" :label="$t('system.role.status')">
          <Switch
            :checked="form.status === 1"
            :checked-children="$t('system.user.enable')"
            :un-checked-children="$t('system.user.disable')"
            @change="(v: any) => (form.status = v ? 1 : 0)"
          />
        </FormItem>
      </Form>
      <template #footer>
        <Button @click="editOpen = false">{{ $t('system.role.cancel') }}</Button>
        <Button type="primary" @click="submit">{{ $t('system.role.save') }}</Button>
      </template>
    </Modal>

    <!-- 菜单授权 -->
    <Modal v-model:open="menuOpen" :title="`菜单授权 - ${currentRole?.name ?? ''}`" width="520">
      <div class="mb-2 flex gap-2">
        <Button
          size="small"
          @click="checkedKeys = collectIds(treeData)"
        >
          全选
        </Button>
        <Button size="small" @click="checkedKeys = []">{{ $t('system.role.clear') }}</Button>
      </div>
      <Tree
        v-model:checkedKeys="checkedKeys"
        v-model:expandedKeys="expandedKeys"
        checkable
        :tree-data="(treeData as any)"
        :field-names="{ children: 'children', title: 'name', key: 'id' }"
      />
      <template #footer>
        <Button @click="menuOpen = false">{{ $t('system.role.cancel') }}</Button>
        <Button type="primary" @click="submitMenus">{{ $t('system.role.save') }}</Button>
      </template>
    </Modal>
  </Page>
</template>
