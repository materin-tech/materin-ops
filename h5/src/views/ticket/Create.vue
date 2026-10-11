<script setup>
import { showSuccessToast, showToast } from 'vant';
import { ref } from 'vue';
import { useRouter } from 'vue-router';

import { createTicket } from '../../api';

const router = useRouter();
const form = ref({ title: '', description: '', type: 0, priority: 2 });
const submitting = ref(false);

async function submit() {
  if (!form.value.title.trim()) {
    showToast('请填写标题');
    return;
  }
  submitting.value = true;
  try {
    await createTicket(form.value);
    showSuccessToast('工单已提交');
    router.replace('/ticket');
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="提交工单" left-arrow fixed @click-left="router.back()" />
    <van-notice-bar left-icon="info-o" text="提交后系统按优先级自动匹配 SLA：P1 响应 30 分钟 / 解决 4 小时" />
    <van-form style="margin-top: 56px" @submit="submit">
      <van-cell-group inset>
        <van-field
          v-model="form.title"
          label="标题"
          placeholder="一句话描述问题"
          required
          maxlength="200"
        />
        <van-field label="类型">
          <template #input>
            <van-radio-group v-model="form.type" direction="horizontal">
              <van-radio :name="0">故障</van-radio>
              <van-radio :name="1">咨询</van-radio>
              <van-radio :name="2">变更</van-radio>
              <van-radio :name="3">其他</van-radio>
            </van-radio-group>
          </template>
        </van-field>
        <van-field label="优先级">
          <template #input>
            <van-rate v-model="form.priority" :count="4" />
            <span class="pl">{{ ['P1 紧急', 'P2 高', 'P3 中', 'P4 低'][form.priority] }}</span>
          </template>
        </van-field>
        <van-field
          v-model="form.description"
          label="描述"
          type="textarea"
          rows="5"
          autosize
          placeholder="详细描述问题现象、影响范围（支持后续在 PC 端富文本补充）"
        />
      </van-cell-group>
      <div class="p-4">
        <van-button round block type="primary" native-type="submit" :loading="submitting">
          提交工单
        </van-button>
      </div>
    </van-form>
  </div>
</template>

<style scoped>
.pl { margin-left: 8px; color: #666; }
</style>
