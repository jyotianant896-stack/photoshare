import { useEffect, useMemo, useState } from 'react'
import api, { messageFrom } from '../api/client.js'
import Notice from './Notice.jsx'
import PhotoSheet from './PhotoSheet.jsx'

function CopyRow({ label, value, big = false }) {
  const [copied, setCopied] = useState(false)
  return (
    <div className="credential">
      <span>{label}</span>
      <div className="value">
        <span className={big ? 'pin-value' : ''} style={{ flex: 1 }}>
          {value}
        </span>
        <button
          type="button"
          className="btn secondary small"
          onClick={async () => {
            try {
              await navigator.clipboard.writeText(value)
              setCopied(true)
              setTimeout(() => setCopied(false), 1600)
            } catch {
              setCopied(false)
            }
          }}
        >
          {copied ? 'Copied' : 'Copy'}
        </button>
      </div>
    </div>
  )
}

/**
 * Curate then publish. Selection is saved explicitly rather than on every click,
 * so an admin can work through a thousand frames and commit once.
 */
export default function GalleryPanel({ eventId, photos, onPublished }) {
  const [gallery, setGallery] = useState(null)
  const [selected, setSelected] = useState([])
  const [saved, setSaved] = useState([])
  const [title, setTitle] = useState('')
  const [pin, setPin] = useState('')
  const [expiresAt, setExpiresAt] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [credentials, setCredentials] = useState(null)

  useEffect(() => {
    api
      .get(`/api/events/${eventId}/gallery`)
      .then(({ data }) => {
        setGallery(data)
        setSelected(data.selectedPhotoIds)
        setSaved(data.selectedPhotoIds)
        setTitle(data.title || '')
      })
      .catch((caught) => setError(messageFrom(caught)))
  }, [eventId])

  const dirty = useMemo(() => {
    if (selected.length !== saved.length) return true
    const savedSet = new Set(saved)
    return selected.some((id) => !savedSet.has(id))
  }, [selected, saved])

  function toggle(photoId) {
    setSelected((current) =>
      current.includes(photoId)
        ? current.filter((id) => id !== photoId)
        : [...current, photoId],
    )
  }

  async function saveSelection() {
    setBusy(true)
    setError('')
    try {
      const { data } = await api.put(`/api/events/${eventId}/gallery/selection`, {
        photoIds: selected,
      })
      setGallery(data)
      setSaved(data.selectedPhotoIds)
      setMessage(`Saved ${data.selectedCount} photos for the client.`)
    } catch (caught) {
      setError(messageFrom(caught))
    } finally {
      setBusy(false)
    }
  }

  async function publish() {
    setBusy(true)
    setError('')
    setMessage('')
    try {
      if (dirty) {
        const { data } = await api.put(`/api/events/${eventId}/gallery/selection`, {
          photoIds: selected,
        })
        setSaved(data.selectedPhotoIds)
      }
      const { data } = await api.post(`/api/events/${eventId}/gallery/publish`, {
        title: title || undefined,
        pin: pin || undefined,
        expiresAt: expiresAt ? new Date(expiresAt).toISOString() : undefined,
      })
      setCredentials(data)
      setPin('')
      const refreshed = await api.get(`/api/events/${eventId}/gallery`)
      setGallery(refreshed.data)
      onPublished?.()
    } catch (caught) {
      setError(messageFrom(caught))
    } finally {
      setBusy(false)
    }
  }

  async function unpublish() {
    if (!window.confirm('Take the gallery offline? The link stops working immediately.')) return
    setBusy(true)
    try {
      const { data } = await api.post(`/api/events/${eventId}/gallery/unpublish`)
      setGallery(data)
      setCredentials(null)
      setMessage('The gallery is offline. Publishing again issues a new PIN.')
      onPublished?.()
    } catch (caught) {
      setError(messageFrom(caught))
    } finally {
      setBusy(false)
    }
  }

  if (photos.length === 0) {
    return (
      <div className="empty">
        <strong>Nothing to curate yet</strong>
        Once the team uploads photos, pick the ones the client should see.
      </div>
    )
  }

  return (
    <div className="two-col">
      <div>
        <div className="sheet-bar">
          <span className="grow">
            {selected.length} of {photos.length} marked for the client
          </span>
          <button
            type="button"
            className="btn quiet small"
            onClick={() => setSelected(photos.map((photo) => photo.id))}
          >
            Mark all
          </button>
          <button type="button" className="btn quiet small" onClick={() => setSelected([])}>
            Clear
          </button>
          <button
            type="button"
            className="btn small"
            onClick={saveSelection}
            disabled={busy || !dirty}
          >
            {dirty ? 'Save selection' : 'Selection saved'}
          </button>
        </div>

        <Notice tone="error" onDismiss={() => setError('')}>
          {error}
        </Notice>
        <Notice tone="success" onDismiss={() => setMessage('')}>
          {message}
        </Notice>

        <PhotoSheet photos={photos} selectable selectedIds={selected} onToggle={toggle} />
      </div>

      <div className="panel">
        <h3 style={{ marginBottom: 6 }}>Send to the client</h3>
        <p style={{ color: 'var(--muted)', fontSize: '0.86rem', marginBottom: 18 }}>
          Publishing creates a link and a PIN. The PIN is shown once and stored hashed,
          so keep a copy before you close this.
        </p>

        {credentials && (
          <>
            <CopyRow label="Gallery link" value={credentials.shareUrl} />
            <CopyRow label="Access PIN" value={credentials.pin} big />
          </>
        )}

        {gallery?.published && !credentials && (
          <>
            <CopyRow label="Gallery link" value={gallery.shareUrl} />
            <p style={{ color: 'var(--muted)', fontSize: '0.84rem', marginBottom: 16 }}>
              Opened {gallery.viewCount} times. Publish again to issue a new PIN.
            </p>
          </>
        )}

        <label className="field">
          <span>Gallery title</span>
          <input value={title} onChange={(event) => setTitle(event.target.value)} />
        </label>

        <label className="field">
          <span>PIN (leave empty for a random six digit one)</span>
          <input
            value={pin}
            inputMode="numeric"
            pattern="\d{4,8}"
            placeholder="482917"
            onChange={(event) => setPin(event.target.value.replace(/\D/g, '').slice(0, 8))}
          />
        </label>

        <label className="field">
          <span>Expires (optional)</span>
          <input
            type="date"
            value={expiresAt}
            onChange={(event) => setExpiresAt(event.target.value)}
          />
        </label>

        <div className="row">
          <button
            type="button"
            className="btn"
            onClick={publish}
            disabled={busy || selected.length === 0}
          >
            {gallery?.published ? 'Publish again' : 'Publish gallery'}
          </button>
          {gallery?.published && (
            <button type="button" className="btn danger small" onClick={unpublish} disabled={busy}>
              Take offline
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
