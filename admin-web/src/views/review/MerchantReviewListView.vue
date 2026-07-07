<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchMerchantReviews, replyReview } from '@/api/review'
import type { Review } from '@/api/review'

const loading = ref(false)
const replying = ref(false)
const reviews = ref<Review[]>([])
const total = ref(0)
const query = reactive({ keyword: '', page: 1, size: 10 })
const replyVisible = ref(false)
const replyContent = ref('')
const currentReview = ref<Review | null>(null)

async function loadData() {
  loading.value = true
  try {
    const result = await fetchMerchantReviews(query)
    reviews.value = result.list
    total.value = result.total
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function openReply(row: Review) {
  currentReview.value = row
  replyContent.value = row.merchantReply || ''
  replyVisible.value = true
}

async function handleReply() {
  if (!currentReview.value) return
  const content = replyContent.value.trim()
  if (!content) {
    ElMessage.warning('请输入回复内容')
    return
  }
  replying.value = true
  try {
    await replyReview(currentReview.value.id, content)
    ElMessage.success('回复成功')
    replyVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '回复失败')
  } finally {
    replying.value = false
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
      <el-table-column prop="hotelName" label="酒店" min-width="120" />
      <el-table-column prop="userNickname" label="用户" width="100" />
      <el-table-column prop="rating" label="评分" width="70" />
      <el-table-column prop="content" label="评价内容" min-width="180" show-overflow-tooltip />
      <el-table-column label="商家回复" min-width="180">
        <template #default="{ row }">
          <span v-if="row.merchantReply">{{ row.merchantReply }}</span>
          <span v-else class="muted">未回复</span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="评价时间" width="170" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openReply(row)">{{ row.merchantReply ? '修改回复' : '回复' }}</el-button>
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

  <el-dialog v-model="replyVisible" title="回复评价" width="520px">
    <div v-if="currentReview" class="review-preview">
      <div class="preview-user">{{ currentReview.userNickname }} · {{ currentReview.rating }} 星</div>
      <div class="preview-content">{{ currentReview.content || '暂无文字评价' }}</div>
    </div>
    <el-input
      v-model="replyContent"
      type="textarea"
      :rows="4"
      maxlength="500"
      show-word-limit
      placeholder="请输入回复内容"
    />
    <template #footer>
      <el-button @click="replyVisible = false">取消</el-button>
      <el-button type="primary" :loading="replying" @click="handleReply">提交回复</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.pager { margin-top: 16px; justify-content: flex-end; }
.muted { color: #909399; }
.review-preview {
  margin-bottom: 12px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
}
.preview-user { font-weight: 600; margin-bottom: 6px; }
.preview-content { color: #606266; font-size: 14px; }
</style>
