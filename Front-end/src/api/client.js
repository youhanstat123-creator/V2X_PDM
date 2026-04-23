// 백엔드 포트를 1004로 설정
const BASE_URL = 'http://localhost:1004'; 

export async function parseJsonSafe(res) {
  const text = await res.text();
  if (!text) return {};
  try {
    return JSON.parse(text);
  } catch {
    return {};
  }
}

export async function apiGet(path) {
  // 경로 앞에 http://localhost:1004가 붙도록 수정
  const res = await fetch(`${BASE_URL.replace(/\/$/, '')}/${path.replace(/^\//, '')}`);
  const data = await parseJsonSafe(res);
  
  if (!res.ok) {
    const msg = data.message || data.error || res.statusText;
    throw new Error(typeof msg === 'string' ? msg : '요청에 실패했습니다.');
  }
  return data;
}