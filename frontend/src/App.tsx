import { useEffect, useState } from 'react'
import { apiGet, apiPostForm, UnauthorizedError } from './api'
import { LoginForm } from './auth/LoginForm'

type Me = {
  id: string
  email: string
}

/** The signed-in user, or null when signed out. */
async function fetchMe(): Promise<Me | null> {
  try {
    return await apiGet<Me>('/api/auth/me')
  } catch (e) {
    if (e instanceof UnauthorizedError) {
      return null
    }
    throw e
  }
}

export default function App() {
  // undefined while loading, null when signed out
  const [me, setMe] = useState<Me | null | undefined>(undefined)

  useEffect(() => {
    fetchMe().then(setMe)
  }, [])

  async function signOut() {
    await apiPostForm('/api/auth/logout', {})
    setMe(null)
  }

  if (me === undefined) {
    return <p>Loading…</p>
  }
  if (me === null) {
    return <LoginForm onSignedIn={() => fetchMe().then(setMe)} />
  }
  return (
    <main>
      <p>Signed in as {me.email}</p>
      <button type="button" onClick={signOut}>
        Sign out
      </button>
    </main>
  )
}
