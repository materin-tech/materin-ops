<script lang="ts" setup>
import type { Product, ProductMember, ProductModuleNode } from '#/api/requirement';

import { onMounted, reactive, ref } from 'vue';

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
  SelectOption,
  Space,
  Table,
  Tag,
  Tree,
  Typography,
} from 'ant-design-vue';

import { $t } from '#/locales';
import RichEditor from '#/components/rich-editor/RichEditor.vue';

import { flattenModules, useUserOptions } from '#/views/requirement/shared';

import {
  createProduct,
  createProductModule,
  deleteProduct,
  deleteProductModule,
  listProducts,
  productMembers,
  productModuleTree,
  saveProductMembers,
  updateProduct,
  updateProductModule,
} from '#/api/requirement';

const { hasAccessByCodes } = useAccess();

const loading = ref(false);
const list = ref<Product[]>([]);
const keyword = ref('');
const statusFilter = ref<number | undefined>(undefined);

async function load() {
  loading.value = true;
  try {
    list.value = await listProducts({
      keyword: keyword.value || undefined,
      status: statusFilter.value,
    });
  } finally {
    loading.value = false;
  }
}

// ---------- 新增 / 编辑 ----------
const editOpen = ref(false);
const editing = ref<null | Product>(null);
const form = reactive({ code: '', description: '', name: '', status: 1 });

function openCreate() {
  editing.value = null;
  Object.assign(form, { code: '', description: '', name: '', status: 1 });
  editOpen.value = true;
}

function openEdit(row: Product) {
  editing.value = row;
  Object.assign(form, {
    code: row.code,
    description: row.description ?? '',
    name: row.name,
    status: row.status,
  });
  editOpen.value = true;
}

async function submit() {
  if (!form.name.trim() || !form.code.trim()) {
    message.warning($t('requirement.product.nameRequired'));
    return;
  }
  if (editing.value) {
    await updateProduct(editing.value.id, { ...form });
    message.success($t('requirement.product.updated'));
  } else {
    await createProduct({ ...form });
    message.success($t('requirement.product.created'));
  }
  editOpen.value = false;
  await load();
}

async function remove(row: Product) {
  await deleteProduct(row.id);
  message.success($t('requirement.product.deletedMsg'));
  await load();
}

// ---------- 模块树 ----------
const moduleOpen = ref(false);
const moduleProductId = ref<number>(0);
const moduleTree = ref<ProductModuleNode[]>([]);
const moduleForm = reactive({ name: '', parentId: 0, sort: 0 });
const editingModule = ref<null | ProductModuleNode>(null);
const expandedKeys = ref<number[]>([]);
const currentProduct = ref<null | Product>(null);

async function openModules(row: Product) {
  currentProduct.value = row;
  moduleProductId.value = row.id;
  moduleOpen.value = true;
  await reloadModules();
}

async function reloadModules() {
  moduleTree.value = await productModuleTree(moduleProductId.value);
}

function resetModuleForm() {
  editingModule.value = null;
  Object.assign(moduleForm, { name: '', parentId: 0, sort: 0 });
}

async function submitModule() {
  if (!moduleForm.name.trim()) {
    message.warning($t('requirement.product.moduleNameRequired'));
    return;
  }
  if (editingModule.value) {
    await updateProductModule(editingModule.value.id, { ...moduleForm });
  } else {
    await createProductModule(moduleProductId.value, { ...moduleForm });
  }
  message.success($t('requirement.product.save'));
  resetModuleForm();
  await reloadModules();
}

function editModule(node: ProductModuleNode) {
  editingModule.value = node;
  Object.assign(moduleForm, {
    name: node.name,
    parentId: node.parentId,
    sort: node.sort,
  });
}

async function removeModule(node: ProductModuleNode) {
  await deleteProductModule(node.id);
  message.success($t('requirement.product.deletedMsg'));
  await reloadModules();
}

// ---------- 成员授权 ----------
const memberOpen = ref(false);
const members = ref<ProductMember[]>([]);
const memberRows = ref<ProductMember[]>([]);
const { loadUsers, userOptions } = useUserOptions();

async function openMembers(row: Product) {
  memberOpen.value = true;
  currentProduct.value = row;
  await loadUsers();
  members.value = await productMembers(row.id);
  memberRows.value = members.value.map((m) => ({ ...m }));
}

function addMemberRow() {
  memberRows.value.push({ role: 'member', userId: 0 });
}

function removeMemberRow(index: number) {
  memberRows.value.splice(index, 1);
}

async function submitMembers() {
  const valid = memberRows.value.filter((m) => m.userId > 0);
  await saveProductMembers(moduleProductId.value, valid);
  message.success($t('requirement.product.memberSaved'));
  memberOpen.value = false;
}

onMounted(load);

const columns = [
  { title: 'ID', dataIndex: 'id', width: 70 },
  { title: $t('requirement.product.name'), dataIndex: 'name' },
  { title: $t('requirement.product.code'), dataIndex: 'code', width: 160 },
  { title: $t('requirement.product.status'), dataIndex: 'status', width: 90 },
  { title: $t('requirement.product.descLabel'), dataIndex: 'description' },
  { title: $t('requirement.product.action'), width: 280, key: 'action' },
];
</script>

<template>
  <Page :title="$t('requirement.product.menu')" :description="$t('requirement.product.desc')">
    <div class="mb-4 flex items-center gap-2">
      <Input
        v-model:value="keyword"
        placeholder="名称/编码"
        style="width: 220px"
        allow-clear
        @pressEnter="load"
      />
      <Select
        v-model:value="statusFilter"
        placeholder="状态"
        style="width: 120px"
        allow-clear
      >
        <SelectOption :value="1">{{ $t('requirement.product.enable') }}</SelectOption>
        <SelectOption :value="0">{{ $t('requirement.product.disable') }}</SelectOption>
      </Select>
      <Button type="primary" @click="load">{{ $t('requirement.product.query') }}</Button>
      <div class="flex-1"></div>
      <Button
        v-if="hasAccessByCodes(['requirement:product:add'])"
        type="primary"
        @click="openCreate"
      >{{ $t('requirement.product.add') }}</Button>
    </div>

    <Table
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="false"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <Space>
            <Button
              v-if="hasAccessByCodes(['requirement:product:edit'])"
              size="small"
              @click="openEdit(record as Product)"
            >
              {{ $t('requirement.product.edit') }}
            </Button>
            <Button size="small" @click="openModules(record as Product)">{{ $t('requirement.product.modules') }}</Button>
            <Button
              v-if="hasAccessByCodes(['requirement:product:member'])"
              size="small"
              @click="openMembers(record as Product)"
            >
              {{ $t('requirement.product.members') }}
            </Button>
            <Popconfirm
              v-if="hasAccessByCodes(['requirement:product:delete'])"
              title="确认删除该产品？"
              @confirm="remove(record as Product)"
            >
              <Button danger size="small">{{ $t('requirement.product.delete') }}</Button>
            </Popconfirm>
          </Space>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <Tag :color="record.status === 1 ? 'green' : 'default'">
            {{ record.status === 1 ? $t('requirement.product.enable') : $t('requirement.product.disable') }}
          </Tag>
        </template>
      </template>
    </Table>

    <!-- 新增/编辑产品 -->
    <Modal v-model:open="editOpen" :title="editing ? $t('requirement.product.editProduct') : $t('requirement.product.addProduct')">
      <Form layout="vertical">
        <FormItem :label="$t('requirement.product.name')">
          <Input v-model:value="form.name" :placeholder="$t('requirement.product.name')" />
        </FormItem>
        <FormItem :label="$t('requirement.product.code')">
          <Input v-model:value="form.code" :placeholder="$t('requirement.product.codePh')" :disabled="!!editing" />
        </FormItem>
        <FormItem :label="$t('requirement.product.descLabel')">
          <RichEditor v-model:value="form.description" :height="200" />
        </FormItem>
        <FormItem :label="$t('requirement.product.status')">
          <Select v-model:value="form.status">
            <SelectOption :value="1">{{ $t('requirement.product.enable') }}</SelectOption>
            <SelectOption :value="0">{{ $t('requirement.product.disable') }}</SelectOption>
          </Select>
        </FormItem>
      </Form>
      <template #footer>
        <Button @click="editOpen = false">{{ $t('requirement.product.cancel') }}</Button>
        <Button type="primary" @click="submit">{{ $t('requirement.product.save') }}</Button>
      </template>
    </Modal>

    <!-- 模块树抽屉 -->
    <Drawer
      v-model:open="moduleOpen"
      :title="`${$t('requirement.product.moduleTree')} - ${currentProduct?.name ?? ''}`"
      width="520"
    >
      <Form layout="inline" class="mb-4">
        <FormItem :label="$t('requirement.product.parent')">
          <Select v-model:value="moduleForm.parentId" style="width: 140px">
            <SelectOption :value="0">{{ $t('requirement.product.top') }}</SelectOption>
            <SelectOption
              v-for="flat in flattenModules(moduleTree)"
              :key="flat.id"
              :value="flat.id"
            >
              {{ flat.name }}
            </SelectOption>
          </Select>
        </FormItem>
        <FormItem :label="$t('requirement.product.name')">
          <Input v-model:value="moduleForm.name" style="width: 140px" />
        </FormItem>
        <Button type="primary" @click="submitModule">{{ editingModule ? $t('requirement.product.moduleUpdate') : $t('requirement.product.moduleAdd') }}</Button>
        <Button v-if="editingModule" class="ml-2" @click="resetModuleForm">{{ $t('requirement.product.cancelEdit') }}</Button>
      </Form>
      <Tree
        v-if="moduleTree.length > 0"
        v-model:expandedKeys="expandedKeys"
        :tree-data="(moduleTree as any)"
        :field-names="{ children: 'children', title: 'name', key: 'id' }"
      >
        <template #title="{ dataRef }">
          <Space>
            <span>{{ dataRef.name }}</span>
            <Button size="small" type="link" @click.stop="editModule(dataRef)">{{ $t('requirement.product.edit') }}</Button>
            <Popconfirm :title="$t('requirement.product.confirmDeleteModule')" @confirm="removeModule(dataRef)">
              <Button size="small" type="link" danger @click.stop>{{ $t('requirement.product.delete') }}</Button>
            </Popconfirm>
          </Space>
        </template>
      </Tree>
      <Typography v-else type="secondary">{{ $t('requirement.product.noModule') }}</Typography>
    </Drawer>

    <!-- 成员授权抽屉 -->
    <Drawer
      v-model:open="memberOpen"
      :title="`${$t('requirement.product.memberAuth')} - ${currentProduct?.name ?? ''}`"
      width="560"
    >
      <Button class="mb-4" @click="addMemberRow">{{ $t('requirement.product.memberAdd') }}</Button>
      <div v-for="(m, i) in memberRows" :key="i" class="mb-2 flex items-center gap-2">
        <Select
          v-model:value="m.userId"
          :placeholder="$t('requirement.product.chooseUser')"
          style="width: 240px"
          :options="
            userOptions.map((u) => ({
              label: u.nickname || u.username,
              value: u.id,
            }))
          "
        />
        <Select v-model:value="m.role" style="width: 120px">
          <SelectOption value="owner">{{ $t('requirement.product.roleOwner') }}</SelectOption>
          <SelectOption value="dev">{{ $t('requirement.product.roleDev') }}</SelectOption>
          <SelectOption value="qa">{{ $t('requirement.product.roleQa') }}</SelectOption>
          <SelectOption value="member">{{ $t('requirement.product.roleMember') }}</SelectOption>
        </Select>
        <Button danger size="small" @click="removeMemberRow(i)">{{ $t('requirement.product.memberRemove') }}</Button>
      </div>
      <template #footer>
        <Button @click="memberOpen = false">{{ $t('requirement.product.cancel') }}</Button>
        <Button type="primary" @click="submitMembers">{{ $t('requirement.product.save') }}</Button>
      </template>
    </Drawer>
  </Page>
</template>
