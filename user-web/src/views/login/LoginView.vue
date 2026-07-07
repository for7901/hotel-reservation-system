<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showSuccessToast, showFailToast } from 'vant'
import { useAuthStore } from '@/stores/auth'
import { isValidPhone } from '@/utils/error'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const loading = ref(false)

const form = reactive({
  phone: '',
  password: '',
})

async function handleLogin() {
  if (!form.phone || !form.password) {
    showFailToast('请填写手机号和密码')
    return
  }
  if (!isValidPhone(form.phone)) {
    showFailToast('请输入正确的11位手机号')
    return
  }
  loading.value = true
  try {
    await authStore.login(form)
    showSuccessToast('登录成功')
    const redirect = (route.query.redirect as string) || '/'
    router.replace(redirect)
  } catch (e) {
    showFailToast(e instanceof Error ? e.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="登录" />
    <div class="form">
      <van-cell-group inset>
        <van-field v-model="form.phone" label="手机号" placeholder="请输入11位手机号" type="tel" maxlength="11" />
        <van-field v-model="form.password" label="密码" placeholder="请输入密码" type="password" />
      </van-cell-group>
      <div class="actions">
        <van-button type="primary" block :loading="loading" @click="handleLogin">登录</van-button>
        <van-button block plain hairline class="mt" @click="router.push('/register')">注册账号</van-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f7f8fa;
}

.form {
  padding-top: 24px;
}

.actions {
  margin: 24px 16px;
}

.mt {
  margin-top: 12px;
}
</style>
