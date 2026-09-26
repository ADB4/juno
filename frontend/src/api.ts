/** Shared client for calls to the Spring Boot API. Every feature should go through these helpers. */

export class UnauthorizedError extends Error {
  constructor() {
    super('Not signed in')
    this.name = 'UnauthorizedError'
  }
}

function readCookie(name: string): string | undefined {
  const prefix = `${name}=`
  return document.cookie
    .split('; ')
    .find((cookie) => cookie.startsWith(prefix))
    ?.slice(prefix.length)
}

// Spring Security keeps the CSRF token in the XSRF-TOKEN cookie and clears it on sign-in and sign-out.
// When it's missing, ask the backend for a fresh one.
async function csrfToken(): Promise<string> {
  let token = readCookie('XSRF-TOKEN')
  if (!token) {
    await fetch('/api/auth/csrf', { credentials: 'same-origin' })
    token = readCookie('XSRF-TOKEN')
  }
  if (!token) {
    throw new Error('Could not get a CSRF token')
  }
  return decodeURIComponent(token)
}

async function send(method: string, url: string, body?: BodyInit, contentType?: string): Promise<Response> {
  const headers = new Headers()
  if (contentType) {
    headers.set('Content-Type', contentType)
  }
  if (method !== 'GET') {
    headers.set('X-XSRF-TOKEN', await csrfToken())
  }
  const response = await fetch(url, { method, headers, body, credentials: 'same-origin' })
  if (response.status === 401) {
    throw new UnauthorizedError()
  }
  if (!response.ok) {
    throw new Error(`${method} ${url} failed with ${response.status}`)
  }
  return response
}

async function readJson<T>(response: Response): Promise<T> {
  return (response.status === 204 ? undefined : await response.json()) as T
}

export async function apiGet<T>(url: string): Promise<T> {
  return readJson<T>(await send('GET', url))
}

export async function apiPost<T = void>(url: string, data?: unknown): Promise<T> {
  const body = data === undefined ? undefined : JSON.stringify(data)
  return readJson<T>(await send('POST', url, body, body ? 'application/json' : undefined))
}

/** Sends form fields; Spring Security's login endpoints read form fields, not JSON. */
export async function apiPostForm(url: string, fields: Record<string, string>): Promise<void> {
  await send('POST', url, new URLSearchParams(fields))
}
