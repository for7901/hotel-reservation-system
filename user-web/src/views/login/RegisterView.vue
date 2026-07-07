<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast, showFailToast } from 'vant'
import { useAuthStore } from '@/stores/auth'
import { isValidPhone } from '@/utils/error'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)

const form = reactive({
  phone: '',
  password: '',
  nickname: '',
})

async function handleRegister() {
  if (!form.phone || !form.password) {
    showFailToast('请填写手机号和密码')
    return
  }
  if (!isValidPhone(form.phone)) {
    showFailToast('请输入正确的11位手机号')
    return
  }
  if (form.password.length < 6 || form.password.length > 20) {
    showFailToast('密码长度需在6-20位之间')
    return
  }
  loading.value = true
  try {
    await authStore.register(form)
    showSuccessToast('注册成功')
    const redirect = (route.query.redirect as string) || '/'
    router.replace(redirect)
  } catch (e) {
    showFailToast(e instanceof Error ? e.message : '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="注册" left-arrow @click-left="router.back()" />
    <div class="form">
      <van-cell-group inset>
        <van-field v-model="form.phone" label="手机号" placeholder="请输入11位手机号" type="tel" maxlength="11" />
        <van-field v-model="form.password" label="密码" placeholder="6-20位密码" type="password" />
        <van-field v-model="form.nickname" label="昵称" placeholder="选填" />
      </van-cell-group>
      <div class="actions">
        <van-button type="primary" block :loading="loading" @click="handleRegister">注册</van-button>
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
</style>
