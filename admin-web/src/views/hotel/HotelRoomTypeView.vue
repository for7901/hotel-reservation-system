<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createRoomType,
  deleteRoomType,
  fetchRoomTypes,
  updateRoomType,
} from '@/api/hotel'
import type { RoomType, RoomTypeSaveRequest } from '@/types/hotel'

const route = useRoute()
const router = useRouter()
const hotelId = Number(route.params.hotelId)
const loading = ref(false)
const roomTypes = ref<RoomType[]>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const form = reactive<RoomTypeSaveRequest>({
  name: '',
  bedType: '大床',
  area: 30,
  maxGuests: 2,
  basePrice: 299,
  description: '',
  breakfast: 0,
})

async function loadData() {
  loading.value = true
  try {
    roomTypes.value = await fetchRoomTypes(hotelId)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { name: '', bedType: '大床', area: 30, maxGuests: 2, basePrice: 299, description: '', breakfast: 0 })
  dialogVisible.value = true
}

function openEdit(row: RoomType) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    bedType: row.bedType,
    area: row.area,
    maxGuests: row.maxGuests,
    basePrice: row.basePrice,
    description: row.description || '',
    breakfast: row.breakfast,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  try {
    if (editingId.value) {
      await updateRoomType(hotelId, editingId.value, form)
      ElMessage.success('更新成功')
    } else {
      await createRoomType(hotelId, form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  }
}

async function handleDelete(row: RoomType) {
  await ElMessageBox.confirm(`确定删除「${row.name}」？`, '提示', { type: 'warning' })
  await deleteRoomType(hotelId, row.id)
  ElMessage.success('已删除')
  loadData()
}

onMounted(loadData)
</script>

<template>
  <el-card>
    <template #header>
      <div class="header">
        <el-button link @click="router.back()">← 返回</el-button>
        <span>房型管理</span>
        <el-button type="primary" @click="openCreate">新增房型</el-button>
      </div>
    </template>
    <el-table v-loading="loading" :data="roomTypes" stripe>
      <el-table-column prop="name" label="房型" min-width="140" />
      <el-table-column prop="bedType" label="床型" width="90" />
      <el-table-column prop="area" label="面积" width="80" />
      <el-table-column prop="maxGuests" label="人数" width="70" />
      <el-table-column prop="basePrice" label="价格" width="90" />
      <el-table-column label="早餐" width="80">
        <template #default="{ row }">{{ row.breakfast ? '含早' : '无早' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/hotels/${hotelId}/rooms/${row.id}/inventory`)">库存</el-button>
          <el-button link @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="editingId ? '编辑房型' : '新增房型'" width="480px">
    <el-form label-width="80px">
      <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
      <el-form-item label="床型"><el-input v-model="form.bedType" /></el-form-item>
      <el-form-item label="面积"><el-input-number v-model="form.area" :min="10" /></el-form-item>
      <el-form-item label="人数"><el-input-number v-model="form.maxGuests" :min="1" :max="10" /></el-form-item>
      <el-form-item label="价格"><el-input-number v-model="form.basePrice" :min="1" :precision="2" /></el-form-item>
      <el-form-item label="含早">
        <el-switch v-model="form.breakfast" :active-value="1" :inactive-value="0" />
      </el-form-item>
      <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
