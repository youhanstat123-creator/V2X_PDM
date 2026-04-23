import axios from 'axios'

const api = axios.create({
  // 1004번 백엔드 포트로 주소를 직접 고정합니다.
  baseURL: 'http://localhost:1004', 
  headers: {
    'Content-Type': 'application/json',
  },
})

export default api