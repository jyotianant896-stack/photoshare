import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext.jsx'

export default function TopBar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  function signOut() {
    logout()
    navigate('/login')
  }

  return (
    <header className="topbar">
      <div className="topbar-inner">
        <Link to="/events" className="wordmark">
          Frame
        </Link>
        <div className="topbar-spacer" />
        {user && (
          <>
            <div className="whoami">
              <strong>{user.name}</strong>
              <span>{user.role === 'ADMIN' ? 'Studio lead' : 'Photographer'}</span>
            </div>
            <button type="button" className="btn secondary small" onClick={signOut}>
              Sign out
            </button>
          </>
        )}
      </div>
    </header>
  )
}
