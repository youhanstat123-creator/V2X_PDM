<template>
  <div class="dashboard-wrapper" :class="{ 'dark-theme': isDarkMode }">
    <aside class="sidebar">

      <router-link to="/dashboard" class="sidebar-logo-link">
        <div class="sidebar-logo">V2XCONTROL<span class="dot">.</span></div>
      </router-link>
      <nav class="menu-list">
        <router-link to="/dashboard" class="menu-item" active-class="active"
          >통합모니터링</router-link
        >
        <router-link to="/analysis" class="menu-item" active-class="active"
          >장비 정밀진단</router-link
        >
        <router-link to="/v2x" class="menu-item" active-class="active">V2X 통신관리</router-link>
        <router-link to="/recommend" class="menu-item" active-class="active">AI분석 및 리포트</router-link>
      </nav>

      <div class="logout-wrapper" style="margin-top: auto; padding: 20px">
        <button @click="handleLogout" class="logout-btn">Logout</button>
      </div>
    </aside>

    <div class="main-container">
      <header class="top-header">
        <div class="header-right">
          <div class="current-time">{{ currentTime }}</div>
          <div class="user-info">관리자님 환영합니다</div>
          <button @click="toggleDarkMode" class="dark-mode-btn">
            {{ isDarkMode ? '☀️' : '🌙' }}
          </button>
        </div>
      </header>

      <main class="content-area">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, provide, onMounted, onUnmounted } from 'vue' // 💡 lifecycle hooks 추가
import { useRouter } from 'vue-router'
import { clearAuthenticated } from '@/auth/session'

const router = useRouter()
const isDarkMode = ref(false)

// --- 💡 실시간 날짜 시간 로직 ---
const currentTime = ref('')

const updateTime = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth() + 1
  const date = now.getDate()
  const hours = now.getHours()
  const minutes = String(now.getMinutes()).padStart(2, '0')
  const seconds = String(now.getSeconds()).padStart(2, '0')
  const ampm = hours >= 12 ? '오후' : '오전'
  const displayHours = hours % 12 || 12

  currentTime.value = `${year}. ${month}. ${date}. ${ampm} ${displayHours}:${minutes}:${seconds}`
}

let timer
onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  clearInterval(timer)
})
// ------------------------------

provide('isDarkMode', isDarkMode)

const toggleDarkMode = () => {
  isDarkMode.value = !isDarkMode.value
}

const handleLogout = () => {
  if (confirm('로그아웃 하시겠습니까?')) {
    clearAuthenticated()
    router.push('/')
  }
}
</script>

<style scoped>
.dashboard-wrapper {
  display: flex;
  flex-direction: row;
  align-items: stretch;
  width: 100%;
  max-width: 100%;
  flex: 1;
  min-height: 0;
  min-width: 0;
  height: 100%;
  overflow: hidden;
}
.sidebar {
  flex-shrink: 0;
  width: 250px;
  min-width: 250px;
  max-width: 250px;
  background-color: #4a90e2;
  color: white;
  display: flex;
  flex-direction: column;
  padding: 30px 0;
  box-sizing: border-box;
}
.sidebar-logo {
  font-size: 28px;
  font-weight: 900;
  text-align: center;
  margin-bottom: 50px;
}
.dot {
  color: #f1c40f;
}
.menu-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.menu-item {
  padding: 18px 30px;
  color: rgba(255, 255, 255, 0.7);
  text-decoration: none;
  font-weight: 600;
  border-left: 5px solid transparent;
  box-sizing: border-box;
}
.menu-item:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.06);
}
.menu-item.active {
  background: rgba(255, 255, 255, 0.1);
  color: white;
  border-left-color: #f1c40f;
}
.main-container {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background-color: #f4f7f9;
  transition: all 0.3s;
}
.top-header {
  height: 70px;
  background: white;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 40px;
  border-bottom: 1px solid #eee;
  transition: all 0.3s;
}
.content-area {
  flex: 1;
  min-height: 0;
  padding: 15px;
  padding-bottom: 20px;
  overflow-x: hidden;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}
.page-title {
  font-size: 23px;
  font-weight: 700;
}
.logout-btn {
  width: 100%;
  padding: 10px;
  background: transparent;
  border: 1px solid white;
  color: white;
  cursor: pointer;
  border-radius: 4px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
}
/* 💡 시간 스타일 추가 */
.current-time {
  font-size: 15px;
  color: #414040;
  margin-right: 3px;
}
.user-info {
  font-weight: 600;
  color: #333;
}
.dark-mode-btn {
  background: #eee;
  border: none;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  cursor: pointer;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: 0.2s;
}
.dark-mode-btn:hover {
  background: #ddd;
  transform: scale(1.1);
}

/* --- 다크테마 설정 --- */
.dark-theme .main-container {
  background-color: #535151;
}
.dark-theme .top-header {
  background-color: #1e1e1e;
  border-bottom-color: #333;
  color: white;
}
/* 💡 다크모드 시 시간 글자색 */
.dark-theme .current-time {
  color: #aaa;
}
.dark-theme .user-info {
  color: #eee;
}
.dark-theme .dark-mode-btn {
  background: #333;
  color: white;
}

:deep(.dark-theme .kpi-card),
:deep(.dark-theme .map-container),
:deep(.dark-theme .chart-container),
:deep(.dark-theme .bottom-row) {
  background-color: #1e1e1e !important;
  color: #eee !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3) !important;
}

:deep(.dark-theme strong),
:deep(.dark-theme h3) {
  color: white !important;
}
:deep(.dark-theme .list-table th) {
  color: #999;
  border-bottom-color: #333;
}
:deep(.dark-theme .list-table td) {
  border-bottom-color: #333;
}
.dark-theme .sidebar {
  background-color: #1e1e1e !important;
  color: #eee !important;
  border-right: 1px solid #333;
}
.dark-theme .menu-item.active {
  border-left-color: #e67e22;
  background: rgba(255, 255, 255, 0.05);
}
.dark-theme .menu-item {
  border-left-color: transparent;
}
.dark-theme .logout-btn {
  border-color: #555;
  color: #aaa;
}
.dark-theme .logout-btn:hover {
  background: #333;
}

/* CSS 추가/수정 */
.sidebar-logo-link {
  text-decoration: none; /* 밑줄 제거 */
  display: block;        /* 영역 전체 클릭 가능하게 */
  cursor: pointer;       /* 마우스 포인터 모양 변경 */
}

.sidebar-logo {
  font-size: 28px;
  font-weight: 900;
  text-align: center;
  margin-bottom: 50px;
  color: white; /* 링크로 감싸면 색상이 변할 수 있으므로 명시적 지정 */
}
</style>
