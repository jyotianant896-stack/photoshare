import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import api, { messageFrom } from '../api/client.js'
import TopBar from '../components/TopBar.jsx'
import Notice from '../components/Notice.jsx'
import Uploader from '../components/Uploader.jsx'
import PhotoSheet from '../components/PhotoSheet.jsx'
import TeamPanel from '../components/TeamPanel.jsx'
import GalleryPanel from '../components/GalleryPanel.jsx'
import Lightbox from '../components/Lightbox.jsx'

export default function EventPage() {
  const { eventId } = useParams()
  const [event, setEvent] = useState(null)
  const [photos, setPhotos] = useState([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [tab, setTab] = useState('photos')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [openIndex, setOpenIndex] = useState(null)

  const loadEvent = useCallback(async () => {
    try {
      const { data } = await api.get(`/api/events/${eventId}`)
      setEvent(data)
    } catch (caught) {
      setError(messageFrom(caught, 'That event is not available.'))
    }
  }, [eventId])

  const loadPhotos = useCallback(
    async (nextPage = 0) => {
      try {
        const { data } = await api.get(`/api/events/${eventId}/photos`, {
          params: { page: nextPage, size: 60 },
        })
        setPhotos(data.items)
        setTotal(data.totalItems)
        setTotalPages(Math.max(data.totalPages, 1))
        setPage(data.page)
      } catch (caught) {
        setError(messageFrom(caught))
      }
    },
    [eventId],
  )

  useEffect(() => {
    setLoading(true)
    Promise.all([loadEvent(), loadPhotos(0)]).finally(() => setLoading(false))
  }, [loadEvent, loadPhotos])

  async function deletePhoto(photo) {
    if (!window.confirm(`Delete ${photo.filename}? This cannot be undone.`)) return
    try {
      await api.delete(`/api/photos/${photo.id}`)
      await Promise.all([loadPhotos(page), loadEvent()])
    } catch (caught) {
      setError(messageFrom(caught))
    }
  }

  if (loading) {
    return (
      <>
        <TopBar />
        <p className="loading">Loading the event…</p>
      </>
    )
  }

  if (!event) {
    return (
      <>
        <TopBar />
        <main className="shell page">
          <Notice tone="error">{error || 'That event is not available.'}</Notice>
          <Link className="backlink" to="/events">
            Back to events
          </Link>
        </main>
      </>
    )
  }

  return (
    <>
      <TopBar />
      <main className="shell page">
        <Link className="backlink" to="/events">
          ← All events
        </Link>

        <div className="page-head">
          <div className="grow">
            <h1>{event.name}</h1>
            <p>
              {event.description || 'No description yet.'}{' '}
              {event.canManage
                ? 'You are the lead on this event.'
                : 'You can upload photos and see your own uploads.'}
            </p>
          </div>
        </div>

        <Notice tone="error" onDismiss={() => setError('')}>
          {error}
        </Notice>

        <div className="tabs" role="tablist">
          <button
            role="tab"
            className="tab"
            aria-selected={tab === 'photos'}
            onClick={() => setTab('photos')}
          >
            Photos<span className="tab-count">{total}</span>
          </button>
          {event.canManage && (
            <button
              role="tab"
              className="tab"
              aria-selected={tab === 'gallery'}
              onClick={() => setTab('gallery')}
            >
              Client gallery
            </button>
          )}
          <button
            role="tab"
            className="tab"
            aria-selected={tab === 'team'}
            onClick={() => setTab('team')}
          >
            Team<span className="tab-count">{event.memberCount}</span>
          </button>
        </div>

        {tab === 'photos' && (
          <>
            <Uploader
              eventId={eventId}
              onUploaded={() => {
                loadPhotos(0)
                loadEvent()
              }}
            />

            {photos.length === 0 ? (
              <div className="empty">
                <strong>No photos yet</strong>
                {event.canManage
                  ? 'Uploads from every photographer on this event land here.'
                  : 'Your uploads will appear here.'}
              </div>
            ) : (
              <>
                <div className="sheet-bar">
                  <span className="grow">
                    {event.canManage
                      ? `${total} photos from the whole team`
                      : `${total} photos you uploaded`}
                  </span>
                  {totalPages > 1 && (
                    <>
                      <button
                        type="button"
                        className="btn quiet small"
                        disabled={page === 0}
                        onClick={() => loadPhotos(page - 1)}
                      >
                        Previous
                      </button>
                      <span>
                        Page {page + 1} of {totalPages}
                      </span>
                      <button
                        type="button"
                        className="btn quiet small"
                        disabled={page + 1 >= totalPages}
                        onClick={() => loadPhotos(page + 1)}
                      >
                        Next
                      </button>
                    </>
                  )}
                </div>
                <PhotoSheet
                  photos={photos}
                  onOpen={(index) => setOpenIndex(index)}
                  onDelete={deletePhoto}
                />
              </>
            )}
          </>
        )}

        {tab === 'gallery' && event.canManage && (
          <GalleryPanel eventId={eventId} photos={photos} onPublished={loadEvent} />
        )}

        {tab === 'team' && (
          <TeamPanel eventId={eventId} canManage={event.canManage} onChanged={loadEvent} />
        )}
      </main>

      {openIndex !== null && (
        <Lightbox
          photos={photos}
          index={openIndex}
          onIndexChange={setOpenIndex}
          onClose={() => setOpenIndex(null)}
        />
      )}
    </>
  )
}
