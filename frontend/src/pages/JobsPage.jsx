import { useEffect, useState } from "react";
import api, { getErrorMessage } from "../services/api.js";
import { EmptyState, ErrorMessage, Loading } from "../components/Feedback.jsx";
import JobCard from "../components/JobCard.jsx";

export default function JobsPage({ embedded = false }) {
  const [filters, setFilters] = useState({ company: "", location: "", branch: "", cgpa: "" });
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [query, setQuery] = useState(filters);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");
    const params = Object.fromEntries(Object.entries(query).filter(([, value]) => value !== ""));
    api.get("/api/jobs", { params })
      .then(({ data }) => { if (active) setJobs(data); })
      .catch((requestError) => { if (active) setError(getErrorMessage(requestError)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [query]);

  function updateFilter(event) {
    setFilters((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  function search(event) {
    event.preventDefault();
    setQuery(filters);
  }

  return (
    <section className={`jobs-section ${embedded ? "jobs-embedded" : ""}`}>
      <div className="section-heading">
        <div><span className="eyebrow">FIND YOUR FIT</span><h2>Open opportunities</h2><p>Roles approved by your placement team, all in one place.</p></div>
        <span className="result-count">{jobs.length} {jobs.length === 1 ? "role" : "roles"}</span>
      </div>
      <form className="filter-bar" onSubmit={search}>
        <label><span>Company</span><input name="company" value={filters.company} onChange={updateFilter} placeholder="e.g. Acme" /></label>
        <label><span>Location</span><input name="location" value={filters.location} onChange={updateFilter} placeholder="e.g. Bengaluru" /></label>
        <label><span>Eligible branch</span><input name="branch" value={filters.branch} onChange={updateFilter} placeholder="e.g. Computer Science" /></label>
        <label className="cgpa-filter"><span>Minimum CGPA</span><input name="cgpa" type="number" min="0" max="10" step="0.01" value={filters.cgpa} onChange={updateFilter} placeholder="e.g. 7.5" /></label>
        <button className="button button-primary" type="submit">Search roles</button>
      </form>
      <ErrorMessage>{error}</ErrorMessage>
      {loading ? <Loading label="Finding opportunities" /> : jobs.length ? (
        <div className="job-grid">{jobs.map((job) => <JobCard key={job.id} job={job} />)}</div>
      ) : <EmptyState title="No open roles found">Try changing your filters, or check again later for new opportunities.</EmptyState>}
    </section>
  );
}
