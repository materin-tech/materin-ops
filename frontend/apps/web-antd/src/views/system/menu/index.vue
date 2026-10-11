<script lang="ts" setup>
import type { MenuNode } from '#/api/system';

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
  SelectOption,
  Space,
  Switch,
  Tag,
  Tree,
} from 'ant-design-vue';

import { $t } from '#/locales';

import {
  createMenu,
  deleteMenu,
  menuTree,
  updateMenu,
} from '#/api/system';

const { hasAccessByCodes } = useAccess();

const loading = ref(false);
const treeData = ref<MenuNode[]>([]);
const expandedKeys = ref<number[]>([]);

async function load() {
  loading.value = true;
  try {
    treeData.value = await menuTree();
    expandedKeys.value = collectParentIds(treeData.value);
  } finally {
    loading.value = false;
  }
}

function collectParentIds(nodes: MenuNode[], acc: number[] = []): number[] {
  for (const n of nodes) {
    if (n.children?.length) {
      acc.push(n.id);
      collectParentIds(n.children, acc);
    }
  }
  return acc;
}

const TYPE_TAG: Record<string, string> = {
  B: 'orange',
  D: 'blue',
  M: 'green',
};
const TYPE_LABEL: Record<string, string> = {
  B: '按钮',
  D: '目录',
  M: '菜单',
};

// ---------- 新增 / 编辑 ----------
const editOpen = ref(false);
const editing = ref<null | MenuNode>(null);
const form = reactive({
  component: '',
  icon: '',
  name: '',
  parentId: 0,
  path: '',
  perm: '',
  sort: 0,
  status: 1,
  type: 'M' as 'B' | 'D' | 'M',
  visible: 1,
});
const parentOptions = ref<{ id: number; name: string; type: string }[]>([]);

function flattenParents(nodes: MenuNode[], acc: { id: number; name: string; type: string }[] = []) {
  for (const n of nodes) {
    if (n.type !== 'B') acc.push({ id: n.id, name: n.name, type: n.type });
    if (n.children?.length) flattenParents(n.children, acc);
  }
  return acc;
}

function openCreate(parentId = 0, type: 'B' | 'D' | 'M' = 'M') {
  editing.value = null;
  Object.assign(form, {
    component: '',
    icon: '',
    name: '',
    parentId,
    path: '',
    perm: '',
    sort: 0,
    status: 1,
    type,
    visible: 1,
  });
  editOpen.value = true;
}

function openEdit(row: MenuNode) {
  editing.value = row;
  Object.assign(form, {
    component: row.component ?? '',
    icon: row.icon ?? '',
    name: row.name,
    parentId: row.parentId,
    path: row.path ?? '',
    perm: row.perm ?? '',
    sort: row.sort ?? 0,
    status: row.status ?? 1,
    type: row.type as 'B' | 'D' | 'M',
    visible: row.visible ?? 1,
  });
  editOpen.value = true;
}

async function submit() {
  if (!form.name.trim()) {
    message.warning($t('system.menuPage.nameRequired'));
    return;
  }
  const payload = {
    ...form,
    component: form.component || null,
    icon: form.icon || null,
    path: form.path || null,
    perm: form.perm || null,
  };
  if (editing.value) {
    await updateMenu(editing.value.id, payload);
    message.success($t('system.menuPage.updated'));
  } else {
    await createMenu(payload);
    message.success($t('system.menuPage.created'));
  }
  editOpen.value = false;
  await load();
}

async function remove(row: MenuNode) {
  await deleteMenu(row.id);
  message.success($t('system.menuPage.deleted'));
  await load();
}

onMounted(load);
</script>

<template>
  <Page :title="$t('system.menuPage.menu')" :description="$t('system.menuPage.desc')">
    <div class="mb-4 flex items-center gap-2">
      <Button @click="() => { parentOptions = flattenParents(treeData); openCreate(0, 'D'); }">{{ $t('system.menuPage.addDir') }}</Button>
      <div class="flex-1"></div>
      <Button type="primary" @click="load">{{ $t('system.menuPage.refresh') }}</Button>
    </div>

    <Tree
      v-if="treeData.length > 0"
      v-model:expandedKeys="expandedKeys"
      :tree-data="(treeData as any)"
      :field-names="{ children: 'children', title: 'name', key: 'id' }"
    >
      <template #title="{ dataRef }">
        <Space wrap>
          <Tag :color="TYPE_TAG[dataRef.type]">{{ TYPE_LABEL[dataRef.type] }}</Tag>
          <span>{{ dataRef.name }}</span>
          <span v-if="dataRef.perm" class="text-gray-400">{{ dataRef.perm }}</span>
          <span v-if="dataRef.path" class="text-gray-400">{{ dataRef.path }}</span>
          <Switch
            v-if="dataRef.type !== 'B'"
            :checked="dataRef.status === 1"
            size="small"
          />
          <template v-if="hasAccessByCodes(['system:menu:add'])">
            <Button
              v-if="dataRef.type === 'D'"
              size="small"
              type="link"
              @click.stop="() => { parentOptions = flattenParents(treeData); openCreate(dataRef.id, 'M'); }"
            >{{ $t('system.menuPage.addMenu') }}
            </Button>
            <Button
              v-else-if="dataRef.type === 'M'"
              size="small"
              type="link"
              @click.stop="() => { parentOptions = flattenParents(treeData); openCreate(dataRef.id, 'B'); }"
            >{{ $t('system.menuPage.addButton') }}
            </Button>
          </template>
          <Button
            v-if="hasAccessByCodes(['system:menu:edit'])"
            size="small"
            type="link"
            @click.stop="() => { parentOptions = flattenParents(treeData); openEdit(dataRef); }"
          >{{ $t('system.menuPage.edit') }}
          </Button>
          <Popconfirm
            v-if="hasAccessByCodes(['system:menu:delete'])"
            :title="$t('system.menuPage.delConfirm')"
            @confirm="remove(dataRef as MenuNode)"
          >
            <Button size="small" type="link" danger @click.stop>{{ $t('system.menuPage.del') }}</Button>
          </Popconfirm>
        </Space>
      </template>
    </Tree>

    <!-- 新增/编辑菜单 -->
    <Modal v-model:open="editOpen" :title="editing ? '编辑菜单' : '新增菜单'">
      <Form layout="vertical">
        <FormItem :label="$t('system.menuPage.type')">
          <Select v-model:value="form.type">
            <SelectOption value="D">{{ $t('system.menuPage.typeD') }}</SelectOption>
            <SelectOption value="M">{{ $t('system.menuPage.typeM') }}</SelectOption>
            <SelectOption value="B">{{ $t('system.menuPage.typeB') }}</SelectOption>
          </Select>
        </FormItem>
        <FormItem :label="$t('system.menuPage.parent')">
          <Select v-model:value="form.parentId">
            <SelectOption :value="0">{{ $t('system.menuPage.top') }}</SelectOption>
            <SelectOption v-for="p in parentOptions" :key="p.id" :value="p.id">
              {{ p.name }}
            </SelectOption>
          </Select>
        </FormItem>
        <FormItem :label="$t('system.menuPage.name')">
          <Input v-model:value="form.name" />
        </FormItem>
        <FormItem v-if="form.type !== 'B'" :label="$t('system.menuPage.path')">
          <Input v-model:value="form.path" placeholder="/system/users" />
        </FormItem>
        <FormItem v-if="form.type === 'M'" :label="$t('system.menuPage.component')">
          <Input v-model:value="form.component" placeholder="system/user/index" />
        </FormItem>
        <FormItem v-if="form.type === 'B' || form.type === 'M'" :label="$t('system.menuPage.perm')">
          <Input v-model:value="form.perm" placeholder="system:user:add" />
        </FormItem>
        <FormItem v-if="form.type !== 'B'" :label="$t('system.menuPage.icon')">
          <Input v-model:value="form.icon" placeholder="lucide:user" />
        </FormItem>
        <FormItem :label="$t('system.menuPage.sort')">
          <Input v-model:value="form.sort" type="number" />
        </FormItem>
      </Form>
      <template #footer>
        <Button @click="editOpen = false">{{ $t('system.menuPage.cancel') }}</Button>
        <Button type="primary" @click="submit">{{ $t('system.menuPage.save') }}</Button>
      </template>
    </Modal>
  </Page>
</template>
