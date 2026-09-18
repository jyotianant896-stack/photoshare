import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext.jsx'
import { messageFrom } from '../api/client.js'
import Notice from '../components/Notice.jsx'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      await login(email, password)
      navigate(location.state?.from || '/events', { replace: true })
    } catch (caught) {
      setError(messageFrom(caught, 'Email or password is incorrect.'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth-page">
      <form className="auth-card" onSubmit={submit}>
        <h1>Sign in to Frame</h1>
        <p className="lede">Upload, curate and deliver event photographs.</p>

        <Notice tone="error">{error}</Notice>

        <label className="field">
          <span>Email</span>
          <input
            type="email"
            required
            autoComplete="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
        </label>
        <label className="field">
          <span>Password</span>
          <input
            type="password"
            required
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </label>

        <button className="btn" type="submit" disabled={busy} style={{ width: '100%' }}>
          {busy ? 'Signing in…' : 'Sign in'}
        </button>

        <p className="auth-alt">
          Running a studio? <Link to="/register">Create a lead account</Link>
        </p>

        <div className="demo-hint">
          Demo accounts
          <br />
          Lead: <code>admin@trizen-ai.com</code> / <code>Admin@12345</code>
          <br />
          Photographer: <code>photographer@trizen-ai.com</code> / <code>Member@12345</code>
        </div>
      </form>
    </div>
  )
}
