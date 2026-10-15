import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import api, { getErrorMessage } from "../services/api.js";
import { useAuth } from "../context/AuthContext.jsx";
import { EmptyState, ErrorMessage, Loading, SuccessMessage } from "../components/Feedback.jsx";

export default function JobDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [job, setJob] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [applying, setApplying] = useState(false);

  useEffect(() => {
    let active = true;
    api.get(`/api/jobs/${id}`)
      .then(({ data }) => { if (active) setJob(data); })
      .catch((requestError) => { if (active) setError(getErrorMessage(requestError)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [id]);

  async function apply() {
    if (!user) {
      navigate("/login", { state: { from: `/jobs/${id}` } });
      return;
    }
    setApplying(true);
    setError("");
    setSuccess("");
    try {
      await api.post(`/api/students/jobs/${id}/applications`);
      setSuccess("Your application was submitted.");
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setApplying(false);
    }
  }

  if (loading) return <Loading label="Loading role details" />;
  if (!job) return <section className="page-narrow"><ErrorMessage>{error}</ErrorMessage><EmptyState title="Role unavailable">This role may have closed or may no longer be accepting applications.</EmptyState><Link to="/" className="text-link">← Back to opportunities</Link></section>;

  return (
    <section className="page-narrow detail-page">
      <Link to="/" className="text-link">← All opportunities</Link>
      <article className="detail-card">
        <div className="detail-company"><div className="company-avatar company-avatar-large">{job.companyName?.slice(0, 1) || "C"}</div><div><span className="eyebrow">{job.companyName}</span><h1>{job.title}</h1><p>{job.location || "Location not specified"} · {job.employmentType || "Full time"}</p></div></div>
        <div className="detail-meta">
          <div><span>Application deadline</span><strong>{job.applicationDeadline}</strong></div>
          <div><span>Minimum CGPA</span><strong>{job.minimumCgpa ?? "Not specified"}</strong></div>
          <div><span>Eligible branch</span><strong>{job.eligibleBranch || "All branches"}</strong></div>
          {job.salary != null && <div><span>Compensation</span><strong>₹ {Number(job.salary).toLocaleString("en-IN")}</strong></div>}
        </div>
        <h2>About the role</h2><p className="detail-description">{job.description}</p>
        {job.requiredSkills?.length > 0 && <><h2>Skills to bring</h2><div className="skill-list">{job.requiredSkills.map((skill) => <span className="skill-chip" key={skill}>{skill}</span>)}</div></>}
        <ErrorMessage>{error}</ErrorMessage><SuccessMessage>{success}</SuccessMessage>
        <div className="detail-actions">
          {user?.role === "STUDENT" ? <button className="button button-primary" disabled={applying || Boolean(success)} onClick={apply}>{applying ? "Submitting…" : success ? "Application sent" : "Apply for this role"}</button>
            : user ? <p className="muted">Sign in as a student to apply.</p>
              : <button className="button button-primary" onClick={apply}>Sign in to apply</button>}
          <span>Applications are checked against the role's eligibility requirements.</span>
        </div>
      </article>
    </section>
  );
}
