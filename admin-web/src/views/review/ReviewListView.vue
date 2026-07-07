<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchReviews, updateReviewVisibility } from '@/api/review'
import type { Review } from '@/api/review'

const loading = ref(false)
const reviews = ref<Review[]>([])
const total = ref(0)
const query = reactive({ keyword: '', page: 1, size: 10 })

async function loadData() {
  loading.value = true
  try {
    const result = await fetchReviews(query)
    reviews.value = result.list
    total.value = result.total
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function toggleVisibility(row: Review) {
  const next = row.status === 1 ? 0 : 1
  try {
    await updateReviewVisibility(row.id, next)
    ElMessage.success('操作成功')
    loadData()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

onMounted(loadData)
</script>

<template>
  <el-card>
    <template #header>评价管理</template>
    <el-form :inline="true" @submit.prevent="loadData">
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" placeholder="内容/昵称" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="reviews" stripe>
      <el-table-column prop="userNickname" label="用户" width="100" />
      <el-table-column prop="rating" label="评分" width="70" />
      <el-table-column prop="content" label="内容" min-width="180" show-overflow-tooltip />
      <el-table-column label="商家回复" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ row.merchantReply || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '展示' : '隐藏' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="时间" width="170" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button link @click="toggleVisibility(row)">{{ row.status === 1 ? '隐藏' : '展示' }}</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无评价" /></template>
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
</template>

<style scoped>
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
