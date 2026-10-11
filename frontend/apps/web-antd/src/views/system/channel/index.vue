<script lang="ts" setup>
import type { NotifyChannel } from '#/api/notice/channel';

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
  Textarea,
} from 'ant-design-vue';

import { deleteChannel, listChannels, saveChannel, testChannel } from '#/api/notice/channel';
import { $t } from '#/locales';

const { hasAccessByCodes } = useAccess();

const loading = ref(false);
const list = ref<NotifyChannel[]>([]);
const editOpen = ref(false);
const testing = ref(false);
const editing = ref<null | NotifyChannel>(null);
const form = reactive({
  enabled: 1,
  name: '',
  secret: '',
  type: 'wecom' as 'dingtalk' | 'feishu' | 'wecom',
  webhookUrl: '',
});

const TYPE_TAG: Record<string, string> = {
  dingtalk: 'blue',
  feishu: 'purple',
  wecom: 'green',
};
const TYPE_LABEL: Record<string, string> = {
  dingtalk: '钉钉机器人',
  feishu: '飞书机器人',
  wecom: '企业微信机器人',
};

async function load() {
  loading.value = true;
  try {
    list.value = await listChannels();
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  editing.value = null;
  Object.assign(form, { enabled: 1, name: '', secret: '', type: 'wecom', webhookUrl: '' });
  editOpen.value = true;
}

function openEdit(row: NotifyChannel) {
  editing.value = row;
  Object.assign(form, {
    enabled: row.enabled,
    name: row.name,
    secret: row.secret ?? '',
    type: row.type,
    webhookUrl: row.webhookUrl,
  });
  editOpen.value = true;
}

async function submit() {
  if (!form.name.trim() || !form.webhookUrl.trim()) {
    message.warning('名称与 Webhook 必填');
    return;
  }
  await saveChannel({ id: editing.value?.id, ...form } as NotifyChannel);
  message.success('已保存');
  editOpen.value = false;
  await load();
}

async function remove(row: NotifyChannel) {
  await deleteChannel((row as NotifyChannel).id!);
  message.success('已删除');
  await load();
}

async function doTest(row: NotifyChannel) {
  testing.value = true;
  try {
    await testChannel((row as NotifyChannel).id!);
    message.success(`测试消息已推送，请到 ${TYPE_LABEL[row.type]} 群查看`);
  } catch (e: any) {
    message.error(e?.message || '渠道调用失败');
  } finally {
    testing.value = false;
  }
}

const columns = [
  { title: 'ID', dataIndex: 'id', width: 70 },
  { title: '渠道', dataIndex: 'type', width: 140 },
  { title: '名称', dataIndex: 'name', width: 180 },
  { title: 'Webhook', dataIndex: 'webhookUrl', ellipsis: true },
  { title: '加签', dataIndex: 'secret', width: 80 },
  { title: '启用', dataIndex: 'enabled', width: 90 },
  { title: '操作', width: 230, key: 'action' },
];

onMounted(load);
</script>

<template>
  <Page :title="$t('system.channel.menu')" description="企业微信 / 钉钉 / 飞书群机器人：广播通知时同步推送">
    <div class="mb-4 flex items-center gap-2">
      <div class="flex-1"></div>
      <Button
        v-if="hasAccessByCodes(['notice:channel:edit'])"
        type="primary"
        @click="openCreate"
      >
        新增渠道
      </Button>
    </div>

    <Table
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="false"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'type'">
          <Tag :color="TYPE_TAG[record.type]">{{ TYPE_LABEL[record.type] }}</Tag>
        </template>
        <template v-else-if="column.dataIndex === 'secret'">
          <Tag v-if="record.secret" color="orange">加签</Tag>
          <span v-else class="text-gray-300">-</span>
        </template>
        <template v-else-if="column.dataIndex === 'enabled'">
          <Switch :checked="record.enabled === 1" disabled />
        </template>
        <template v-else-if="column.key === 'action'">
          <Space>
            <Button
              v-if="hasAccessByCodes(['notice:channel:test'])"
              :loading="testing"
              size="small"
              @click="doTest(record as NotifyChannel)"
            >
              测试
            </Button>
            <Button
              v-if="hasAccessByCodes(['notice:channel:edit'])"
              size="small"
              @click="openEdit(record as NotifyChannel)"
            >
              编辑
            </Button>
            <Popconfirm
              v-if="hasAccessByCodes(['notice:channel:edit'])"
              title="确认删除该渠道？"
              @confirm="remove(record as NotifyChannel)"
            >
              <Button danger size="small">删除</Button>
            </Popconfirm>
          </Space>
        </template>
      </template>
    </Table>

    <Modal v-model:open="editOpen" :title="editing ? '编辑渠道' : '新增渠道'">
      <Form layout="vertical">
        <FormItem label="渠道类型">
          <Select
            v-model:value="form.type"
            :disabled="!!editing"
            :options="[
              { label: '企业微信机器人', value: 'wecom' },
              { label: '钉钉机器人', value: 'dingtalk' },
              { label: '飞书机器人', value: 'feishu' },
            ]"
          />
        </FormItem>
        <FormItem label="渠道名称">
          <Input v-model:value="form.name" placeholder="如：运维值班群" />
        </FormItem>
        <FormItem label="Webhook 地址">
          <Textarea
            v-model:value="form.webhookUrl"
            :rows="2"
            placeholder="机器人的完整 Webhook 地址"
          />
        </FormItem>
        <FormItem label="加签密钥（钉钉安全设置，其他渠道留空）">
          <Input v-model:value="form.secret" placeholder="SEC 开头的加签密钥（可空）" />
        </FormItem>
        <FormItem label="启用">
          <Switch
            :checked="form.enabled === 1"
            @change="(v: any) => (form.enabled = v ? 1 : 0)"
          />
        </FormItem>
      </Form>
      <template #footer>
        <Button @click="editOpen = false">取消</Button>
        <Button type="primary" @click="submit">保存</Button>
      </template>
    </Modal>
  </Page>
</template>
