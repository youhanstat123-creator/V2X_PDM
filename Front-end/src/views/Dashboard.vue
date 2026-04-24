<template>
  <div class="dashboard-content">
    <p v-if="loadError" class="load-error">{{ loadError }}</p>

    <div class="kpi-row">
      <div class="kpi-card">
        <span>📊 전체 기기</span>
        <strong>{{ fmtNum(summary.totalDevices) }}</strong>
      </div>
      <div class="kpi-card">
        <span>✔️ 정상 작동</span>
        <strong>{{ fmtNum(summary.normalDevices) }}</strong>
      </div>
      <div class="kpi-card error">
        <span>⚠️ 이상 탐지</span>
        <strong>{{ fmtNum(summary.abnormalDevices) }}</strong>
      </div>
      <div class="kpi-card">
        <span>📈 이상 비율</span>
        <strong>{{ fmtPct(summary.abnormalRate) }}</strong>
      </div>
      <div class="kpi-card">
        <span>🔴 평균 위험 점수</span>
        <strong>{{ fmtAvgScore(summary.averageRiskScore) }}/100</strong>
      </div>
    </div>

    <div class="middle-row">
      <div class="map-container">
        <h3>🗺️ 교차로 실시간 모니터링</h3>
        <p v-if="!intersectionDevices.length" class="empty-hint">등록된 교차로가 없습니다.</p>
        <GoogleMap
          api-key="AIzaSyAsUi8_UJRCDNIJXPJNy4Al-xOs1IzPinc"
          style="width: 100%; height: 350px; border-radius: 10px"
          :center="mapCenter"
          :zoom="intersectionDevices.length ? 13 : 11"
          :styles="isDarkMode ? mapDarkStyle : []"
        >
          <Marker
            v-for="device in intersectionDevices"
            :key="device.id"
            :options="{
              position: device.position,
              title: device.name,
              label:
                device.status === 'error'
                  ? { text: '!', color: 'white', fontWeight: 'bold' }
                  : null,
            }"
          />
        </GoogleMap>
      </div>

      <div class="chart-container">
        <div class="analysis-header">
          <h3>📊 실시간 데이터 분석 (SPAT/Latency)</h3>
          <label class="device-select-wrap">
            <span>기기 선택</span>
            <select v-model="selectedIntersectionId" @change="loadSpatLatency">
              <option v-for="opt in analysisDeviceOptions" :key="opt.id" :value="opt.id">
                {{ opt.label }}
              </option>
            </select>
          </label>
        </div>
        <div class="analysis-grid">
          <div class="analysis-section">
            <span class="sub-title">SPAT 신호 송수신 현황</span>
            <div class="spat-donut-wrapper">
              <div
                class="donut-chart"
                :style="{
                  background: donutGradient(spat.spatSuccessRate),
                }"
              >
                <div class="inner-circle">
                  <span class="percent">{{ spat.spatSuccessRate?.toFixed?.(1) ?? 0 }}%</span>
                  <span class="label">성공률</span>
                </div>
              </div>
              <div class="spat-details">
                <p>
                  🚦 현재 신호: <strong>{{ spat.currentSignal ?? '-' }}</strong>
                </p>
                <p>
                  ⏱️ 잔여 시간: <strong>{{ spat.remainTimeSeconds ?? 0 }}초</strong>
                </p>
                <p>
                  📦 패킷 상태: <strong>{{ spat.packetStatus ?? '-' }}</strong>
                </p>
              </div>
            </div>
          </div>

          <div class="analysis-section">
            <span class="sub-title">시간 단위 응답 지연 (Latency Trend)</span>
            <div class="latency-chart-area">
              <div class="line-chart-container">
                <svg
                  viewBox="-12 -22 424 118"
                  preserveAspectRatio="xMidYMid meet"
                  overflow="visible"
                  class="line-chart"
                >
                  <g v-if="chartPoints.length" class="latency-guides">
                    <line
                      class="latency-guide-mid"
                      x1="0"
                      :y1="latencyChart.yMidGuide"
                      x2="400"
                      :y2="latencyChart.yMidGuide"
                    />
                    <line
                      class="latency-guide-80"
                      x1="0"
                      :y1="latencyChart.y80"
                      x2="400"
                      :y2="latencyChart.y80"
                    />
                  </g>
                  <path
                    v-for="(seg, idx) in latencyPathSegments"
                    :key="idx"
                    :d="seg.d"
                    fill="none"
                    stroke="#4a90e2" 
                    stroke-width="2.75"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    vector-effect="non-scaling-stroke"
                    class="path-animation latency-stroke"
                  />

                  <g class="chart-point" v-for="(p, i) in chartPoints" :key="i">
                    <circle :cx="p.x" :cy="p.y" r="14" fill="transparent" class="hover-trigger" />
                    
                    <circle
                      :cx="p.x"
                      :cy="p.y"
                      r="5"
                      :fill="p.val >= 80 ? '#e74c3c' : '#4a90e2'"
                      class="dot-fill"
                    />

                    <g class="tooltip">
                      <rect :x="p.x - 20" :y="p.y - 35" width="40" height="22" rx="4" fill="rgba(0,0,0,0.8)" />
                      <text :x="p.x" :y="p.y - 20" text-anchor="middle" fill="white" font-size="10">
                        {{ p.val }}ms
                      </text>
                    </g>
                  </g>
                 
                </svg>
              </div>
              <div class="latency-label">
                <span>최근 샘플</span>
                <span class="info-msg">💡 점에 마우스를 올리면 수치가 보입니다</span>
                <span
                  >현재
                  <strong :class="{ 'latency-value-warn': (Number(spat.currentLatency) || 0) >= 80 }">{{
                    spat.currentLatency?.toFixed?.(1) ?? 0
                  }}ms</strong></span
                >
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="bottom-row">
      <h3>⚠️ 이상 탐지 기기 상세 현황</h3>
      <p v-if="!abnormalList.length" class="empty-hint">이상 탐지 데이터가 없습니다.</p>
      <table v-else class="list-table">
        <thead>
          <tr>
            <th>교차로 ID</th>
            <th>기기 ID</th>
            <th>위험 점수</th>
            <th>등급</th>
            <th>탐지 시간</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(row, idx) in abnormalList"
            :key="idx"
            :class="{ urgent: row.riskLevel && row.riskLevel !== '정상' }"
          >
            <td>{{ row.intersectionId }}</td>
            <td>{{ row.deviceId }}</td>
            <td>{{ row.riskScore }}</td>
            <td>
              <span class="tag" :class="riskTagClass(row.riskLevel)">{{ row.riskLevel }}</span>
            </td>
            <td>{{ formatDt(row.analysisTime) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { GoogleMap, Marker } from 'vue3-google-map'
import { ref, inject, computed, onMounted, onUnmounted, watch } from 'vue'
import * as dashApi from '@/api/dashboard'

const isDarkMode = inject('isDarkMode', ref(false))
const loadError = ref('')

const summary = ref({
  totalDevices: 0,
  normalDevices: 0,
  abnormalDevices: 0,
  abnormalRate: 0,
  averageRiskScore: 0,
})
const abnormalList = ref([])
const mapMarkers = ref([])
const spat = ref({
  spatSuccessRate: 0,
  status: '',
  currentSignal: '',
  remainTimeSeconds: 0,
  packetStatus: '',
  currentLatency: 0,
  latencyTrend: [],
})
const selectedIntersectionId = ref('')

const defaultCenter = { lat: 37.4493, lng: 126.7012 }

const intersectionDevices = computed(() =>
  mapMarkers.value.map((m) => ({
    id: m.id,
    name: m.name,
    status: m.status === 'error' ? 'error' : 'normal',
    position: { lat: m.lat, lng: m.lng },
  })),
)

const mapCenter = computed(() => {
  const first = mapMarkers.value[0]
  if (first && first.lat != null && first.lng != null) {
    return { lat: first.lat, lng: first.lng }
  }
  return defaultCenter
})

const analysisDeviceOptions = computed(() =>
  intersectionDevices.value.slice(0, 3).map((device) => ({
    id: device.id,
    label: `${device.id} (${device.name})`,
  })),
)

function ensureSelectedIntersection() {
  const options = analysisDeviceOptions.value
  if (!options.length) {
    selectedIntersectionId.value = ''
    return
  }
  if (!options.some((opt) => opt.id === selectedIntersectionId.value)) {
    selectedIntersectionId.value = options[0].id
  }
}

const LAT_PLOT_TOP = 30
const LAT_PLOT_BOT = 78

function buildLatencyChart(trend) {
  if (!trend?.length) {
    return {
      points: [],
      yMidGuide: (LAT_PLOT_TOP + LAT_PLOT_BOT) / 2,
      y80: LAT_PLOT_BOT - (80 / 100) * (LAT_PLOT_BOT - LAT_PLOT_TOP),
      msMax: 100,
    }
  }
  const vals = trend.map((v) => Number(v))
  let msMax = Math.max(100, ...vals, 80)
  msMax = Math.ceil(msMax / 20) * 20
  const msMin = 0
  const span = msMax - msMin || 1
  const yFor = (ms) => LAT_PLOT_BOT - ((ms - msMin) / span) * (LAT_PLOT_BOT - LAT_PLOT_TOP)

  const valFor = (v) => Math.round(Number(v) * 10) / 10
  const n = vals.length

  let points
  if (n === 1) {
    const v = vals[0]
    const y = yFor(v)
    const val = valFor(v)
    points = [
      { x: 0, y, val },
      { x: 400, y, val },
    ]
  } else {
    points = vals.map((v, i) => ({
      x: (i / (n - 1)) * 400,
      y: yFor(v),
      val: valFor(v),
    }))
  }

  return {
    points,
    yMidGuide: (LAT_PLOT_TOP + LAT_PLOT_BOT) / 2,
    y80: yFor(80),
    msMax,
  }
}

const latencyChart = computed(() => buildLatencyChart(spat.value.latencyTrend || []))

const chartPoints = computed(() => latencyChart.value.points)

const latencyPathSegments = computed(() => {
  const pts = chartPoints.value
  if (pts.length < 2) return []

  const path = []
  let d = `M${pts[0].x},${pts[0].y}`

  for (let i = 0; i < pts.length - 1; i++) {
    const p0 = pts[i]
    const p1 = pts[i + 1]

    const dx = (p1.x - p0.x) * 0.5

    d += ` C ${p0.x + dx},${p0.y} ${p1.x - dx},${p1.y} ${p1.x},${p1.y}`
  }

  path.push({
    d,
   
  })

  return path
})

function fmtNum(v) {
  if (v == null) return '0'
  return Number(v).toLocaleString('ko-KR')
}

function fmtPct(v) {
  if (v == null) return '0%'
  return `${Number(v).toFixed(2)}%`
}

/** Jackson BigDecimal 등이 객체로 오면 숫자로만 표시 */
function fmtAvgScore(v) {
  if (v == null || v === '') return '0'
  if (typeof v === 'number' && !Number.isNaN(v)) return v.toFixed(2)
  const n = Number(v)
  if (!Number.isNaN(n)) return n.toFixed(2)
  return String(v)
}

function donutGradient(rate) {
  const r = Math.min(100, Math.max(0, Number(rate) || 0))
  return `conic-gradient(#2ecc71 0% ${r}%, #eee ${r}% 100%)`
}

function formatDt(t) {
  if (t == null) return '-'
  if (typeof t === 'string') return t.replace('T', ' ').slice(0, 19)
  return String(t)
}

function riskTagClass(level) {
  if (!level) return 'blue'
  if (level === '위험' || level === '경고') return 'red'
  if (level === '주의') return 'yellow'
  return 'blue'
}

/** API가 camelCase / snake_case 어느 쪽이든 요약 카드에 숫자가 나가도록 정규화 */
function mapDashboardSummary(raw) {
  const z = (v) => (v === undefined || v === null || v === '' ? 0 : Number(v))
  const pick = (a, b) => (a !== undefined && a !== null ? a : b)
  if (!raw || typeof raw !== 'object') {
    return {
      totalDevices: 0,
      normalDevices: 0,
      abnormalDevices: 0,
      abnormalRate: 0,
      averageRiskScore: 0,
    }
  }
  return {
    totalDevices: z(pick(raw.totalDevices, raw.total_devices)),
    normalDevices: z(pick(raw.normalDevices, raw.normal_devices)),
    abnormalDevices: z(pick(raw.abnormalDevices, raw.abnormal_devices)),
    abnormalRate: z(pick(raw.abnormalRate, raw.abnormal_rate)),
    averageRiskScore: z(pick(raw.averageRiskScore, raw.average_risk_score)),
  }
}

function mapAbnormalRows(rows) {
  if (!Array.isArray(rows)) return []
  return rows.map((row) => ({
    intersectionId: row.intersectionId ?? row.intersection_id ?? '',
    deviceId: row.deviceId ?? row.device_id ?? '',
    riskScore: row.riskScore ?? row.risk_score,
    riskLevel: row.riskLevel ?? row.risk_level ?? '',
    analysisTime: row.analysisTime ?? row.analysis_time,
  }))
}

function mapMapMarkers(rows) {
  if (!Array.isArray(rows)) return []
  return rows.map((row) => ({
    id: row.id,
    name: row.name,
    status: row.status,
    lat: row.latitude ?? row.lat,
    lng: row.longitude ?? row.lng,
  }))
}

async function loadDashboard() {
  loadError.value = ''
  try {
    const [s, a, m] = await Promise.all([
      dashApi.getDashboardSummary(),
      dashApi.getAbnormalList(),
      dashApi.getMapMarkers(),
    ])
    summary.value = mapDashboardSummary(s)
    abnormalList.value = mapAbnormalRows(a)
    mapMarkers.value = mapMapMarkers(m)
    ensureSelectedIntersection()
    await loadSpatLatency()
  } catch (e) {
    loadError.value = e instanceof Error ? e.message : '대시보드 데이터를 불러오지 못했습니다.'
  }
}

/** KPI·이상 목록·지도 마커 갱신 (실시간 시뮬 반영, SPAT는 별도 폴링) */
async function loadSummaryAndAbnormal() {
  try {
    const [s, a, m] = await Promise.all([
      dashApi.getDashboardSummary(),
      dashApi.getAbnormalList(),
      dashApi.getMapMarkers(),
    ])
    summary.value = mapDashboardSummary(s)
    abnormalList.value = mapAbnormalRows(a)
    mapMarkers.value = mapMapMarkers(m)
    ensureSelectedIntersection()
  } catch {
    /* 무시 — 초기 loadDashboard 오류만 표시 */
  }
}

async function loadSpatLatency() {
  try {
    const sp = await dashApi.getSpatLatency(selectedIntersectionId.value)
    spat.value = sp && typeof sp === 'object' ? { ...spat.value, ...sp } : spat.value
  } catch {
    /* 무시 */
  }
}

let spatPoll = null
let summaryPoll = null

onMounted(() => {
  loadDashboard()
  spatPoll = setInterval(async () => {
    loadSpatLatency()
  }, 2000)
  summaryPoll = setInterval(loadSummaryAndAbnormal, 4000)
})

watch(analysisDeviceOptions, () => {
  ensureSelectedIntersection()
})

onUnmounted(() => {
  if (spatPoll) clearInterval(spatPoll)
  if (summaryPoll) clearInterval(summaryPoll)
})

const mapDarkStyle = [
  { elementType: 'geometry', stylers: [{ color: '#242f3e' }] },
  { elementType: 'labels.text.stroke', stylers: [{ color: '#242f3e' }] },
  { elementType: 'labels.text.fill', stylers: [{ color: '#746855' }] },
  { featureType: 'road', elementType: 'geometry', stylers: [{ color: '#38414e' }] },
  { featureType: 'road', elementType: 'geometry.stroke', stylers: [{ color: '#212a37' }] },
  { featureType: 'road', elementType: 'labels.text.fill', stylers: [{ color: '#9ca5b3' }] },
  { featureType: 'water', elementType: 'geometry', stylers: [{ color: '#17263c' }] },
]
</script>

<style scoped>
.load-error {
  color: #c0392b;
  padding: 8px 12px;
  background: #fdecea;
  border-radius: 8px;
  font-size: 14px;
}
.empty-hint {
  color: #888;
  font-size: 14px;
  margin: 8px 0;
}
.dashboard-content {
  display: flex;
  flex-direction: column;
  gap: 15px;
  padding-bottom: 8px;
  max-width: 100%;
  box-sizing: border-box;
}
.kpi-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 20px;
}
.kpi-card {
  background: white;
  padding: 20px;
  border-radius: 15px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}
.kpi-card span {
  font-size: 15px;
  color: #888;
  display: block;
  margin-bottom: 8px;
  text-align: center;
}
.kpi-card strong {
  font-size: 26px;
  text-align: center;
  color: #333;
  display: block;
}
.kpi-card.error strong {
  color: #e74c3c;
}
.middle-row {
  display: grid;
  grid-template-columns: minmax(0, 6fr) minmax(0, 4fr);
  gap: 20px;
}
.map-container {
  background: white;
  padding: 20px;
  border-radius: 15px;
  min-height: 400px;
  min-width: 0;
}
.chart-container {
  background: white;
  padding: 14px 16px 16px;
  border-radius: 15px;
  min-width: 0;
  min-height: 0;
  align-self: start;
}
.chart-container > h3 {
  margin: 0 0 4px;
  font-size: 1rem;
}
.analysis-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}
.analysis-header h3 {
  margin: 0;
  font-size: 1rem;
}
.device-select-wrap {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #666;
}
.device-select-wrap select {
  border: 1px solid #d1d5db;
  border-radius: 6px;
  padding: 4px 8px;
  font-size: 12px;
  background: #fff;
  color: #222;
}
.bottom-row {
  background: white;
  padding: 15px;
  border-radius: 15px;
  min-width: 0;
}
.analysis-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 6px;
}
.sub-title {
  font-size: 13px;
  color: #666;
  font-weight: 600;
  display: block;
  margin-bottom: 6px;
}
.spat-donut-wrapper {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 2px 0 4px;
}
.donut-chart {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.inner-circle {
  width: 74px;
  height: 74px;
  background: white;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.inner-circle .percent {
  font-size: 18px;
  font-weight: 800;
  color: #2ecc71;
}
.inner-circle .label {
  font-size: 11px;
  color: #aaa;
}

.spat-details p {
  font-size: 14px;
  margin: 5px 0;
  color: #333 !important;
}
.spat-details p strong {
  color: #000 !important;
}

/* 꺽은선 + 하단 라벨을 감싸는 단순 사각 박스만 */
.latency-chart-area {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 10px 12px 8px;
  overflow: visible;
}
.line-chart-container {
  width: 100%;
  aspect-ratio: 424 / 118;
  overflow: visible;
  box-sizing: border-box;
}
.line-chart {
  display: block;
  width: 100%;
  height: 100%;
  overflow: visible;
}
.latency-guides line {
  vector-effect: non-scaling-stroke;
}
.latency-guide-mid {
  stroke: #d1d5db;
  stroke-width: 1;
  stroke-dasharray: 5 5;
}
.latency-guide-80 {
  stroke: #f97373;
  stroke-width: 1;
  stroke-dasharray: 4 4;
  opacity: 0.95;
}
.latency-value-warn {
  color: #e74c3c !important;
}
.latency-stroke {
  filter: none;
}
.dot-fill {
  filter: none;
}
.path-animation {
  stroke-dasharray: 1000;
  stroke-dashoffset: 1000;
  animation: dash 2s forwards;
}
@keyframes dash {
  to {
    stroke-dashoffset: 0;
  }
}
.tooltip {
  visibility: hidden;
  opacity: 0;
  transition: 0.2s;
  pointer-events: none;
}
.chart-point:hover .tooltip {
  visibility: visible;
  opacity: 1;
}
.hover-trigger {
  cursor: pointer;
}
.latency-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 6px;
  font-size: 11px;
  color: #888;
}
.info-msg {
  font-size: 10px;
  color: #bbb;
}
.list-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 15px;
}
.list-table th {
  text-align: left;
  padding: 12px;
  border-bottom: 2px solid #eee;
  color: #666;
  font-size: 13px;
}
.list-table td {
  padding: 12px;
  border-bottom: 1px solid #eee;
  font-size: 13px;
}
.urgent {
  background: rgba(231, 76, 60, 0.03);
}
.tag {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: bold;
}
.tag.red {
  background: #fee2e2;
  color: #ef4444;
}
.tag.yellow {
  background: #fef3c7;
  color: #d97706;
}
.tag.blue {
  background: #dbeafe;
  color: #2563eb;
}

.dark-theme .kpi-card,
.dark-theme .map-container,
.dark-theme .chart-container,
.dark-theme .bottom-row {
  background-color: #1e1e1e !important;
  color: #eee !important;
  border: 1px solid #333 !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.5) !important;
}
.dark-theme .inner-circle {
  background-color: #1e1e1e !important;
}
.dark-theme .latency-chart-area {
  background: #1c1c1e !important;
  border: 1px solid #333 !important;
}
.dark-theme .latency-guide-mid {
  stroke: #52525b !important;
}
.dark-theme .latency-guide-80 {
  stroke: #f87171 !important;
}
.dark-theme .latency-value-warn {
  color: #f87171 !important;
}
.dark-theme .latency-label,
.dark-theme .latency-label .info-msg {
  color: #a1a1aa !important;
}
.dark-theme h3,
.dark-theme strong {
  color: #ffffff !important;
}
.dark-theme .kpi-card span,
.dark-theme .sub-title {
  color: #e4dbdb !important;
}
.dark-theme .device-select-wrap {
  color: #d4d4d8 !important;
}
.dark-theme .device-select-wrap select {
  background: #111827;
  border-color: #374151;
  color: #f3f4f6;
}
.dark-theme .list-table th {
  border-bottom: 2px solid #444 !important;
  color: #bbb !important;
}
.dark-theme .list-table td {
  border-bottom: 1px solid #333 !important;
  color: #ddd !important;
}
.dark-theme .spat-details p,
.dark-theme .spat-details p strong {
  color: #ffffff !important;
}
.dark-theme {
  --color-background: #0f1115;
  --color-text: #ffffff;
}
</style>
