/** One line of feedback. Says what happened, never apologises vaguely. */
export default function Notice({ tone = 'info', children, onDismiss }) {
  if (!children) return null
  return (
    <div className={`notice ${tone}`} role={tone === 'error' ? 'alert' : 'status'}>
      <div className="row">
        <span className="grow" style={{ flex: 1 }}>
          {children}
        </span>
        {onDismiss && (
          <button type="button" className="btn quiet small" onClick={onDismiss}>
            Dismiss
          </button>
        )}
      </div>
    </div>
  )
}
