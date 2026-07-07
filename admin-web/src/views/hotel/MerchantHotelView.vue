<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createHotel,
  deleteHotel,
  fetchCities,
  fetchMerchantHotels,
  fetchProvinces,
  updateHotel,
} from '@/api/hotel'
import type { AdminHotel, City, HotelSaveRequest, Province } from '@/types/hotel'

const router = useRouter()
const loading = ref(false)
const hotels = ref<AdminHotel[]>([])
const total = ref(0)
const cities = ref<City[]>([])
const provinces = ref<Province[]>([])
const provinceId = ref<number>()
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const page = ref(1)

const form = reactive<HotelSaveRequest>({
  cityId: 0,
  name: '',
  address: '',
  starRating: 3,
  coverImage: '',
  description: '',
  facilities: [],
})

const statusMap: Record<string, string> = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回',
}

const formRef = ref()
const submitting = ref(false)
const rules = {
  cityId: [{ required: true, message: '请选择城市', trigger: 'change' }],
  name: [{ required: true, message: '请输入酒店名称', trigger: 'blur' }],
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }],
}

const facilitiesInput = ref('')

async function loadData() {
  loading.value = true
  try {
    const result = await fetchMerchantHotels({ page: page.value, size: 10 })
    hotels.value = result.list
    total.value = result.total
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function loadCitiesForProvince(pid: number) {
  cities.value = await fetchCities(pid)
  provinceId.value = pid
}

async function onProvinceChange(pid: number) {
  await loadCitiesForProvince(pid)
  form.cityId = cities.value[0]?.id || 0
}

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    cityId: cities.value[0]?.id || 0,
    name: '',
    address: '',
    starRating: 3,
    coverImage: '',
    description: '',
    facilities: [],
  })
  facilitiesInput.value = ''
  dialogVisible.value = true
}

async function openEdit(row: AdminHotel) {
  editingId.value = row.id
  if (row.provinceId) {
    await loadCitiesForProvince(row.provinceId)
  }
  Object.assign(form, {
    cityId: row.cityId,
    name: row.name,
    address: row.address,
    starRating: row.starRating,
    coverImage: row.coverImage || '',
    description: row.description || '',
    facilities: row.facilities || [],
  })
  facilitiesInput.value = (row.facilities || []).join(', ')
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  form.facilities = facilitiesInput.value
    .split(/[,，]/)
    .map((s) => s.trim())
    .filter(Boolean)
  submitting.value = true
  try {
    if (editingId.value) {
      await updateHotel(editingId.value, form)
      ElMessage.success('更新成功')
    } else {
      await createHotel(form)
      ElMessage.success('创建成功，等待审核')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: AdminHotel) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.name}」？`, '提示', { type: 'warning' })
    await deleteHotel(row.id)
    ElMessage.success('已删除')
    loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e instanceof Error ? e.message : '删除失败')
    }
  }
}

onMounted(async () => {
  try {
    provinces.value = await fetchProvinces()
    if (provinces.value.length) {
      await loadCitiesForProvince(provinces.value[0].id)
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载地区失败')
  }
  await loadData()
})
</script>

<template>
  <el-card>
    <template #header>
      <div class="header">
        <span>我的酒店</span>
        <el-button type="primary" @click="openCreate">新增酒店</el-button>
      </div>
    </template>
    <el-table v-loading="loading" :data="hotels" stripe>
      <el-table-column prop="name" label="酒店名称" min-width="160" />
      <el-table-column prop="cityName" label="城市" width="100" />
      <el-table-column prop="starRating" label="星级" width="70" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag>{{ statusMap[row.status] || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最低价" width="90">
        <template #default="{ row }">¥{{ row.minPrice }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/hotels/${row.id}/rooms`)">房型</el-button>
          <el-button link @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无酒店，点击新增创建" />
      </template>
    </el-table>
    <el-pagination
      v-model:current-page="page"
      :total="total"
      layout="total, prev, pager, next"
      class="pager"
      @current-change="loadData"
    />
  </el-card>

  <el-dialog v-model="dialogVisible" :title="editingId ? '编辑酒店' : '新增酒店'" width="520px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="省份">
        <el-select v-model="provinceId" style="width: 100%" @change="onProvinceChange">
          <el-option v-for="p in provinces" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="城市" prop="cityId">
        <el-select v-model="form.cityId" style="width: 100%">
          <el-option v-for="c in cities" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="名称" prop="name">
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item label="地址" prop="address">
        <el-input v-model="form.address" />
      </el-form-item>
      <el-form-item label="星级">
        <el-input-number v-model="form.starRating" :min="1" :max="5" />
      </el-form-item>
      <el-form-item label="封面图">
        <el-input v-model="form.coverImage" placeholder="图片URL" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="3" />
      </el-form-item>
      <el-form-item label="设施">
        <el-input v-model="facilitiesInput" placeholder="逗号分隔，如：WiFi,停车场" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
