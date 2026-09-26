import { useState, type FormEvent } from 'react'
import { apiPostForm, UnauthorizedError } from '../api'

type Props = {
  onSignedIn: () => void
}

export function LoginForm({ onSignedIn }: Props) {
  const [step, setStep] = useState<'email' | 'code'>('email')
  const [email, setEmail] = useState('')
  const [code, setCode] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function sendCode(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await apiPostForm('/api/auth/code', { username: email })
      setStep('code')
    } catch {
      setError("We couldn't send a code. Check the email address and try again.")
    } finally {
      setBusy(false)
    }
  }

  async function verifyCode(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await apiPostForm('/api/auth/code/verify', { username: email, token: code })
      onSignedIn()
    } catch (e) {
      setError(e instanceof UnauthorizedError ? 'That code is wrong or has expired.' : 'Something went wrong. Try again.')
    } finally {
      setBusy(false)
    }
  }

  function useDifferentEmail() {
    setStep('email')
    setCode('')
    setError(null)
  }

  if (step === 'email') {
    return (
      <form onSubmit={sendCode}>
        <label>
          Email
          <input
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </label>
        <button type="submit" disabled={busy}>
          Send sign-in code
        </button>
        {error && <p role="alert">{error}</p>}
      </form>
    )
  }

  return (
    <form onSubmit={verifyCode}>
      <p>We sent a 6-digit code to {email}.</p>
      <label>
        Code
        <input
          inputMode="numeric"
          autoComplete="one-time-code"
          pattern="[0-9]{6}"
          maxLength={6}
          required
          value={code}
          onChange={(e) => setCode(e.target.value)}
        />
      </label>
      <button type="submit" disabled={busy}>
        Sign in
      </button>
      <button type="button" onClick={useDifferentEmail}>
        Use a different email
      </button>
      {error && <p role="alert">{error}</p>}
    </form>
  )
}
