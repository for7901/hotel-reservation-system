<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createBanner, deleteBanner, fetchBanners, updateBanner } from '@/api/banner'
import type { Banner, BannerSaveRequest } from '@/api/banner'

const loading = ref(false)
const banners = ref<Banner[]>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive<BannerSaveRequest>({ title: '', imageUrl: '', linkUrl: '', sortOrder: 0, status: 1 })

async function loadData() {
  loading.value = true
  try {
    banners.value = await fetchBanners()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { title: '', imageUrl: '', linkUrl: '', sortOrder: 0, status: 1 })
  dialogVisible.value = true
}

function openEdit(row: Banner) {
  editingId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

async function handleSave() {
  try {
    if (editingId.value) {
      await updateBanner(editingId.value, form)
    } else {
      await createBanner(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  }
}

async function handleDelete(row: Banner) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.title}」？`)
    await deleteBanner(row.id)
    ElMessage.success('已删除')
    loadData()
  } catch {
    // cancelled
  }
}

onMounted(loadData)
</script>

<template>
  <el-card>
    <template #header>
      <div class="header">
        <span>Banner 管理</span>
        <el-button type="primary" @click="openCreate">新增</el-button>
      </div>
    </template>
    <el-table v-loading="loading" :data="banners" stripe>
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="imageUrl" label="图片" min-width="180" show-overflow-tooltip />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">{{ row.status === 1 ? '启用' : '禁用' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button link @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无 Banner" /></template>
    </el-table>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="editingId ? '编辑 Banner' : '新增 Banner'" width="520px">
    <el-form label-width="80px">
      <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
      <el-form-item label="图片URL"><el-input v-model="form.imageUrl" /></el-form-item>
      <el-form-item label="链接"><el-input v-model="form.linkUrl" /></el-form-item>
      <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
      <el-form-item label="状态">
        <el-select v-model="form.status" style="width: 100%">
          <el-option label="启用" :value="1" /><el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.header { display: flex; justify-content: space-between; align-items: center; }
</style>
