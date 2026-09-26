import { useEffect, useState } from 'react'

export default function App() {
  const [message, setMessage] = useState('Loading…')

  useEffect(() => {
    fetch('/api/hello')
      .then((res) => res.json())
      .then((data: { message: string }) => setMessage(data.message))
      .catch(() => setMessage('Backend unreachable'))
  }, [])

  return <h1>{message}</h1>
}
