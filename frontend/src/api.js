const BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080'

export function getToken() {
  return localStorage.getItem('token')
}

export function setSession(token, user) {
  localStorage.setItem('token', token)
  localStorage.setItem('user', JSON.stringify(user))
}

export function clearSession() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
}

export function storedUser() {
  try {
    return JSON.parse(localStorage.getItem('user'))
  } catch {
    return null
  }
}

export async function api(path, { method = 'GET', body } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`
  let res
  try {
    res = await fetch(BASE + path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) })
  } catch {
    throw new Error('Cannot reach the server. Is the Spring Boot backend running on ' + BASE + '?')
  }
  if (res.status === 401 && path !== '/api/auth/login') {
    clearSession()
    window.location.href = '/login'
    return
  }
  const text = await res.text()
  const data = text ? JSON.parse(text) : null
  if (!res.ok) throw new Error((data && data.message) || `Request failed (${res.status})`)
  return data
}
