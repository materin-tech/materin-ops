<script lang="ts" setup>
// 独立工单创建页：报障入口，所有登录用户可用
import { onMounted, reactive, ref } from 'vue';

import { useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  Form,
  FormItem,
  Input,
  message,
  Select,
} from 'ant-design-vue';

import { createTicket } from '#/api/ticket';
import { $t } from '#/locales';
import RichEditor from '#/components/rich-editor/RichEditor.vue';
import { PRIORITY, useUserOptions } from '#/views/requirement/shared';

const router = useRouter();
onMounted(() => loadUsers());
const { loadUsers, userOptions } = useUserOptions();

const submitting = ref(false);
const form = reactive({
  assigneeId: undefined as undefined | number,
  description: '',
  priority: 2,
  title: '',
  type: 0,
});

const TICKET_TYPE = [
  { label: $t('ticket.tp.bug'), value: 0 },
  { label: $t('ticket.tp.consult'), value: 1 },
  { label: $t('ticket.tp.change'), value: 2 },
  { label: $t('ticket.tp.other'), value: 3 },
];

async function submit() {
  if (!form.title.trim()) {
    message.warning($t('ticket.titleRequired'));
    return;
  }
  submitting.value = true;
  try {
    await createTicket({ ...form });
    message.success($t('ticket.created'));
    router.push({ name: 'TicketList' });
  } finally {
    submitting.value = false;
  }
}

function reset() {
  Object.assign(form, { assigneeId: undefined, description: '', priority: 2, title: '', type: 0 });
}
</script>

<template>
  <Page :title="$t('ticket.createTitle')" :description="$t('ticket.createDesc')">
    <div class="grid grid-cols-1 gap-4 xl:grid-cols-3">
      <!-- 左：标题 + 描述 -->
      <Card class="xl:col-span-2" :title="$t('ticket.contentSection')">
        <Form layout="vertical">
          <FormItem :label="$t('ticket.titleCol')" required>
            <Input
              v-model:value="form.title"
              :placeholder="$t('ticket.titlePh2')"
              show-count
              :maxlength="200"
              size="large"
            />
          </FormItem>
          <FormItem :label="$t('requirement.task.description')">
            <RichEditor v-model:value="form.description" :height="420" />
          </FormItem>
        </Form>
      </Card>

      <!-- 右：属性 + 提交 -->
      <Card :title="$t('ticket.propsSection')">
        <Form layout="vertical">
          <FormItem :label="$t('requirement.task.type')">
            <Select
              v-model:value="form.type"
              :options="TICKET_TYPE.map((s) => ({ label: s.label, value: s.value }))"
            />
          </FormItem>
          <FormItem :label="$t('requirement.task.priority')">
            <Select
              v-model:value="form.priority"
              :options="PRIORITY.map((s) => ({ label: s.label, value: s.value }))"
            />
          </FormItem>
          <FormItem :label="$t('ticket.assignee')">
            <Select
              v-model:value="form.assigneeId"
              :placeholder="$t('ticket.assigneePh')"
              allow-clear
              :options="userOptions.map((u) => ({ label: u.nickname || u.username, value: u.id }))"
            />
          </FormItem>
        </Form>
        <div class="mt-2 flex flex-col gap-2">
          <Button :loading="submitting" size="large" type="primary" @click="submit">
            {{ $t('ticket.submit') }}
          </Button>
          <Button size="large" @click="reset">{{ $t('requirement.task.reset') }}</Button>
        </div>
        <div class="mt-4 rounded bg-blue-50 p-3 text-xs leading-5 text-blue-600 dark:bg-blue-950 dark:text-blue-300">
          {{ $t('ticket.slaHint') }}
        </div>
      </Card>
    </div>
  </Page>
</template>
