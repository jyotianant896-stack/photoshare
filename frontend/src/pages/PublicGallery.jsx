import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import api, { messageFrom } from '../api/client.js'
import Notice from '../components/Notice.jsx'
import Lightbox from '../components/Lightbox.jsx'

const tokenKey = (slug) => `photoshare.gallery.${slug}`

/**
 * The customer's whole experience. No account: the PIN is exchanged for a short
 * lived token that is kept in session storage, so a refresh does not ask again
 * but closing the tab does.
 */
export default function PublicGallery() {
  const { slug } = useParams()
  const [teaser, setTeaser] = useState(null)
  const [gallery, setGallery] = useState(null)
  const [pin, setPin] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(true)
  const [openIndex, setOpenIndex] = useState(null)

  useEffect(() => {
    let cancelled = false

    async function boot() {
      const savedToken = sessionStorage.getItem(tokenKey(slug))
      if (savedToken) {
        try {
          const { data } = await api.get(`/api/public/galleries/${slug}/photos`, {
            headers: { 'X-Gallery-Token': savedToken },
          })
          if (!cancelled) {
            setGallery(data)
            setLoading(false)
          }
          return
        } catch {
          sessionStorage.removeItem(tokenKey(slug))
        }
      }
      try {
        const { data } = await api.get(`/api/public/galleries/${slug}`)
        if (!cancelled) setTeaser(data)
      } catch (caught) {
        if (!cancelled) setError(messageFrom(caught, 'This gallery link is not available.'))
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    boot()
    return () => {
      cancelled = true
    }
  }, [slug])

  async function unlock(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      const { data } = await api.post(`/api/public/galleries/${slug}/access`, { pin })
      sessionStorage.setItem(tokenKey(slug), data.accessToken)
      setGallery(data.gallery)
    } catch (caught) {
      setError(messageFrom(caught, 'That PIN is not correct.'))
      setPin('')
    } finally {
      setBusy(false)
    }
  }

  if (loading) {
    return (
      <div className="room">
        <p className="loading" style={{ color: 'var(--room-muted)' }}>
          Opening the gallery…
        </p>
      </div>
    )
  }

  if (gallery) {
    return (
      <div className="room">
        <div className="room-shell">
          <header className="room-head">
            <h1>{gallery.title}</h1>
            <p className="meta">
              {gallery.eventName}
              {gallery.eventDate
                ? ` · ${new Date(gallery.eventDate).toLocaleDateString(undefined, {
                    day: 'numeric',
                    month: 'long',
                    year: 'numeric',
                  })}`
                : ''}{' '}
              · {gallery.photoCount} photographs
            </p>
          </header>

          <div className="room-grid">
            {gallery.photos.map((photo, index) => (
              <button
                type="button"
                className="room-frame"
                key={photo.id}
                onClick={() => setOpenIndex(index)}
                aria-label={`Open ${photo.filename}`}
              >
                <img src={photo.url} alt={photo.filename} loading="lazy" />
              </button>
            ))}
          </div>

          <p className="room-foot">
            Shared privately by your photography team. Please do not pass the link
            and PIN on.
          </p>
        </div>

        {openIndex !== null && (
          <Lightbox
            photos={gallery.photos}
            index={openIndex}
            onIndexChange={setOpenIndex}
            onClose={() => setOpenIndex(null)}
          />
        )}
      </div>
    )
  }

  if (!teaser) {
    return (
      <div className="room unlock">
        <div className="unlock-card">
          <h1>Link unavailable</h1>
          <p className="lede">
            {error || 'This gallery has been taken offline or the link is wrong.'}
          </p>
        </div>
      </div>
    )
  }

  return (
    <div className="room unlock">
      <form className="unlock-card" onSubmit={unlock}>
        <h1>{teaser.title}</h1>
        <p className="lede">{teaser.eventName} · enter the PIN your photographer sent you</p>

        <Notice tone="error">{error}</Notice>

        <input
          className="pin-input"
          inputMode="numeric"
          autoComplete="one-time-code"
          maxLength={8}
          required
          autoFocus
          aria-label="Gallery PIN"
          value={pin}
          onChange={(event) => setPin(event.target.value.replace(/\D/g, ''))}
        />

        <button className="btn" type="submit" disabled={busy || pin.length < 4}>
          {busy ? 'Checking…' : 'View photographs'}
        </button>
      </form>
    </div>
  )
}
