<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import {
  approveOrderGuests,
  approveCheckout,
  fetchAdminOrders,
  fetchMerchantOrders,
  fetchOrderDetail,
  rejectOrderGuests,
  rejectCheckout,
} from '@/api/order'
import { ORDER_STATUS, type Order } from '@/types/order'

const authStore = useAuthStore()
const loading = ref(false)
const detailLoading = ref(false)
const reviewing = ref(false)
const orders = ref<Order[]>([])
const total = ref(0)
const detailVisible = ref(false)
const currentOrder = ref<Order | null>(null)
const query = reactive({ status: '', keyword: '', page: 1, size: 10 })

const isMerchant = () => authStore.user?.role === 'MERCHANT'

const statusTagType: Record<string, string> = {
  PENDING_PAYMENT: 'warning',
  PAID: 'warning',
  CONFIRMED: 'success',
  CHECKOUT_PENDING: 'warning',
  REFUNDED: 'info',
  CANCELLED: 'info',
}

async function loadData() {
  loading.value = true
  try {
    const params = {
      status: query.status || undefined,
      keyword: query.keyword || undefined,
      page: query.page,
      size: query.size,
    }
    const result = authStore.user?.role === 'ADMIN'
      ? await fetchAdminOrders(params)
      : await fetchMerchantOrders(params)
    orders.value = result.list
    total.value = result.total
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function openDetail(row: Order) {
  detailLoading.value = true
  detailVisible.value = true
  try {
    currentOrder.value = await fetchOrderDetail(row.id)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载详情失败')
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

async function handleApprove() {
  if (!currentOrder.value) return
  reviewing.value = true
  try {
    currentOrder.value = await approveOrderGuests(currentOrder.value.id)
    ElMessage.success('已确认入住人信息')
    await loadData()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  } finally {
    reviewing.value = false
  }
}

async function handleReject() {
  if (!currentOrder.value) return
  try {
    const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝入住人信息', {
      confirmButtonText: '确认拒绝',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：身份证信息不匹配',
      inputValidator: (val) => !!val?.trim() || '请填写拒绝原因',
    })
    reviewing.value = true
    currentOrder.value = await rejectOrderGuests(currentOrder.value.id, value.trim())
    ElMessage.success('已拒绝并退款')
    await loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e instanceof Error ? e.message : '操作失败')
    }
  } finally {
    reviewing.value = false
  }
}

async function handleRejectCheckout() {
  if (!currentOrder.value) return
  try {
    const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝退房申请', {
      confirmButtonText: '确认拒绝',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：已超过可退时间',
      inputValidator: (val) => !!val?.trim() || '请填写拒绝原因',
    })
    reviewing.value = true
    currentOrder.value = await rejectCheckout(currentOrder.value.id, value.trim())
    ElMessage.success('已拒绝退房申请')
    await loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e instanceof Error ? e.message : '操作失败')
    }
  } finally {
    reviewing.value = false
  }
}

async function handleApproveCheckout() {
  if (!currentOrder.value) return
  try {
    await ElMessageBox.confirm(
      `确认退房并退款 ¥${currentOrder.value.refundAmount ?? '-'}？`,
      '确认退款',
      { type: 'warning' }
    )
    reviewing.value = true
    currentOrder.value = await approveCheckout(currentOrder.value.id)
    ElMessage.success('已确认退房并退款')
    await loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e instanceof Error ? e.message : '操作失败')
    }
  } finally {
    reviewing.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <el-card>
    <template #header>订单管理</template>
    <el-form :inline="true" @submit.prevent="loadData">
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部" style="width: 140px">
          <el-option v-for="(label, key) in ORDER_STATUS" :key="key" :label="label" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" placeholder="订单号/酒店/入住人" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="orders" stripe>
      <el-table-column prop="orderNo" label="订单号" min-width="180" />
      <el-table-column prop="hotelName" label="酒店" min-width="140" />
      <el-table-column prop="roomTypeName" label="房型" width="120" />
      <el-table-column label="入住人数" width="90">
        <template #default="{ row }">{{ row.guestCount || 1 }} 人</template>
      </el-table-column>
      <el-table-column prop="guestName" label="主入住人" width="100" />
      <el-table-column label="入住日期" width="180">
        <template #default="{ row }">{{ row.checkInDate }} ~ {{ row.checkOutDate }}</template>
      </el-table-column>
      <el-table-column label="金额" width="100">
        <template #default="{ row }">¥{{ row.totalAmount }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="(statusTagType[row.status] as any) || 'info'">
            {{ ORDER_STATUS[row.status] || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无订单" />
      </template>
    </el-table>
    <el-pagination
      v-model:current-page="query.page"
      :page-size="query.size"
      :total="total"
      layout="total, prev, pager, next"
      class="pager"
      @current-change="loadData"
    />
  </el-card>

  <el-drawer v-model="detailVisible" title="订单详情" size="480px">
    <div v-loading="detailLoading">
      <template v-if="currentOrder">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ ORDER_STATUS[currentOrder.status] }}</el-descriptions-item>
          <el-descriptions-item label="酒店">{{ currentOrder.hotelName }}</el-descriptions-item>
          <el-descriptions-item label="房型">{{ currentOrder.roomTypeName }}</el-descriptions-item>
          <el-descriptions-item label="入住">{{ currentOrder.checkInDate }} ~ {{ currentOrder.checkOutDate }}</el-descriptions-item>
          <el-descriptions-item label="晚数">{{ currentOrder.nights }} 晚</el-descriptions-item>
          <el-descriptions-item label="入住人数">{{ currentOrder.guestCount || 1 }} 人</el-descriptions-item>
          <el-descriptions-item label="主联系人">{{ currentOrder.guestName }} {{ currentOrder.guestPhone }}</el-descriptions-item>
          <el-descriptions-item label="总价">¥{{ currentOrder.totalAmount }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ currentOrder.createdAt }}</el-descriptions-item>
          <el-descriptions-item v-if="currentOrder.paidAt" label="支付时间">{{ currentOrder.paidAt }}</el-descriptions-item>
          <el-descriptions-item v-if="currentOrder.rejectReason" label="备注/原因">{{ currentOrder.rejectReason }}</el-descriptions-item>
          <el-descriptions-item v-if="currentOrder.checkoutApplyAt" label="退房申请时间">{{ currentOrder.checkoutApplyAt }}</el-descriptions-item>
          <el-descriptions-item v-if="currentOrder.refundAmount != null" label="预计退款">¥{{ currentOrder.refundAmount }}</el-descriptions-item>
          <el-descriptions-item v-if="currentOrder.refundPolicy" label="退款说明">{{ currentOrder.refundPolicy }}</el-descriptions-item>
        </el-descriptions>

        <div class="guest-section">
          <div class="guest-title">入住人信息</div>
          <el-table :data="currentOrder.guests || []" size="small" stripe>
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="name" label="姓名" width="100" />
            <el-table-column prop="phone" label="手机号" width="130" />
            <el-table-column prop="idCard" label="身份证" min-width="160">
              <template #default="{ row }">{{ row.idCard || '-' }}</template>
            </el-table-column>
          </el-table>
        </div>

        <div v-if="isMerchant() && currentOrder.status === 'CHECKOUT_PENDING'" class="review-actions">
          <el-alert type="warning" show-icon :closable="false" title="用户申请提前退房，请确认并退款" />
          <el-button type="primary" :loading="reviewing" @click="handleApproveCheckout">确认退房并退款</el-button>
          <el-button type="danger" plain :loading="reviewing" @click="handleRejectCheckout">拒绝申请</el-button>
        </div>
        <div v-if="isMerchant() && currentOrder.status === 'PAID'" class="review-actions">
          <el-alert type="info" show-icon :closable="false" title="用户已支付，请审核入住人信息" />
          <el-button type="primary" :loading="reviewing" @click="handleApprove">确认通过</el-button>
          <el-button type="danger" plain :loading="reviewing" @click="handleReject">拒绝并退款</el-button>
        </div>
      </template>
    </div>
  </el-drawer>
</template>

<style scoped>
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.guest-section {
  margin-top: 20px;
}

.guest-title {
  margin-bottom: 8px;
  font-weight: 600;
}

.review-actions {
  margin-top: 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
