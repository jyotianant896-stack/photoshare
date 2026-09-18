import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api, { messageFrom } from '../api/client.js'
import { useAuth } from '../auth/AuthContext.jsx'
import TopBar from '../components/TopBar.jsx'
import Notice from '../components/Notice.jsx'

function formatDate(value) {
  if (!value) return 'No date set'
  return new Date(value).toLocaleDateString(undefined, {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })
}

export default function EventsPage() {
  const { isAdmin } = useAuth()
  const [events, setEvents] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [creating, setCreating] = useState(false)
  const [form, setForm] = useState({ name: '', description: '', eventDate: '' })
  const [busy, setBusy] = useState(false)

  async function load() {
    try {
      const { data } = await api.get('/api/events')
      setEvents(data)
    } catch (caught) {
      setError(messageFrom(caught))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  async function createEvent(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      await api.post('/api/events', {
        name: form.name,
        description: form.description || undefined,
        eventDate: form.eventDate || undefined,
      })
      setForm({ name: '', description: '', eventDate: '' })
      setCreating(false)
      await load()
    } catch (caught) {
      setError(messageFrom(caught))
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <TopBar />
      <main className="shell page">
        <div className="page-head">
          <div className="grow">
            <h1>Events</h1>
            <p>
              {isAdmin
                ? 'Every shoot you run, with the team and the client gallery attached.'
                : 'The shoots you have been assigned to.'}
            </p>
          </div>
          {isAdmin && (
            <button type="button" className="btn" onClick={() => setCreating((open) => !open)}>
              {creating ? 'Cancel' : 'New event'}
            </button>
          )}
        </div>

        <Notice tone="error" onDismiss={() => setError('')}>
          {error}
        </Notice>

        {creating && (
          <form className="panel" style={{ marginBottom: 24 }} onSubmit={createEvent}>
            <label className="field">
              <span>Event name</span>
              <input
                required
                autoFocus
                placeholder="Arjun & Priya Wedding"
                value={form.name}
                onChange={(event) => setForm({ ...form, name: event.target.value })}
              />
            </label>
            <label className="field">
              <span>Description</span>
              <textarea
                placeholder="Two day celebration, three photographers."
                value={form.description}
                onChange={(event) => setForm({ ...form, description: event.target.value })}
              />
            </label>
            <label className="field">
              <span>Date</span>
              <input
                type="date"
                value={form.eventDate}
                onChange={(event) => setForm({ ...form, eventDate: event.target.value })}
              />
            </label>
            <button className="btn" type="submit" disabled={busy}>
              {busy ? 'Creating…' : 'Create event'}
            </button>
          </form>
        )}

        {loading ? (
          <p className="loading">Loading events…</p>
        ) : events.length === 0 ? (
          <div className="empty">
            <strong>No events yet</strong>
            {isAdmin
              ? 'Create one, add your photographers, and the uploads can start.'
              : 'Your lead will add you to an event when a shoot is booked.'}
          </div>
        ) : (
          <div className="event-list">
            {events.map((event) => (
              <Link className="event-row" key={event.id} to={`/events/${event.id}`}>
                <div>
                  <div className="title">{event.name}</div>
                  <div className="meta">
                    {formatDate(event.eventDate)} · led by {event.ownerName}
                    {event.galleryPublished && (
                      <>
                        {' '}
                        · <span className="live-dot">gallery live</span>
                      </>
                    )}
                  </div>
                </div>
                <div className="counts">
                  <div className="count">
                    <b>{event.photoCount}</b>
                    <span>photos</span>
                  </div>
                  <div className="count">
                    <b>{event.memberCount}</b>
                    <span>team</span>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )}
      </main>
    </>
  )
}
