import { apiGet } from './client'

export function getDashboardSummary() {
  return apiGet('/api/dashboard/summary')
}

export function getAbnormalList() {
  return apiGet('/api/dashboard/abnormal-list')
}

export function getMapMarkers() {
  return apiGet('/api/dashboard/map')
}

export function getSpatLatency(intersectionId) {
  const query = intersectionId ? `?intersectionId=${encodeURIComponent(intersectionId)}` : ''
  return apiGet(`/api/dashboard/spat-latency${query}`)
}
