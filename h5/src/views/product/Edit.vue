<script setup>
import { showSuccessToast, showToast } from 'vant';
import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { createProduct, productDetail, updateProduct } from '../../api';

const route = useRoute();
const router = useRouter();
const editing = !!route.params.id;
const form = ref({ name: '', code: '', description: '', status: 1 });
const submitting = ref(false);

if (editing) {
  productDetail(route.params.id).then((d) => {
    form.value = { name: d.name, code: d.code, description: d.description ?? '', status: d.status };
  });
}

async function submit() {
  if (!form.value.name.trim() || (!editing && !form.value.code.trim())) {
    showToast('名称与编码必填');
    return;
  }
  submitting.value = true;
  try {
    if (editing) {
      await updateProduct(route.params.id, form.value);
      showSuccessToast('已更新');
    } else {
      await createProduct(form.value);
      showSuccessToast('已创建');
    }
    router.replace('/product');
  } catch (e) {
    showToast(e.message || '保存失败');
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <div class="page">
    <van-nav-bar :title="editing ? '编辑产品' : '新增产品'" left-arrow fixed @click-left="router.back()" />
    <van-form style="margin-top: 56px" @submit="submit">
      <van-cell-group inset>
        <van-field v-model="form.name" label="名称" placeholder="产品名称" required maxlength="128" />
        <van-field
          v-model="form.code"
          label="编码"
          placeholder="唯一编码"
          required
          maxlength="64"
          :disabled="editing"
        />
        <van-field label="状态">
          <template #input>
            <van-switch v-model="form.status" :active-value="1" :inactive-value="0" size="22px" />
          </template>
        </van-field>
        <van-field
          v-model="form.description"
          label="描述"
          type="textarea"
          rows="3"
          autosize
          placeholder="产品描述（可选）"
        />
      </van-cell-group>
      <div class="p-4">
        <van-button round block type="primary" native-type="submit" :loading="submitting">
          保 存
        </van-button>
      </div>
    </van-form>
  </div>
</template>
