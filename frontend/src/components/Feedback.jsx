export function ErrorMessage({ children }) {
  if (!children) return null;
  return <div className="feedback feedback-error" role="alert">{children}</div>;
}

export function SuccessMessage({ children }) {
  if (!children) return null;
  return <div className="feedback feedback-success" role="status">{children}</div>;
}

export function Loading({ label = "Loading" }) {
  return <div className="loading" role="status"><span className="spinner" />{label}…</div>;
}

export function EmptyState({ title, children }) {
  return <div className="empty-state"><span className="empty-icon">✳</span><strong>{title}</strong><p>{children}</p></div>;
}
