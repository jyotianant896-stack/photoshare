import { Link } from 'react-router-dom'

export default function NotFound() {
  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1>Nothing here</h1>
        <p className="lede">
          That address does not match an event or a gallery.
        </p>
        <Link className="btn" to="/events" style={{ textDecoration: 'none' }}>
          Back to events
        </Link>
      </div>
    </div>
  )
}
