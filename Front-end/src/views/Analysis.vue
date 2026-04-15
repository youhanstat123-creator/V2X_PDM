<template>
  <div class="analysis-view" :class="{ 'dark-theme': isDarkMode }">
    <div class="header">
      <div class="title-wrap">
        <h2>🛠️ 장비 예지보전 정밀 진단</h2>
        <div class="selector-group">
          <label>분석 대상 교차로:</label>
          <select v-model="selectedId" @change="updateData" class="intersection-select">
            <option v-for="item in intersectionList" :key="item.id" :value="item.id">
              {{ item.name }} ({{ item.id }})
            </option>
          </select>
        </div>
      </div>
    </div>

    <div class="top-prediction-row">
      <div class="v2x-card risk-mini-card" :class="riskData.risk_level">
        <span class="label">종합 위험 점수</span>
        <strong class="score">{{ riskData.total_risk_score }}<small>/100</small></strong>
        <div class="badge">{{ riskData.risk_level }}</div>
      </div>

      <div class="v2x-card rul-card">
        <div class="rul-info">
          <h3>⏳ 잔여 수명 예측 (RUL)</h3>
          <p class="rul-desc">
            현재 추세라면 메인보드가 약
            <strong class="text-danger">{{ riskData.remain_days }}일 이내</strong>에 완전히 고장 날
            확률이 <strong class="text-danger">{{ riskData.fail_prob }}%</strong>입니다.
          </p>
          <div class="rul-progress-container">
            <div class="rul-bar">
              <div
                class="rul-fill"
                :style="{
                  width: (100 - riskData.total_risk_score) + '%',
                  backgroundColor: riskData.total_risk_score > 60 ? '#e74c3c' : '#2ecc71',
                }"
              ></div>
            </div>
            <div class="rul-labels">
              <span>교체 필요</span>
              <span>정상 운영</span>
            </div>
          </div>
        </div>
      </div>

      <div class="breakdown-mini-column">
        <div class="v2x-card mini-sub-card">
          <span>제어기 위험 지수</span>
          <strong>{{ riskData.controller_risk_score }}</strong>
        </div>
        <div class="v2x-card mini-sub-card">
          <span>V2X 통신 위험 지수</span>
          <strong>{{ riskData.v2x_risk_score }}</strong>
        </div>
      </div>
    </div>

    <div class="v2x-card history-chart-card">
      <h3>📈 최근 1시간 위험 점수 추이 (실시간 10분 단위)</h3>
      <div class="chart-inner-bg">
        <div class="chart-container">
          <svg viewBox="0 0 1000 160" class="history-svg">
            <line v-for="i in 3" :key="i" x1="0" :y1="i * 40 + 20" x2="1000" :y2="i * 40 + 20" stroke="#f0f0f0" stroke-width="1" />
            <path :d="chartPath" fill="none" stroke="#3498db" stroke-width="2" stroke-linejoin="round" stroke-linecap="round" />
            <g v-for="(p, i) in historyPoints" :key="i">
              <circle :cx="p.x" :cy="p.y" r="4" :fill="p.val > 60 ? '#e74c3c' : '#3498db'" />
              <text :x="p.x" :y="p.y - 12" text-anchor="middle" font-size="11" font-weight="600" :fill="p.val > 60 ? '#e74c3c' : '#555'">
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

    <div class="detail-analysis-grid">
      <div class="v2x-card detail-card">
        <h3>🚥 제어기 상태 ({{ controllerLog.deviceId }})</h3>
        <div class="metrics-grid">
          <div class="metric-item"><span>CPU 온도</span><strong>{{ controllerLog.cpuTemp }}°C</strong></div>
          <div class="metric-item"><span>응답 속도</span><strong>{{ controllerLog.responseTimeMs }}ms</strong></div>
          <div class="metric-item"><span>가동 시간</span><strong>{{ controllerUptimeHours }}h</strong></div>
          <div class="metric-item"><span>에러 건수</span><strong class="text-warning">{{ controllerLog.errorCount }}</strong></div>
        </div>
      </div>
      <div class="v2x-card detail-card">
        <h3>📡 V2X 품질 ({{ commLog.deviceId }})</h3>
        <div class="metrics-grid">
          <div class="metric-item"><span>평균 지연</span><strong>{{ commLog.avgLatencyMs }}ms</strong></div>
          <div class="metric-item"><span>차량 접속</span><strong>{{ commLog.connectedVehicleCount }}</strong></div>
          <div class="metric-item"><span>통신 실패</span><strong class="text-danger">{{ commLog.commFailCount }}</strong></div>
          <div class="metric-item"><span>성공률</span><strong>{{ v2xSpatSuccessPercent }}</strong></div>
        </div>
      </div>
    </div>

    <div class="v2x-card analysis-report-box">
      <div class="comment-content">
        <div class="icon">💡</div>
        <p><strong>AI 분석 요약:</strong> {{ riskData.analysis_comment }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, inject } from 'vue'
import api from '@/api'

const isDarkMode = inject('isDarkMode', ref(false))
let dataTimer = null

const intersectionList = [
  { id: 'ICN-01', name: '인천시청입구 삼거리' },
  { id: 'ICN-02', name: '예술회관역 사거리' },
  { id: 'ICN-03', name: '중앙공원 사거리' },
]
const selectedId = ref('ICN-03')
const chartLabels = ref([])

const riskData = ref({ total_risk_score: 0, controller_risk_score: 0, v2x_risk_score: 0, risk_level: '정상', remain_days: 0, fail_prob: 0, analysis_comment: '' })
const controllerLog = ref({ deviceId: '-', cpuTemp: 0, responseTimeMs: 0, uptimeMin: 0, errorCount: 0 })
const commLog = ref({ deviceId: '-', spatSendCount: 0, spatFailCount: 0, avgLatencyMs: 0, commFailCount: 0, connectedVehicleCount: 0 })
const currentHistory = ref([0, 0, 0, 0, 0, 0, 0])

function normalizeControllerStatus(d) {
  if (!d || typeof d !== 'object') return { deviceId: '-', cpuTemp: 0, responseTimeMs: 0, uptimeMin: 0, errorCount: 0 }
  return {
    deviceId: d.deviceId ?? d.device_id ?? '-',
    cpuTemp: Number(d.cpuTemp ?? d.cpu_temp ?? 0),
    responseTimeMs: Number(d.responseTimeMs ?? d.response_time_ms ?? 0),
    uptimeMin: Number(d.uptimeMin ?? d.uptime_min ?? 0),
    errorCount: Number(d.errorCount ?? d.error_count ?? 0),
  }
}

function normalizeV2xStatus(d) {
  if (!d || typeof d !== 'object') return { deviceId: '-', spatSendCount: 0, spatFailCount: 0, avgLatencyMs: 0, commFailCount: 0, connectedVehicleCount: 0 }
  return {
    deviceId: d.deviceId ?? d.device_id ?? '-',
    spatSendCount: Number(d.spatSendCount ?? d.spat_send_count ?? 0),
    spatFailCount: Number(d.spatFailCount ?? d.spat_fail_count ?? 0),
    avgLatencyMs: Number(d.avgLatencyMs ?? d.avg_latency_ms ?? 0),
    commFailCount: Number(d.commFailCount ?? d.comm_fail_count ?? 0),
    connectedVehicleCount: Number(d.connectedVehicleCount ?? d.connected_vehicle_count ?? 0),
  }
}

const controllerUptimeHours = computed(() => Math.floor((controllerLog.value.uptimeMin || 0) / 60))
const v2xSpatSuccessPercent = computed(() => {
  const send = commLog.value.spatSendCount
  const fail = commLog.value.spatFailCount
  if (!send || send <= 0) return '—'
  return `${(((send - fail) / send) * 100).toFixed(1)}%`
})

const historyPoints = computed(() => {
  const width = 1000;
  const height = 160;
  const paddingX = 60;
  const paddingY = 40;
  const usableWidth = width - (paddingX * 2);
  const usableHeight = height - (paddingY * 2);

  return currentHistory.value.map((val, idx) => ({
    x: paddingX + (usableWidth / (currentHistory.value.length - 1)) * idx,
    y: (height - paddingY) - (val / 100 * usableHeight),
    val: val
  }))
})

const chartPath = computed(() => 
  historyPoints.value.map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x},${p.y}`).join(' ')
)

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
  const id = selectedId.value;
  try {
    const [paRes, histRes, ctrlRes, vxRes] = await Promise.allSettled([
      api.get(`/api/predictive/${id}`),
      api.get(`/api/predictive/${id}/history`),
      api.get(`/api/predictive/${id}/controller`),
      api.get(`/api/predictive/${id}/v2x`),
    ]);

    if (paRes.status === 'fulfilled') {
      const pa = paRes.value.data;
      riskData.value = {
        total_risk_score: Math.round(pa.total_risk_score || pa.totalRiskScore || 0),
        controller_risk_score: Math.round(pa.controller_risk_score || pa.controllerRiskScore || 0),
        v2x_risk_score: Math.round(pa.v2x_risk_score || pa.v2xRiskScore || 0),
        risk_level: pa.risk_level || pa.riskLevel || '정상',
        remain_days: pa.remain_days ?? pa.remainDays ?? 15,
        fail_prob: pa.fail_prob ?? pa.failProb ?? 5,
        analysis_comment: pa.analysis_comment || pa.analysisComment || '',
      };
    }

    if (histRes.status === 'fulfilled' && histRes.value.data?.length > 0) {
      const scores = histRes.value.data.map((h) => h.total_risk_score ?? h.score ?? 0);
      currentHistory.value = scores.length >= 7 ? [...scores.slice(-7)] : [...Array(7 - scores.length).fill(0), ...scores];
    }
    if (ctrlRes.status === 'fulfilled') controllerLog.value = normalizeControllerStatus(ctrlRes.value.data);
    if (vxRes.status === 'fulfilled') commLog.value = normalizeV2xStatus(vxRes.value.data);
  } catch (e) {
    console.error('Data update failed', e);
  }
}

onMounted(() => {
  updateData();
  dataTimer = setInterval(updateData, 5000);
})

onUnmounted(() => {
  if (dataTimer) clearInterval(dataTimer);
})
</script>

<style scoped>
.analysis-view {
  display: flex;
  flex-direction: column;
  gap: 15px;
  padding: 15px;
  width: 100%;
  box-sizing: border-box;
  background: #f4f7f9;
}

.header {
  display: flex;
  justify-content: space-between;
  padding-bottom: 15px;
  border-bottom: 2px solid #edf2f7;
}

/* ⭐ 공통 카드 스타일 */
.v2x-card {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
  box-sizing: border-box;
}

.intersection-select {
  padding: 6px 12px; border-radius: 6px; border: 1px solid #ddd; background: white; cursor: pointer;
}

/* 상단 레이아웃 */
.top-prediction-row {
  display: grid;
  grid-template-columns: 2fr 5.15fr 2fr;
  gap: 15px;
  align-items: stretch;
}

.risk-mini-card { text-align: center; border-top: 4px solid #2ecc71; }
.risk-mini-card.위험 { border-top-color: #e74c3c; }
.risk-mini-card .score { font-size: 30px; font-weight: 800; display: block; }
.risk-mini-card .badge {
  display: inline-block; padding: 2px 12px; border-radius: 20px;
  background: #2ecc71; color: white; font-size: 12px; margin-top: 5px;
}
.risk-mini-card.위험 .badge { background: #e74c3c; }

.rul-progress-container { margin-top: 15px; }
.rul-bar { height: 12px; background: #eee; border-radius: 6px; overflow: hidden; }
.rul-fill { height: 100%; transition: width 0.8s ease; }
.rul-labels { display: flex; justify-content: space-between; font-size: 11px; color: #999; margin-top: 5px; }

/* ⭐ 카드 분리 설정: 각 카드가 독립적으로 존재하도록 간격 부여 */
.breakdown-mini-column {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.mini-sub-card {
  padding: 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  border-top: none; /* 포인트 테두리 제거 */
}
.mini-sub-card span { font-size: 12px; color: #888; display: block; }
.mini-sub-card strong { font-size: 20px; color: #333; }

/* 그래프 */
.chart-inner-bg {
  background: #ffffff; border: 1px solid #eef1f5; border-radius: 10px;
  padding: 30px 10px 15px; margin-top: 15px;
}
.history-svg { width: 100%; height: auto; display: block; overflow: visible; }
.chart-labels {
  display: flex; justify-content: space-between; padding: 0 60px;
  font-size: 12px; color: #a0a0a0; margin-top: 15px;
}
.chart-labels .today { color: #3498db; font-weight: bold; }

/* 하단 상세 정보 */
.detail-analysis-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 15px;
}
.detail-card { border-top: none; } /* 포인트 테두리 제거 */
.detail-card h3 {
  font-size: 16px; margin-bottom: 15px; color: #334155;
  border-left: 4px solid #3498db; padding-left: 10px;
}
.metrics-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.metric-item {
  background: #f8fafc; padding: 15px; border-radius: 10px; text-align: center;
}
.metric-item span { font-size: 12px; color: #64748b; display: block; }
.metric-item strong { font-size: 18px; color: #1e293b; margin-top: 5px; display: block; }

.analysis-report-box { border-top: none; } /* 포인트 테두리 제거 */
.comment-content {
  display: flex; gap: 15px; align-items: center; background: #f0f7ff;
  padding: 15px; border-radius: 8px; font-size: 14px;
}

/* 다크모드 */
.dark-theme .analysis-view { background: #0f172a; }
.dark-theme .v2x-card { background: #1e293b; color: #f1f5f9; border: 1px solid #334155; }
.dark-theme .chart-inner-bg { background: #0f172a; border-color: #334155; }
.dark-theme .metric-item { background: #0f172a; }
.dark-theme strong, .dark-theme h2, .dark-theme h3 { color: #fff !important; }
</style>