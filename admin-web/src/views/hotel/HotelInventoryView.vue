<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchInventory, saveInventory } from '@/api/order'
import type { InventoryItem } from '@/types/order'

const route = useRoute()
const hotelId = Number(route.params.hotelId)
const roomTypeId = Number(route.params.roomTypeId)
const loading = ref(false)
const saving = ref(false)
const items = ref<InventoryItem[]>([])

const startDate = ref(new Date().toISOString().slice(0, 10))

function addDays(dateStr: string, days: number) {
  const d = new Date(dateStr)
  d.setDate(d.getDate() + days)
  return d.toISOString().slice(0, 10)
}

async function loadData() {
  loading.value = true
  try {
    items.value = await fetchInventory(hotelId, roomTypeId, startDate.value, addDays(startDate.value, 14))
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (items.value.length === 0) {
    ElMessage.warning('暂无库存数据可保存')
    return
  }
  saving.value = true
  try {
    await saveInventory(hotelId, roomTypeId, items.value)
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <el-card>
    <template #header>
      <div class="header">
        <span>库存日历（未来14天）</span>
        <div class="actions">
          <el-date-picker v-model="startDate" type="date" value-format="YYYY-MM-DD" @change="loadData" />
          <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
        </div>
      </div>
    </template>
    <el-table v-loading="loading" :data="items" stripe>
      <el-table-column prop="invDate" label="日期" width="140" />
      <el-table-column label="价格" width="160">
        <template #default="{ row }">
          <el-input-number v-model="row.price" :min="1" :precision="2" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="可售间数" width="160">
        <template #default="{ row }">
          <el-input-number v-model="row.availableRooms" :min="0" size="small" />
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无库存，请保存以初始化" />
      </template>
    </el-table>
  </el-card>
</template>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.actions {
  display: flex;
  gap: 12px;
  align-items: center;
}
</style>
