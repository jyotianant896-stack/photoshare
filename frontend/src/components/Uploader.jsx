import { useRef, useState } from 'react'
import api, { messageFrom } from '../api/client.js'

/**
 * Sends the whole batch in one multipart request and reports back what landed.
 * The API answers with uploaded + failed, so a rejected file names itself
 * instead of failing the drop silently.
 */
export default function Uploader({ eventId, onUploaded }) {
  const inputRef = useRef(null)
  const [dragging, setDragging] = useState(false)
  const [busy, setBusy] = useState(false)
  const [progress, setProgress] = useState(0)
  const [failed, setFailed] = useState([])
  const [error, setError] = useState('')

  async function send(fileList) {
    const files = Array.from(fileList || [])
    if (files.length === 0) return

    const form = new FormData()
    files.forEach((file) => form.append('files', file))

    setBusy(true)
    setProgress(0)
    setFailed([])
    setError('')

    try {
      const { data } = await api.post(`/api/events/${eventId}/photos`, form, {
        onUploadProgress: (event) => {
          if (event.total) {
            setProgress(Math.round((event.loaded / event.total) * 100))
          }
        },
      })
      setFailed(data.failed || [])
      onUploaded(data)
    } catch (caught) {
      setError(messageFrom(caught, 'The upload did not go through.'))
    } finally {
      setBusy(false)
      setProgress(0)
      if (inputRef.current) inputRef.current.value = ''
    }
  }

  return (
    <div
      className={`dropzone ${dragging ? 'over' : ''}`}
      onDragOver={(event) => {
        event.preventDefault()
        setDragging(true)
      }}
      onDragLeave={() => setDragging(false)}
      onDrop={(event) => {
        event.preventDefault()
        setDragging(false)
        send(event.dataTransfer.files)
      }}
    >
      <strong>{busy ? 'Uploading…' : 'Drop photos here'}</strong>
      <p>JPEG, PNG, WebP or HEIC, up to 25 MB each and 50 at a time.</p>

      <input
        ref={inputRef}
        type="file"
        multiple
        accept="image/jpeg,image/png,image/webp,image/heic"
        style={{ display: 'none' }}
        onChange={(event) => send(event.target.files)}
      />
      <button
        type="button"
        className="btn"
        disabled={busy}
        onClick={() => inputRef.current?.click()}
      >
        Choose photos
      </button>

      {busy && (
        <div className="progress" aria-hidden="true">
          <i style={{ width: `${progress}%` }} />
        </div>
      )}

      {error && <p className="failed-list">{error}</p>}

      {failed.length > 0 && (
        <ul className="failed-list">
          {failed.map((item) => (
            <li key={item.filename}>
              {item.filename} — {item.reason}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
