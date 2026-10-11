<script setup>
import { showDialog, showSuccessToast, showToast } from 'vant';
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import {
  acceptTicket,
  changeTicketStatus,
  closeTicket,
  commentTicket,
  delegateTicket,
  listUsers,
  ticketActions,
  ticketDetail,
} from '../../api';

const route = useRoute();
const router = useRouter();
const id = route.params.id;
const ticket = ref(null);
const actions = ref([]);
const showDelegate = ref(false);
const showDelegatePicker = ref(false);
const delegateUser = ref('');
const showComment = ref(false);
const comment = ref('');
const users = ref([]);

const STATUS = ['待受理', '处理中', '待确认', '已解决', '已关闭'];
const STATUS_COLOR = ['default', 'primary', 'warning', 'success', 'default'];
const ACTIONS = { 0: '创建', 1: '受理', 2: '转派', 3: '备注', 4: '状态变更', 5: '解决', 6: '关闭' };

function fmt(v) {
  return v ? String(v).replace('T', ' ').slice(0, 19) : '-';
}

async function load() {
  const [t, a] = await Promise.all([ticketDetail(id), ticketActions(id)]);
  ticket.value = t;
  actions.value = a;
}

async function doAccept() {
  await acceptTicket(id);
  showSuccessToast('已受理');
  await load();
}

async function doStatus(s, tip) {
  await changeTicketStatus(id, s);
  showSuccessToast(tip);
  await load();
}

async function doClose() {
  await showConfirmDialog({ title: '关闭工单', message: '确认关闭该工单？' });
  await closeTicket(id);
  showSuccessToast('已关闭');
  await load();
}

async function doComment() {
  if (!comment.value.trim()) {
    showToast('请输入备注内容');
    return;
  }
  await commentTicket(id, comment.value);
  showSuccessToast('备注已提交');
  showComment.value = false;
  comment.value = '';
  await load();
}

async function doDelegate() {
  if (!delegateUser.value) {
    showToast('请选择处理人');
    return;
  }
  await delegateTicket(id, delegateUser.value);
  showSuccessToast('已委派');
  showDelegate.value = false;
  await load();
}

onMounted(async () => {
  await load();
  try {
    const res = await listUsers();
    users.value = res.list.map((u) => ({ text: u.nickname || u.username, value: u.id }));
  } catch { /* 权限不足时忽略 */ }
});
</script>

<template>
  <div class="page" v-if="ticket">
    <van-nav-bar :title="ticket.ticketNo" left-arrow fixed @click-left="router.back()" />
    <div style="margin-top: 56px">
      <van-cell-group inset class="mt">
        <van-cell title="标题">
          <template #label>
            <div class="t-big">{{ ticket.title }}</div>
          </template>
        </van-cell>
        <van-cell title="状态">
          <template #value>
            <van-tag :type="STATUS_COLOR[ticket.status]" size="large">{{ STATUS[ticket.status] }}</van-tag>
          </template>
        </van-cell>
        <van-cell title="响应截止" :value="fmt(ticket.responseDeadline)" />
        <van-cell title="解决截止" :value="fmt(ticket.resolveDeadline)" />
        <van-cell title="首次响应" :value="fmt(ticket.firstResponseAt)" />
        <van-cell title="解决时间" :value="fmt(ticket.resolvedAt)" />
      </van-cell-group>

      <van-cell-group inset title="问题描述" class="mt">
        <van-cell>
          <template #label>
            <div class="desc-html" v-html="ticket.description || '（无描述）'"></div>
          </template>
        </van-cell>
      </van-cell-group>

      <van-cell-group inset title="操作" class="mt">
        <van-grid :column-num="4" :border="false">
          <van-grid-item
            v-if="ticket.status === 0"
            icon="checked"
            text="受理"
            @click="doAccept"
          />
          <van-grid-item
            v-if="ticket.status === 0 || ticket.status === 1"
            icon="manager-o"
            text="委派"
            @click="showDelegate = true"
          />
          <van-grid-item
            v-if="ticket.status === 1"
            icon="clock-o"
            text="提交确认"
            @click="doStatus(2, '已提交确认')"
          />
          <van-grid-item
            v-if="ticket.status === 2"
            icon="done"
            text="确认解决"
            @click="doStatus(3, '已解决')"
          />
          <van-grid-item
            v-if="ticket.status === 2"
            icon="replay"
            text="退回处理"
            @click="doStatus(1, '已退回处理')"
          />
          <van-grid-item icon="comment-o" text="备注" @click="showComment = true" />
          <van-grid-item
            v-if="ticket.status !== 0 && ticket.status !== 4"
            icon="close"
            text="关闭"
            @click="doClose"
          />
        </van-grid>
      </van-cell-group>

      <van-cell-group inset title="操作流水" class="mt mb">
        <van-steps direction="vertical" :active="actions.length">
          <van-step v-for="a in actions" :key="a.id">
            <h4>{{ ACTIONS[a.action] || a.action }}</h4>
            <p v-if="a.content" class="act-content">{{ a.content }}</p>
            <p class="act-time">{{ fmt(a.createdAt) }} · #{{ a.userId }}</p>
          </van-step>
        </van-steps>
      </van-cell-group>
    </div>

    <van-popup v-model:show="showDelegate" round style="padding: 20px">
      <div style="font-weight: 600; margin-bottom: 12px">委派处理人</div>
      <van-field
        is-link
        readonly
        :model-value="users.find((u) => u.value === delegateUser)?.text || ''"
        placeholder="选择处理人"
        @click="showDelegatePicker = true"
      />
      <van-picker
        v-model="delegateUser"
        :columns="users"
        :visible-item-count="4"
        @confirm="showDelegatePicker = false"
      />
      <div style="margin-top: 12px; text-align: right">
        <van-button size="small" @click="showDelegate = false">取消</van-button>
        <van-button size="small" type="primary" @click="doDelegate">确认委派</van-button>
      </div>
    </van-popup>

    <van-popup v-model:show="showComment" round position="bottom" style="padding: 20px">
      <div style="font-weight: 600; margin-bottom: 12px">添加备注</div>
      <van-field
        v-model="comment"
        type="textarea"
        rows="4"
        placeholder="备注内容（支持后续 PC 端查看）"
      />
      <div style="margin-top: 12px; text-align: right">
        <van-button size="small" @click="showComment = false">取消</van-button>
        <van-button size="small" type="primary" @click="doComment">提交</van-button>
      </div>
    </van-popup>
  </div>
</template>

<style scoped>
.t-big { font-size: 16px; font-weight: 600; color: #333; }
.mt { margin-top: 12px; }
.mb { margin-bottom: 60px; }
.act-content { font-size: 13px; color: #555; margin: 2px 0; }
.act-time { font-size: 12px; color: #bbb; }
</style>
