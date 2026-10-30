const TOKEN_KEY = 'voxinsight-token'

export function token() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(value) {
  if (value) {
    localStorage.setItem(TOKEN_KEY, value)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

export async function api(path, { method = 'GET', body, form } = {}) {
  const headers = {}
  const saved = token()
  if (saved) {
    headers.Authorization = `Bearer ${saved}`
  }
  if (body && !form) {
    headers['Content-Type'] = 'application/json'
  }
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
      if (!response.ok) {
        throw new Error('请求失败')
      }
    }
  }
  if (response.status === 401 && !path.startsWith('/api/auth/')) {
    setToken(null)
    if (location.pathname !== '/login') {
      location.assign('/login')
    }
  }
  if (!response.ok) {
    throw new Error(data?.message || '请求失败')
  }
  return data
}

export async function avatarObjectUrl() {
  const saved = token()
  if (!saved) return ''
  const response = await fetch('/api/profile/avatar', {
    headers: { Authorization: `Bearer ${saved}` },
  })
  if (!response.ok) return ''
  return URL.createObjectURL(await response.blob())
}
