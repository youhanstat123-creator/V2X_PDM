<div align="center">

# 🛰 V2X_PDM

> **V2X 통신 기반 예지보전 통합 시스템 — 팀 프로젝트 (5인)**
> Vue3 + Pinia + Google Maps API로 구현한 실시간 V2X 모니터링 대시보드

![Vue.js](https://img.shields.io/badge/Vue.js-3.x-4FC08D?style=for-the-badge&logo=vue.js&logoColor=white)
![Pinia](https://img.shields.io/badge/Pinia-2.x-FFD859?style=for-the-badge&logo=pinia&logoColor=black)
![Vite](https://img.shields.io/badge/Vite-5.x-646CFF?style=for-the-badge&logo=vite&logoColor=white)
![JavaScript](https://img.shields.io/badge/JavaScript-ES6+-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Python](https://img.shields.io/badge/Python-3.x-3776AB?style=for-the-badge&logo=python&logoColor=white)

</div>

---

## 🎯 What & Why

| | |
|---|---|
| **무엇을** | V2X(차량-사물 통신) 장비의 실시간 성능 지표를 수집·분석하여 고장 예측 및 예지보전을 지원하는 통합 관리 시스템 |
| **왜 만들었나** | 도로 인프라의 V2X 장비 고장을 사전에 감지하여 교통 안전사고 예방 |
| **규모** | 5인 팀 프로젝트 (프론트엔드 담당: 한별) |

---

## 🏗 Architecture

V2X_PDM/
├── Front-end/          # Vue 3 SPA (담당)
│   └── src/
│       ├── api/        # axios API 모듈
│       ├── auth/       # 인증 처리
│       ├── components/ # 재사용 컴포넌트
│       ├── layouts/    # 레이아웃 (사이드바, 헤더)
│       ├── router/     # Vue Router (로그인 순서, 권한)
│       ├── stores/     # Pinia 상태관리
│       └── views/      # 화면 컴포넌트
├── Back-end/           # Spring Boot REST API
├── py/                 # Python 데이터 처리
├── models/             # AI 예지보전 모델
└── data/               # V2X 수집 데이터

---

## 💡 Technical Highlights

### 1. 🗺 Google Maps API 기반 교차로 장비 지도 시각화
vue3-google-map을 활용하여 V2X 장비가 설치된 교차로 위치를 지도에 마커로 표시. 마커 클릭 시 해당 기기의 실시간 상태 팝업 표출.

### 2. 📊 SPAT/Latency 기기 선택형 실시간 대시보드
교차로별 최대 3개 기기를 선택하면 선택 기기 기준으로 SPAT(신호 현시) 및 Latency 데이터가 동적으로 갱신. Pinia 스토어로 전역 기기 선택 상태 관리.

### 3. 🌙 다크모드 + 위험 임계값 시각화
다크/라이트 모드 전환 지원. 그래프에 위험 수치 기준선을 오버레이하여 정상/경고/위험 구간을 색상으로 즉시 식별 가능하도록 구현.

---

## 🛠 Tech Stack

| 구분 | 기술 |
|------|------|
| **Frontend** | Vue 3, Pinia 2, Vue Router 4, Vite 5 |
| **지도** | vue3-google-map |
| **HTTP 통신** | Axios |
| **코드 품질** | ESLint, Oxlint, Prettier |
| **Backend** | Java, Spring Boot |
| **Data/AI** | Python, AI 예지보전 모델 |

---

## 📊 Lessons Learned

**[이슈 1] 로그인 순서 버그** - 로그인 후 라우팅 순서가 의도와 다르게 동작 → Vue Router beforeEach 가드 순서 재정의로 해결

**[이슈 2] 실시간 그래프 위험 수치 표현** - Chart.js에 임계값 기준선이 렌더링되지 않음 → annotation 플러그인 적용 및 데이터 정규화 처리

---

## 🚀 Getting Started

cd Front-end && npm install && npm run dev

---

## 👤 My Contribution (담당 역할 - 한별)

팀 내 **프론트엔드 개발 전담**

- Vue Router 로그인 순서 및 인증 흐름 설계
- 대시보드 그래프 구현 (SPAT/Latency 기기 선택, 위험 수치 시각화)
- Google Maps API 연동 교차로 장비 위치 지도 표시
- Pinia 스토어를 활용한 전역 기기 선택 상태 관리
- 다크모드 UI 구현 및 글자색 최적화
- AI 분석 및 리포트 화면 구현
