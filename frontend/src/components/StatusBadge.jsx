export default function StatusBadge({ status }) {
  const tone = {
    APPROVED: "success",
    SELECTED: "success",
    SHORTLISTED: "info",
    PENDING_APPROVAL: "warning",
    APPLIED: "neutral",
    REJECTED: "danger",
    CLOSED: "neutral",
    DRAFT: "neutral",
  }[status] || "neutral";
  return <span className={`badge badge-${tone}`}>{String(status || "UNKNOWN").replaceAll("_", " ")}</span>;
}
