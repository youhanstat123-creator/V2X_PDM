<template>
  <div class="v2x-view" :class="{ 'dark-theme': isDarkMode }">
    <div class="header">
      <div class="title-wrap">
        <h2>📡 V2X 통신 실시간 관리</h2>
        <div class="selector-group">
          <label>관리 교차로:  </label>
          <select v-model="selectedId" @change="updateData" class="intersection-select">
            <option v-for="item in intersectionList" :key="item.id" :value="item.id">
              {{ item.name }} ({{ item.id }})
            </option>
          </select>
        </div>
      </div>
    </div>

    <div class="v2x-stats-row">
      <div class="v2x-card stat-box">
        <span class="label">  평균 통신 지연 </span>
        <strong class="val blue">{{ commLog.avg_latency_ms }}<small>ms</small></strong>
        <p class="sub-desc" :class="commLog.avg_latency_ms > 50 ? 'text-danger' : ''">
          {{ commLog.avg_latency_ms > 50 ? '⚠️ 지연 시간 증가' : '✅ 통신 상태 양호' }}
        </p>
      </div>

      <div class="v2x-card stat-box">
        <span class="label">SPaT 메시지 전송 성공률   </span>
        <strong class="val green">{{ calculateSuccessRate }}<small>%</small></strong>
        <p class="sub-desc">
          실패 건수: {{ commLog.spat_fail_count }} / {{ commLog.spat_send_count }}
        </p>
      </div>

      <div class="v2x-card stat-box">
        <span class="label">접속 차량 </span>
        <strong class="val orange">{{ commLog.connected_vehicle_count }}<small>대</small></strong>
        <p class="sub-desc">장비명: {{ commLog.v2x_device_id }}</p>
      </div>
    </div>

    <div class="v2x-card chart-section">
      <h3>📈 최근 1시간 통신 지연(Latency) 변동 추이 (10분 단위)</h3>
      <div class="chart-inner-bg">
        <div class="chart-container">
          <svg viewBox="0 0 1000 160" class="v2x-svg">
            <line v-for="i in 3" :key="i" x1="0" :y1="i * 40 + 20" x2="1000" :y2="i * 40 + 20" stroke="#bfbdbd" stroke-width="1" />
            
            <path :d="chartPath" fill="none" stroke="#3498db" stroke-width="2" stroke-linejoin="round" stroke-linecap="round" />
            
            <g v-for="(p, i) in historyPoints" :key="i">
              <circle :cx="p.x" :cy="p.y" r="4" :fill="p.val > 60 ? '#e74c3c' : '#3498db'" />
              <text :x="p.x" :y="p.y - 12" text-anchor="middle" font-size="11" font-weight="600" :fill="p.val > 60 ? '#e74c3c' : (isDarkMode ? '#ffffff' : '#555')">
                {{ p.val }}
              </text>
            </g>
          </svg>
          <div class="chart-labels">
            <span v-for="label in chartLabels" :key="label" :class="{ today: label === '현재' }">
              {{ label }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <div class="v2x-card table-card">
      <div class="table-header-group">
        <h3>📋 실시간 통신 로그 </h3>
        <div class="pagination-controls">
          <button @click="prevPage" :disabled="currentPage === 1" class="page-btn">이전</button>
          <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
          <button @click="nextPage" :disabled="currentPage === totalPages" class="page-btn">
            다음
          </button>
        </div>
      </div>
      <div class="table-wrapper">
        <table class="log-table">
          <thead>
            <tr>
              <th>측정 시각</th>
              <th>SPaT 전송</th>
              <th>SPaT 실패</th>
              <th>평균 지연</th>
              <th>통신 실패</th>
              <th>접속 차량</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(log, idx) in paginatedLogs" :key="idx">
              <td>{{ log.v2x_log_time }}</td>
              <td>{{ log.spat_send_count }}건</td>
              <td :class="{ 'text-danger': log.spat_fail_count > 0 }">
                {{ log.spat_fail_count }}건
              </td>
              <td>{{ log.avg_latency_ms }}ms</td>
              <td :class="{ 'text-danger': log.comm_fail_count > 0 }">
                {{ log.comm_fail_count }}건
              </td>
              <td>{{ log.connected_vehicle_count }}대</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, inject } from 'vue'
import api from '@/api'

const isDarkMode = inject('isDarkMode', ref(false))
const currentTime = ref(new Date().toLocaleString())
let timer = null
let dataTimer = null

const intersectionList = [
  { id: 'ICN-01', name: '인천시청입구 삼거리' },
  { id: 'ICN-02', name: '예술회관역 사거리' },
  { id: 'ICN-03', name: '중앙공원 사거리' },
]

const selectedId = ref('ICN-03')
const currentPage = ref(1)
const itemsPerPage = 5

const commLog = ref({
  v2x_device_id: '',
  spat_send_count: 1,
  spat_fail_count: 0,
  avg_latency_ms: 0,
  comm_fail_count: 0,
  connected_vehicle_count: 0,
})

const historyData = ref([0, 0, 0, 0, 0, 0, 0])
const logHistory = ref([])
const chartLabels = ref([])

const totalPages = computed(() => Math.ceil(logHistory.value.length / itemsPerPage) || 1)
const paginatedLogs = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  const end = start + itemsPerPage
  return logHistory.value.slice(start, end)
})

const prevPage = () => { if (currentPage.value > 1) currentPage.value-- }
const nextPage = () => { if (currentPage.value < totalPages.value) currentPage.value++ }

const historyPoints = computed(() => {
  const width = 1000;
  const height = 160;
  const paddingX = 60;
  const paddingY = 40; 
  const usableWidth = width - (paddingX * 2);
  const dataLen = historyData.value.length || 7;

  return historyData.value.map((val, idx) => {
    const normalizedVal = Math.min(val, 120);
    const yRatio = normalizedVal / 120;
    
    return {
      x: paddingX + (usableWidth / (dataLen - 1)) * idx,
      y: (height - paddingY) - (yRatio * (height - paddingY * 2)),
      val: val
    }
  })
})

const chartPath = computed(() =>
  historyPoints.value.map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x},${p.y}`).join(' '),
)

const calculateSuccessRate = computed(() => {
  const total = commLog.value.spat_send_count || 1
  const fail = commLog.value.spat_fail_count || 0
  return (((total - fail) / total) * 100).toFixed(2)
})

const updateChartLabels = () => {
  const labels = [];
  const now = new Date();
  for (let i = 6; i >= 1; i--) {
    const time = new Date(now.getTime() - i * 10 * 60 * 1000);
    labels.push(`${time.getHours().toString().padStart(2, '0')}:${time.getMinutes().toString().padStart(2, '0')}`);
  }
  labels.push('현재');
  chartLabels.value = labels;
}

const updateData = async () => {
  updateChartLabels();
  try {
    const id = selectedId.value
    const [sumRes, histRes, logsRes] = await Promise.all([
      api.get(`/api/v2x/${id}`),
      api.get(`/api/v2x/${id}/history`),
      api.get(`/api/v2x/${id}/logs`),
    ])
    const s = sumRes.data
    commLog.value = {
      v2x_device_id: s.deviceId ?? s.v2x_device_id ?? '',
      spat_send_count: Math.max(1, s.spatSendCount ?? s.spat_send_count ?? 1),
      spat_fail_count: s.spatFailCount ?? s.spat_fail_count ?? 0,
      avg_latency_ms: s.avgLatencyMs ?? s.avg_latency_ms ?? 0,
      comm_fail_count: 0,
      connected_vehicle_count: s.connectedVehicleCount ?? s.connected_vehicle_count ?? 0,
    }
    const rows = logsRes.data ?? []
    if (rows.length) {
      commLog.value.comm_fail_count = rows[0].commFailCount ?? rows[0].comm_fail_count ?? 0
    }

    if (histRes.data) {
      const latencies = histRes.data.map((h) => h.latency ?? h.avg_latency_ms ?? 0);
      historyData.value = latencies.length >= 7 ? latencies.slice(-7) : [...Array(7 - latencies.length).fill(0), ...latencies];
    }

    logHistory.value = rows.map((log) => ({
      v2x_log_time: log.v2xLogTime ?? log.v2x_log_time,
      spat_send_count: log.spatSendCount ?? log.spat_send_count,
      spat_fail_count: log.spatFailCount ?? log.spat_fail_count,
      avg_latency_ms: log.avgLatencyMs ?? log.avg_latency_ms,
      comm_fail_count: log.commFailCount ?? log.comm_fail_count,
      connected_vehicle_count: log.connectedVehicleCount ?? log.connected_vehicle_count,
    }))
  } catch (error) {
    console.error('V2X API 오류:', error)
  }
}

onMounted(() => {
  updateData()
  timer = setInterval(() => { currentTime.value = new Date().toLocaleString() }, 1000)
  dataTimer = setInterval(updateData, 5000);
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (dataTimer) clearInterval(dataTimer)
})
</script>

<style scoped>
.v2x-view {
  display: flex;
  flex-direction: column;
  gap: 15px;
  padding: 15px;
  width: 100%;
  box-sizing: border-box;
  background: #f4f7f9;
}

.v2x-card {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
}

.chart-inner-bg {
  background: #ffffff;
  border: 1px solid #eef1f5;
  border-radius: 10px;
  padding: 30px 10px 15px;
  margin-top: 15px;
}

.chart-container {
  width: 100%;
  overflow: hidden;
}

.v2x-svg {
  width: 100%;
  height: auto;
  display: block;
  overflow: visible;
}

.intersection-select{
  padding: 6px 12px; border-radius: 6px; border: 1px solid #ddd; background: white; cursor: pointer;
}

.chart-labels {
  display: flex;
  justify-content: space-between;
  padding: 0 60px;
  font-size: 12px;
  color: #383838;
  margin-top: 15px;
}
.chart-labels .today { color: #3498db; font-weight: bold; }

.v2x-stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 15px;
}
.val { font-size: 30px; font-weight: 800; }
.val.blue { color: #3498db; }
.val.green { color: #2ecc71; }
.val.orange { color: #e67e22; }
.val small { font-size: 14px; color: #999; margin-left: 3px; }

.table-header-group {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 20px;
}

.page-btn {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  padding: 5px 15px;
  border-radius: 6px;
  cursor: pointer;
  color: #475569 !important; /* 가독성을 위한 진한 색상 */
  font-weight: 600;
}

.table-wrapper { overflow-x: auto; margin-top: 12px; }
.log-table { width: 100%; border-collapse: collapse; font-size: 14px; }
.log-table th { background: #f8fafc; padding: 14px; text-align: left; color: #64748b; border-bottom: 2px solid #edf2f7; }
.log-table td { padding: 14px; border-bottom: 1px solid #f1f5f9; color: #334155; }

.text-danger { color: #ef4444 !important; font-weight: bold; }

/* ⭐ 다크모드 강화: 글자색 및 수치 색상 유지 */
.dark-theme.v2x-view { background: #4f5052 !important; }

.dark-theme .v2x-card { 
  background: #1e1e1e; 
  color: #ffffff; /* 기본 텍스트는 모두 하얀색 */
  border: 1px solid #2a2a2a;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.4); 
}

/* 다크모드에서도 수치(숫자) 색상은 선명하게 유지 */
.dark-theme .val.blue { color: #3498db !important; }
.dark-theme .val.green { color: #2ecc71 !important; }
.dark-theme .val.orange { color: #e67e22 !important; }
.dark-theme .val small { color: #cccccc; }

.dark-theme .chart-inner-bg { 
  background: #1e1e1e; 
  border-color: #2a2a2a; 
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.3); 
}

.dark-theme .log-table th { background: #2a2a2a; color: #ffffff; border-bottom-color: #444; }
.dark-theme .log-table td { border-bottom-color: #2a2a2a; color: #ffffff; }

.dark-theme .page-btn {
  background: #000000 !important;
  color: #ffffff !important; /* 버튼 안 글씨 흰색 */
  border-color: #000000;
}

.dark-theme .page-btn:disabled { opacity: 0.3; }

.dark-theme strong, 
.dark-theme h2, 
.dark-theme h3 { 
  color: #ffffff !important; 
}

.dark-theme .label, .dark-theme .sub-desc {
  color: #ffffff; /* 라벨과 부가설명도 하얀색으로 변경 */
}

.dark-theme .selector-group label {
  color: #ffffff;
}

.dark-theme .intersection-select {
  background: #2a2a2a;
  color: white;
  border-color: #444;
}

.dark-theme .chart-labels {
  color: #ffffff;
}
</style>