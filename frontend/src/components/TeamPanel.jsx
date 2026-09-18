import { useEffect, useState } from 'react'
import api, { messageFrom } from '../api/client.js'
import Notice from './Notice.jsx'

function initials(name) {
  return name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0].toUpperCase())
    .join('')
}

export default function TeamPanel({ eventId, canManage, onChanged }) {
  const [members, setMembers] = useState([])
  const [loading, setLoading] = useState(true)
  const [form, setForm] = useState({ name: '', email: '', password: '' })
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [created, setCreated] = useState(null)

  async function load() {
    setLoading(true)
    try {
      const { data } = await api.get(`/api/events/${eventId}/members`)
      setMembers(data)
    } catch (caught) {
      setError(messageFrom(caught))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [eventId])

  async function addMember(event) {
    event.preventDefault()
    setSaving(true)
    setError('')
    setCreated(null)
    try {
      const payload = {
        name: form.name,
        email: form.email,
        password: form.password ? form.password : undefined,
      }
      const { data } = await api.post(`/api/events/${eventId}/members`, payload)
      setForm({ name: '', email: '', password: '' })
      setCreated(data)
      await load()
      onChanged?.()
    } catch (caught) {
      setError(messageFrom(caught))
    } finally {
      setSaving(false)
    }
  }

  async function remove(member) {
    if (!window.confirm(`Remove ${member.name} from this event?`)) return
    try {
      await api.delete(`/api/events/${eventId}/members/${member.id}`)
      await load()
      onChanged?.()
    } catch (caught) {
      setError(messageFrom(caught))
    }
  }

  return (
    <div className="two-col">
      <div>
        <Notice tone="error" onDismiss={() => setError('')}>
          {error}
        </Notice>

        {loading ? (
          <p className="loading">Loading the team…</p>
        ) : members.length === 0 ? (
          <div className="empty">
            <strong>No photographers on this event yet</strong>
            Add one and they can start uploading straight away.
          </div>
        ) : (
          <div className="member-list">
            {members.map((member) => (
              <div className="member" key={member.id}>
                <span className="avatar">{initials(member.name)}</span>
                <div className="who">
                  {member.name}
                  <span>{member.email}</span>
                </div>
                {canManage && (
                  <button
                    type="button"
                    className="btn danger small"
                    onClick={() => remove(member)}
                  >
                    Remove
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {canManage && (
        <form className="panel" onSubmit={addMember}>
          <h3 style={{ marginBottom: 14 }}>Add a photographer</h3>

          {created && (
            <Notice tone="success" onDismiss={() => setCreated(null)}>
              {created.generatedPassword
                ? `Account created. Share these once: ${created.email} / ${created.generatedPassword}`
                : `${created.name} now has access to this event.`}
            </Notice>
          )}

          <label className="field">
            <span>Name</span>
            <input
              required
              value={form.name}
              onChange={(event) => setForm({ ...form, name: event.target.value })}
              placeholder="Meera Iyer"
            />
          </label>
          <label className="field">
            <span>Email</span>
            <input
              required
              type="email"
              value={form.email}
              onChange={(event) => setForm({ ...form, email: event.target.value })}
              placeholder="meera@studio.com"
            />
          </label>
          <label className="field">
            <span>Password (leave empty to generate one)</span>
            <input
              type="password"
              minLength={8}
              value={form.password}
              onChange={(event) => setForm({ ...form, password: event.target.value })}
              placeholder="At least 8 characters"
            />
          </label>
          <button className="btn" type="submit" disabled={saving}>
            {saving ? 'Adding…' : 'Add to event'}
          </button>
        </form>
      )}
    </div>
  )
}
