<script lang="ts" setup>
import type { SlaPolicy } from '#/api/ticket';

import { onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useAccess } from '@vben/access';

import { Button, Form, FormItem, InputNumber, message, Modal, Switch, Table } from 'ant-design-vue';

import { listSlas, updateSla } from '#/api/ticket';
import { $t } from '#/locales';

const { hasAccessByCodes } = useAccess();

const loading = ref(false);
const list = ref<SlaPolicy[]>([]);
const editOpen = ref(false);
const editing = ref<null | SlaPolicy>(null);
const form = reactive({
  enabled: 1,
  firstResponseMinutes: 60,
  name: '',
  resolveMinutes: 480,
});

async function load() {
  loading.value = true;
  try {
    list.value = await listSlas();
  } finally {
    loading.value = false;
  }
}

function openEdit(row: SlaPolicy) {
  editing.value = row;
  Object.assign(form, {
    enabled: row.enabled,
    firstResponseMinutes: row.firstResponseMinutes,
    name: row.name,
    resolveMinutes: row.resolveMinutes,
  });
  editOpen.value = true;
}

async function submit() {
  if (!editing.value) return;
  await updateSla(editing.value.id, { ...form });
  message.success($t('ticket.sla.saved'));
  editOpen.value = false;
  await load();
}

const PRIORITY_LABEL = ['P1', 'P2', 'P3', 'P4'];

const columns = [
  { title: 'ID', dataIndex: 'id', width: 70 },
  { title: $t('ticket.sla.name'), dataIndex: 'name' },
  { title: $t('ticket.sla.priority'), dataIndex: 'priority', width: 100 },
  { title: $t('ticket.sla.response'), dataIndex: 'firstResponseMinutes', width: 150 },
  { title: $t('ticket.sla.resolve'), dataIndex: 'resolveMinutes', width: 150 },
  { title: $t('ticket.sla.enabled'), dataIndex: 'enabled', width: 90 },
  { title: $t('ticket.sla.action'), width: 100, key: 'action' },
];

onMounted(load);
</script>

<template>
  <Page :title="$t('ticket.sla.menu')" :description="$t('ticket.sla.desc')">
    <Table :columns="columns" :data-source="list" :loading="loading" :pagination="false" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'priority'">
          <span class="font-medium">{{ PRIORITY_LABEL[record.priority] }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'firstResponseMinutes'">
          {{ record.firstResponseMinutes }} min
        </template>
        <template v-else-if="column.dataIndex === 'resolveMinutes'">
          {{ record.resolveMinutes }} min
        </template>
        <template v-else-if="column.dataIndex === 'enabled'">
          <Switch :checked="record.enabled === 1" disabled />
        </template>
        <template v-else-if="column.key === 'action'">
          <Button
            v-if="hasAccessByCodes(['ticket:sla:edit'])"
            size="small"
            @click="openEdit(record as SlaPolicy)"
          >
            {{ $t('ticket.sla.edit') }}
          </Button>
        </template>
      </template>
    </Table>

    <Modal v-model:open="editOpen" :title="$t('ticket.sla.editTitle')">
      <Form layout="vertical">
        <FormItem :label="$t('ticket.sla.name')">
          <span class="font-medium">{{ form.name }}</span>
        </FormItem>
        <FormItem :label="$t('ticket.sla.response')">
          <InputNumber v-model:value="form.firstResponseMinutes" :min="1" :step="15" add-on-after="min" />
        </FormItem>
        <FormItem :label="$t('ticket.sla.resolve')">
          <InputNumber v-model:value="form.resolveMinutes" :min="1" :step="30" add-on-after="min" />
        </FormItem>
        <FormItem :label="$t('ticket.sla.enabled')">
          <Switch
            :checked="form.enabled === 1"
            @change="(v: any) => (form.enabled = v ? 1 : 0)"
          />
        </FormItem>
      </Form>
      <template #footer>
        <Button @click="editOpen = false">{{ $t('ticket.sla.cancel') }}</Button>
        <Button type="primary" @click="submit">{{ $t('ticket.sla.save') }}</Button>
      </template>
    </Modal>
  </Page>
</template>
