<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showFailToast, showSuccessToast } from 'vant'
import { changePassword, getProfile, updateProfile } from '@/api/auth'
import type { UserProfile } from '@/types/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const profile = ref<UserProfile | null>(null)
const loading = ref(false)
const saving = ref(false)
const showEdit = ref(false)
const showPassword = ref(false)

const editForm = reactive({ nickname: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '' })

onMounted(loadProfile)

async function loadProfile() {
  loading.value = true
  try {
    profile.value = await getProfile()
    editForm.nickname = profile.value.nickname || ''
  } catch (e) {
    showFailToast(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function handleSaveProfile() {
  saving.value = true
  try {
    profile.value = await updateProfile({ nickname: editForm.nickname })
    showEdit.value = false
    showSuccessToast('保存成功')
  } catch (e) {
    showFailToast(e instanceof Error ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleChangePassword() {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    showFailToast('请填写完整')
    return
  }
  saving.value = true
  try {
    await changePassword(pwdForm)
    showPassword.value = false
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    showSuccessToast('密码已修改')
  } catch (e) {
    showFailToast(e instanceof Error ? e.message : '修改失败')
  } finally {
    saving.value = false
  }
}

async function handleLogout() {
  try {
    await showConfirmDialog({ title: '确定退出登录？' })
    authStore.logout()
    showSuccessToast('已退出')
    router.replace('/login')
  } catch {
    // cancelled
  }
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="我的" />
    <van-skeleton v-if="loading" title :row="3" style="padding: 16px" />
    <template v-else-if="profile">
      <van-cell-group inset>
        <van-cell title="昵称" :value="profile.nickname" is-link @click="showEdit = true" />
        <van-cell title="手机号" :value="profile.phone" />
      </van-cell-group>
      <van-cell-group inset class="menu">
        <van-cell title="我的订单" is-link to="/orders" />
        <van-cell title="我的收藏" is-link to="/favorites" />
        <van-cell title="优惠券" is-link to="/coupons" />
      </van-cell-group>
      <van-cell-group inset class="menu">
        <van-cell title="修改密码" is-link @click="showPassword = true" />
      </van-cell-group>
      <div class="actions">
        <van-button type="danger" block plain @click="handleLogout">退出登录</van-button>
      </div>
    </template>

    <van-dialog v-model:show="showEdit" title="编辑昵称" show-cancel-button @confirm="handleSaveProfile">
      <van-field v-model="editForm.nickname" placeholder="请输入昵称" style="padding: 16px" />
    </van-dialog>

    <van-dialog v-model:show="showPassword" title="修改密码" show-cancel-button @confirm="handleChangePassword">
      <van-field v-model="pwdForm.oldPassword" label="原密码" type="password" />
      <van-field v-model="pwdForm.newPassword" label="新密码" type="password" />
    </van-dialog>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 60px;
}

.menu {
  margin-top: 12px;
}

.actions {
  margin: 24px 16px;
}
</style>
