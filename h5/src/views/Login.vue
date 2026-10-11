<script setup>
import { showFailToast, showToast } from 'vant';
import { ref } from 'vue';
import { useRouter } from 'vue-router';

import { getUserInfo, login } from '../api';

const router = useRouter();
const form = ref({ username: '', password: '' });
const loading = ref(false);

async function submit() {
  if (!form.value.username || !form.value.password) {
    showToast('请输入账号和密码');
    return;
  }
  loading.value = true;
  try {
    const d = await login(form.value);
    localStorage.setItem('h5_token', d.accessToken);
    try {
      const info = await getUserInfo();
      localStorage.setItem('h5_user', JSON.stringify(info));
    } catch { /* 用户信息获取失败不阻塞登录 */ }
    showToast('登录成功');
    router.replace('/');
  } catch (e) {
    showFailToast(e.message || '登录失败');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="login-page">
    <div class="brand">
      <div class="logo">格物</div>
      <div class="title">materin-ops 格物</div>
      <div class="subtitle">移动运维工作台</div>
    </div>
    <van-form @submit="submit">
      <van-cell-group inset>
        <van-field v-model="form.username" label="账号" placeholder="请输入账号" clearable />
        <van-field
          v-model="form.password"
          label="密码"
          type="password"
          placeholder="请输入密码"
        />
      </van-cell-group>
      <div class="p-4">
        <van-button round block type="primary" native-type="submit" :loading="loading">
          登 录
        </van-button>
      </div>
    </van-form>
    <div class="copyright">Copyright © 2026 materin-ops 格物</div>
  </div>
</template>

<style scoped>
.login-page { min-height: 100vh; display: flex; flex-direction: column; justify-content: center; background: linear-gradient(180deg, #e8f1ff 0%, #f7f8fa 40%); }
.brand { text-align: center; margin-bottom: 32px; }
.logo { width: 56px; height: 56px; margin: 0 auto 12px; border-radius: 14px; background: #1677ff; color: #fff; font-size: 22px; font-weight: 600; display: flex; align-items: center; justify-content: center; }
.title { font-size: 20px; font-weight: 600; }
.subtitle { color: #888; font-size: 13px; margin-top: 4px; }
.copyright { text-align: center; color: #bbb; font-size: 12px; margin-top: 40px; }
</style>
