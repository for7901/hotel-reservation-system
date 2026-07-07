<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createCoupon, fetchCoupons } from '@/api/coupon'
import type { Coupon, CouponSaveRequest } from '@/api/coupon'

const loading = ref(false)
const coupons = ref<Coupon[]>([])
const dialogVisible = ref(false)
const form = reactive<CouponSaveRequest>({ name: '', amount: 50, minAmount: 200, totalCount: 100, status: 1 })

async function loadData() {
  loading.value = true
  try {
    coupons.value = await fetchCoupons()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  try {
    await createCoupon(form)
    ElMessage.success('创建成功')
    dialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '创建失败')
  }
}

onMounted(loadData)
</script>

<template>
  <el-card>
    <template #header>
      <div class="header">
        <span>优惠券管理</span>
        <el-button type="primary" @click="dialogVisible = true">新建优惠券</el-button>
      </div>
    </template>
    <el-table v-loading="loading" :data="coupons" stripe>
      <el-table-column prop="name" label="名称" />
      <el-table-column label="面额" width="90"><template #default="{ row }">¥{{ row.amount }}</template></el-table-column>
      <el-table-column label="门槛" width="90"><template #default="{ row }">¥{{ row.minAmount }}</template></el-table-column>
      <el-table-column label="领取" width="100"><template #default="{ row }">{{ row.claimedCount }}/{{ row.totalCount }}</template></el-table-column>
      <el-table-column label="状态" width="80"><template #default="{ row }">{{ row.status === 1 ? '启用' : '禁用' }}</template></el-table-column>
      <template #empty><el-empty description="暂无优惠券" /></template>
    </el-table>
  </el-card>

  <el-dialog v-model="dialogVisible" title="新建优惠券" width="480px">
    <el-form label-width="90px">
      <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
      <el-form-item label="优惠金额"><el-input-number v-model="form.amount" :min="1" /></el-form-item>
      <el-form-item label="使用门槛"><el-input-number v-model="form.minAmount" :min="0" /></el-form-item>
      <el-form-item label="发放总量"><el-input-number v-model="form.totalCount" :min="1" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">创建</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.header { display: flex; justify-content: space-between; align-items: center; }
</style>
