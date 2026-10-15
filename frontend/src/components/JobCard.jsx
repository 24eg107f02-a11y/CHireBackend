import { Link } from "react-router-dom";
import StatusBadge from "./StatusBadge.jsx";

export default function JobCard({ job }) {
  return (
    <article className="job-card">
      <div className="job-card-top">
        <div className="company-avatar">{job.companyName?.slice(0, 1) || "C"}</div>
        <div className="job-card-heading">
          <h3>{job.title}</h3>
          <p>{job.companyName}</p>
        </div>
        {job.status && <StatusBadge status={job.status} />}
      </div>
      <div className="job-meta">
        <span>⌖ {job.location || "Location not specified"}</span>
        <span>◷ {job.employmentType || "Full time"}</span>
        {job.salary != null && <span>₹ {Number(job.salary).toLocaleString("en-IN")}</span>}
      </div>
      <p className="job-summary">{job.description}</p>
      <div className="job-card-bottom">
        <span className="deadline">Apply by {job.applicationDeadline}</span>
        <Link className="button button-outline button-small" to={`/jobs/${job.id}`}>View details <span aria-hidden="true">→</span></Link>
      </div>
    </article>
  );
}
