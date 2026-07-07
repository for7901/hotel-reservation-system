<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { fetchCities, fetchProvinces } from '@/api/hotel'
import type { City, Province } from '@/types/hotel'

type Option = { text: string; value: number }

const props = defineProps<{
  show: boolean
  provinceId?: number
  cityId?: number
}>()

const emit = defineEmits<{
  'update:show': [value: boolean]
  confirm: [value: { provinceId?: number; cityId?: number }]
}>()

const OPTION_HEIGHT = 44
const provinces = ref<Province[]>([])
const cities = ref<City[]>([])
const loading = ref(false)
const provinceColRef = ref<HTMLElement>()
const cityColRef = ref<HTMLElement>()
const selectedProvinceValue = ref(0)
const selectedCityValue = ref(0)
let provinceScrollTimer: number | undefined
let cityScrollTimer: number | undefined

const provinceOptions = computed<Option[]>(() => [
  { text: '全部省份', value: 0 },
  ...provinces.value.map((p) => ({ text: p.name, value: p.id })),
])

const cityOptions = computed<Option[]>(() => [
  { text: '全部城市', value: 0 },
  ...cities.value.map((c) => ({ text: c.name, value: c.id })),
])

function scrollToValue(col: HTMLElement | undefined, options: Option[], value: number) {
  if (!col) return
  const index = Math.max(0, options.findIndex((item) => item.value === value))
  col.scrollTop = index * OPTION_HEIGHT
}

function readValueFromScroll(col: HTMLElement | undefined, options: Option[]) {
  if (!col || options.length === 0) return options[0]?.value ?? 0
  const index = Math.min(options.length - 1, Math.max(0, Math.round(col.scrollTop / OPTION_HEIGHT)))
  return options[index]?.value ?? 0
}

function snapColumn(col: HTMLElement | undefined, options: Option[]) {
  if (!col) return options[0]?.value ?? 0
  const value = readValueFromScroll(col, options)
  const index = Math.max(0, options.findIndex((item) => item.value === value))
  col.scrollTo({ top: index * OPTION_HEIGHT, behavior: 'smooth' })
  return value
}

async function loadCitiesForProvince(provinceValue: number, cityValue = 0) {
  if (provinceValue === 0) {
    cities.value = []
    selectedCityValue.value = 0
    await nextTick()
    scrollToValue(cityColRef.value, cityOptions.value, 0)
    return
  }
  loading.value = true
  try {
    cities.value = await fetchCities(provinceValue)
    selectedCityValue.value = cityValue
    await nextTick()
    scrollToValue(cityColRef.value, cityOptions.value, cityValue)
  } finally {
    loading.value = false
  }
}

function onProvinceScroll() {
  selectedProvinceValue.value = readValueFromScroll(provinceColRef.value, provinceOptions.value)
  window.clearTimeout(provinceScrollTimer)
  provinceScrollTimer = window.setTimeout(() => {
    void onProvinceScrollEnd()
  }, 120)
}

function onCityScroll() {
  selectedCityValue.value = readValueFromScroll(cityColRef.value, cityOptions.value)
  window.clearTimeout(cityScrollTimer)
  cityScrollTimer = window.setTimeout(() => {
    onCityScrollEnd()
  }, 120)
}

async function onProvinceScrollEnd() {
  const value = snapColumn(provinceColRef.value, provinceOptions.value)
  selectedProvinceValue.value = value
  await loadCitiesForProvince(value, 0)
}

function onCityScrollEnd() {
  const value = snapColumn(cityColRef.value, cityOptions.value)
  selectedCityValue.value = value
}

function selectProvince(option: Option) {
  selectedProvinceValue.value = option.value
  scrollToValue(provinceColRef.value, provinceOptions.value, option.value)
  void loadCitiesForProvince(option.value, 0)
}

function selectCity(option: Option) {
  selectedCityValue.value = option.value
  scrollToValue(cityColRef.value, cityOptions.value, option.value)
}

function onConfirm() {
  const provinceValue = readValueFromScroll(provinceColRef.value, provinceOptions.value)
  const cityValue = readValueFromScroll(cityColRef.value, cityOptions.value)
  if (provinceValue === 0) {
    emit('confirm', {})
  } else {
    emit('confirm', {
      provinceId: provinceValue,
      cityId: cityValue === 0 ? undefined : cityValue,
    })
  }
  emit('update:show', false)
}

async function initPicker() {
  loading.value = true
  try {
    if (!provinces.value.length) {
      provinces.value = await fetchProvinces()
    }
    const provinceValue = props.provinceId ?? 0
    selectedProvinceValue.value = provinceValue
    if (provinceValue) {
      cities.value = await fetchCities(provinceValue)
    } else {
      cities.value = []
    }
    selectedCityValue.value = props.cityId ?? 0
    await nextTick()
    scrollToValue(provinceColRef.value, provinceOptions.value, provinceValue)
    scrollToValue(cityColRef.value, cityOptions.value, props.cityId ?? 0)
  } finally {
    loading.value = false
  }
}

watch(
  () => props.show,
  (visible) => {
    if (visible) void initPicker()
  },
)
</script>

<template>
  <van-popup :show="show" position="bottom" round @update:show="emit('update:show', $event)">
    <div class="region-picker">
      <div class="toolbar">
        <button type="button" class="btn cancel" @click="emit('update:show', false)">取消</button>
        <span class="title">选择地区</span>
        <button type="button" class="btn confirm" @click="onConfirm">确认</button>
      </div>
      <div v-if="loading" class="loading">加载中...</div>
      <div class="columns-wrap">
        <div class="highlight" />
        <div ref="provinceColRef" class="column" @scroll="onProvinceScroll">
          <div class="padding" />
          <div
            v-for="item in provinceOptions"
            :key="`p-${item.value}`"
            class="option"
            :class="{ active: item.value === selectedProvinceValue }"
            @click="selectProvince(item)"
          >
            {{ item.text }}
          </div>
          <div class="padding" />
        </div>
        <div ref="cityColRef" class="column" @scroll="onCityScroll">
          <div class="padding" />
          <div
            v-for="item in cityOptions"
            :key="`c-${item.value}`"
            class="option"
            :class="{ active: item.value === selectedCityValue }"
            @click="selectCity(item)"
          >
            {{ item.text }}
          </div>
          <div class="padding" />
        </div>
      </div>
    </div>
  </van-popup>
</template>

<style scoped>
.region-picker {
  background: #fff;
  user-select: none;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 44px;
  padding: 0 16px;
  border-bottom: 1px solid #ebedf0;
}

.title {
  font-size: 16px;
  font-weight: 600;
}

.btn {
  border: none;
  background: transparent;
  font-size: 14px;
  padding: 0;
  cursor: pointer;
}

.btn.confirm {
  color: #1989fa;
  font-weight: 600;
}

.btn.cancel {
  color: #969799;
}

.loading {
  text-align: center;
  color: #969799;
  font-size: 13px;
  padding: 8px 0;
}

.columns-wrap {
  position: relative;
  display: flex;
  height: 220px;
  overflow: hidden;
}

.highlight {
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  height: 44px;
  transform: translateY(-50%);
  border-top: 1px solid #ebedf0;
  border-bottom: 1px solid #ebedf0;
  pointer-events: none;
  z-index: 1;
}

.column {
  flex: 1;
  overflow-y: auto;
  scroll-snap-type: y mandatory;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.column::-webkit-scrollbar {
  display: none;
}

.padding {
  height: 88px;
  flex-shrink: 0;
}

.option {
  height: 44px;
  line-height: 44px;
  text-align: center;
  scroll-snap-align: center;
  color: #646566;
  font-size: 15px;
  cursor: pointer;
}

.option.active {
  color: #323233;
  font-weight: 600;
}
</style>
