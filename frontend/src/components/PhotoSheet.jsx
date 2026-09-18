/**
 * The contact sheet. Square frames, tight gutters, nothing between the eye and
 * the photographs. A red mark means the frame is going to the client.
 */
export default function PhotoSheet({
  photos,
  selectable = false,
  selectedIds = [],
  onToggle,
  onOpen,
  onDelete,
}) {
  const selected = new Set(selectedIds)

  return (
    <div className="sheet">
      {photos.map((photo, index) => {
        const isMarked = selected.has(photo.id)
        return (
          <div key={photo.id} className={`frame ${isMarked ? 'marked' : ''}`}>
            <button
              type="button"
              onClick={() => (selectable ? onToggle(photo.id) : onOpen(index))}
              style={{
                all: 'unset',
                display: 'block',
                width: '100%',
                height: '100%',
                cursor: selectable ? 'pointer' : 'zoom-in',
              }}
              aria-pressed={selectable ? isMarked : undefined}
              aria-label={
                selectable
                  ? `${isMarked ? 'Remove' : 'Add'} ${photo.filename} ${isMarked ? 'from' : 'to'} the client gallery`
                  : `Open ${photo.filename}`
              }
            >
              <img src={photo.url} alt={photo.filename} loading="lazy" />
            </button>

            {selectable && (
              <span className="mark" aria-hidden="true">
                ✓
              </span>
            )}

            {onDelete && (
              <button
                type="button"
                className="trash"
                onClick={() => onDelete(photo)}
                aria-label={`Delete ${photo.filename}`}
              >
                Delete
              </button>
            )}

            <span className="caption">
              {photo.filename}
              <br />
              {photo.uploadedByName}
            </span>
          </div>
        )
      })}
    </div>
  )
}
