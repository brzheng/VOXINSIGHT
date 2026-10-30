const TOKEN_KEY = 'voxinsight-admin-token'
const NAME_KEY = 'voxinsight-admin-name'

export function token() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function accountName() {
  return localStorage.getItem(NAME_KEY) || ''
}

export function setSession(value, loginName) {
  if (value) {
    localStorage.setItem(TOKEN_KEY, value)
    localStorage.setItem(NAME_KEY, loginName || '')
  } else {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(NAME_KEY)
  }
}

export async function api(path, { method = 'GET', body, form } = {}) {
  const headers = {}
  const saved = token()
  if (saved) headers.Authorization = `Bearer ${saved}`
  if (body && !form) headers['Content-Type'] = 'application/json'
  const response = await fetch(path, {
    method,
    headers,
    body: form ?? (body ? JSON.stringify(body) : undefined),
  })
  const text = await response.text()
  let data = null
  if (text) {
    try {
      data = JSON.parse(text)
    } catch {
      if (!response.ok) throw new Error('请求失败')
    }
  }
  if (response.status === 401 && !path.startsWith('/api/auth/')) {
    setSession(null)
    if (location.pathname !== '/login') location.assign('/login')
  }
  if (!response.ok) throw new Error(data?.message || '请求失败')
  return data
}
