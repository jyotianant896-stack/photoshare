import { useCallback, useEffect } from 'react'

/** Full size view with keyboard paging. Used by both the lab and the client room. */
export default function Lightbox({ photos, index, onClose, onIndexChange }) {
  const photo = photos[index]

  const step = useCallback(
    (delta) => {
      const next = (index + delta + photos.length) % photos.length
      onIndexChange(next)
    },
    [index, photos.length, onIndexChange],
  )

  useEffect(() => {
    function onKey(event) {
      if (event.key === 'Escape') onClose()
      if (event.key === 'ArrowRight') step(1)
      if (event.key === 'ArrowLeft') step(-1)
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose, step])

  if (!photo) return null

  return (
    <div className="lightbox" role="dialog" aria-modal="true" aria-label={photo.filename}>
      <button type="button" className="lightbox-close" onClick={onClose} aria-label="Close">
        ×
      </button>
      {photos.length > 1 && (
        <>
          <button
            type="button"
            className="lightbox-step prev"
            onClick={() => step(-1)}
            aria-label="Previous photo"
          >
            ‹
          </button>
          <button
            type="button"
            className="lightbox-step next"
            onClick={() => step(1)}
            aria-label="Next photo"
          >
            ›
          </button>
        </>
      )}
      <img src={photo.url} alt={photo.filename} />
      <p className="lightbox-meta">
        {photo.filename} · {index + 1} of {photos.length}
      </p>
    </div>
  )
}
