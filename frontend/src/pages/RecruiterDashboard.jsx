import { useCallback, useEffect, useState } from "react";
import api, { getErrorMessage } from "../services/api.js";
import { ErrorMessage, EmptyState, Loading, SuccessMessage } from "../components/Feedback.jsx";
import StatusBadge from "../components/StatusBadge.jsx";

const today = new Date().toISOString().slice(0, 10);
const blankCompany = { name: "", description: "", website: "", headquarters: "" };
const blankJob = {
  companyId: "",
  title: "",
  description: "",
  location: "",
  employmentType: "FULL_TIME",
  salary: "",
  eligibleBranch: "",
  minimumCgpa: "",
  requiredSkills: "",
  applicationDeadline: "",
};

export default function RecruiterDashboard() {
  const [companies, setCompanies] = useState([]);
  const [jobs, setJobs] = useState([]);
  const [selectedJob, setSelectedJob] = useState("");
  const [applicants, setApplicants] = useState([]);
  const [company, setCompany] = useState(blankCompany);
  const [job, setJob] = useState(blankJob);
  const [editingCompanyId, setEditingCompanyId] = useState(null);
  const [editingJobId, setEditingJobId] = useState(null);
  const [interviewTimes, setInterviewTimes] = useState({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [companyResult, jobResult] = await Promise.all([
        api.get("/api/recruiters/companies"),
        api.get("/api/recruiters/jobs"),
      ]);
      setCompanies(companyResult.data);
      setJobs(jobResult.data);
      if (companyResult.data.length) {
        setJob((current) => ({ ...current, companyId: current.companyId || String(companyResult.data[0].id) }));
      }
      if (!companyResult.data.length) setJob(blankJob);
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const loadApplicants = useCallback(async () => {
    if (!selectedJob) {
      setApplicants([]);
      return;
    }
    setError("");
    try {
      const { data } = await api.get(`/api/recruiters/jobs/${selectedJob}/applications`);
      setApplicants(data);
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    }
  }, [selectedJob]);

  useEffect(() => { loadApplicants(); }, [loadApplicants]);

  function editCompany(item) {
    setEditingCompanyId(item.id);
    setCompany({
      name: item.name || "",
      description: item.description || "",
      website: item.website || "",
      headquarters: item.headquarters || "",
    });
  }

  function cancelCompanyEdit() {
    setEditingCompanyId(null);
    setCompany(blankCompany);
  }

  async function saveCompany(event) {
    event.preventDefault();
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      const { data } = editingCompanyId
        ? await api.put(`/api/recruiters/companies/${editingCompanyId}`, company)
        : await api.post("/api/recruiters/companies", company);
      setCompanies((current) => editingCompanyId
        ? current.map((item) => item.id === data.id ? data : item)
        : [...current, data]);
      if (!editingCompanyId) setJob((current) => ({ ...current, companyId: String(data.id) }));
      setCompany(blankCompany);
      setEditingCompanyId(null);
      setSuccess(editingCompanyId ? "Company profile updated." : "Company profile created.");
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setSaving(false);
    }
  }

  function editJob(item) {
    setEditingJobId(item.id);
    setJob({
      companyId: String(item.companyId),
      title: item.title || "",
      description: item.description || "",
      location: item.location || "",
      employmentType: item.employmentType || "FULL_TIME",
      salary: item.salary ?? "",
      eligibleBranch: item.eligibleBranch || "",
      minimumCgpa: item.minimumCgpa ?? "",
      requiredSkills: (item.requiredSkills || []).join(", "),
      applicationDeadline: item.applicationDeadline || "",
    });
    document.getElementById("job-form")?.scrollIntoView({ behavior: "smooth", block: "start" });
  }

  function cancelJobEdit() {
    setEditingJobId(null);
    setJob((current) => ({ ...blankJob, companyId: current.companyId || String(companies[0]?.id || "") }));
  }

  async function saveJob(event) {
    event.preventDefault();
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      const payload = {
        ...job,
        companyId: Number(job.companyId),
        salary: job.salary === "" ? null : Number(job.salary),
        minimumCgpa: job.minimumCgpa === "" ? null : Number(job.minimumCgpa),
        requiredSkills: job.requiredSkills.split(",").map((skill) => skill.trim()).filter(Boolean),
      };
      if (editingJobId) {
        await api.put(`/api/recruiters/jobs/${editingJobId}`, payload);
      } else {
        await api.post("/api/recruiters/jobs", payload);
      }
      const wasEditing = Boolean(editingJobId);
      setEditingJobId(null);
      setJob((current) => ({ ...blankJob, companyId: current.companyId || String(companies[0]?.id || "") }));
      await load();
      setSuccess(wasEditing
        ? "Job updated. It will return to the placement office for approval."
        : "Job submitted. It will appear publicly after placement officer approval.");
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setSaving(false);
    }
  }

  async function closeJob(jobId) {
    setError("");
    setSuccess("");
    try {
      await api.post(`/api/recruiters/jobs/${jobId}/close`);
      await load();
      setSuccess("Job posting closed.");
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    }
  }

  async function updateStatus(applicationId, status) {
    setError("");
    setSuccess("");
    try {
      await api.patch(`/api/recruiters/applications/${applicationId}/status`, { status });
      await loadApplicants();
      setSuccess(`Application ${status.toLowerCase()}.`);
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    }
  }

  async function scheduleInterview(applicationId) {
    const time = interviewTimes[applicationId];
    if (!time) {
      setError("Choose an interview date and time first.");
      return;
    }
    setError("");
    setSuccess("");
    try {
      await api.post(`/api/recruiters/applications/${applicationId}/interviews`, {
        scheduledAt: new Date(time).toISOString(),
        interviewType: "TECHNICAL",
        locationOrMeetingUrl: "",
        notes: "",
      });
      setSuccess("Interview scheduled.");
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    }
  }

  if (loading) return <Loading label="Loading your recruiter workspace" />;

  return (
    <section className="dashboard-page">
      <div className="dashboard-welcome">
        <div><span className="eyebrow">RECRUITER WORKSPACE</span><h1>Build your campus pipeline.</h1><p>Manage company profiles, open roles, and the candidates who could be your next great hire.</p></div>
        <div className="welcome-mark">↗</div>
      </div>
      <ErrorMessage>{error}</ErrorMessage><SuccessMessage>{success}</SuccessMessage>
      <div className="summary-cards">
        <div className="summary-card"><span>COMPANIES</span><strong>{companies.length}</strong></div>
        <div className="summary-card"><span>YOUR JOBS</span><strong>{jobs.length}</strong></div>
        <div className="summary-card"><span>LIVE ROLES</span><strong>{jobs.filter((item) => item.status === "APPROVED").length}</strong></div>
      </div>
      <div className="dashboard-grid recruiter-grid">
        <form className="panel" onSubmit={saveCompany}>
          <div className="panel-heading"><div><span className="eyebrow">YOUR ORGANIZATION</span><h2>Company profile</h2></div><span className="panel-icon">◈</span></div>
          {companies.length > 0 && <div className="company-list">{companies.map((item) => <div className="company-row" key={item.id}><div className="company-avatar">{item.name.slice(0, 1)}</div><div><strong>{item.name}</strong><span>{item.headquarters || "Headquarters not set"}</span></div><button className="text-button" type="button" onClick={() => editCompany(item)}>Edit</button></div>)}</div>}
          <label className="field"><span>Company name</span><input required maxLength="150" value={company.name} onChange={(e) => setCompany({ ...company, name: e.target.value })} placeholder="e.g. Northstar Labs" /></label>
          <label className="field"><span>About your company</span><textarea maxLength="1000" rows="2" value={company.description} onChange={(e) => setCompany({ ...company, description: e.target.value })} /></label>
          <div className="form-grid">
            <label className="field"><span>Website</span><input type="url" maxLength="255" value={company.website} onChange={(e) => setCompany({ ...company, website: e.target.value })} placeholder="https://" /></label>
            <label className="field"><span>Headquarters</span><input maxLength="120" value={company.headquarters} onChange={(e) => setCompany({ ...company, headquarters: e.target.value })} placeholder="City" /></label>
          </div>
          <div className="applicant-actions"><button className="button button-outline" disabled={saving}>{saving ? "Saving…" : editingCompanyId ? "Save company" : "Add company"}</button>{editingCompanyId && <button className="button button-quiet" type="button" onClick={cancelCompanyEdit}>Cancel</button>}</div>
        </form>
        <form className="panel" id="job-form" onSubmit={saveJob}>
          <div className="panel-heading"><div><span className="eyebrow">FIND YOUR NEXT HIRE</span><h2>{editingJobId ? "Edit role" : "Post a role"}</h2></div><span className="panel-icon">✦</span></div>
          {!companies.length ? <EmptyState title="Add a company first">Create a company profile before posting a role.</EmptyState> : <>
            <div className="form-grid">
              <label className="field field-span"><span>Company</span><select required value={job.companyId} onChange={(e) => setJob({ ...job, companyId: e.target.value })}>{companies.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
              <label className="field field-span"><span>Role title</span><input required maxLength="150" value={job.title} onChange={(e) => setJob({ ...job, title: e.target.value })} placeholder="Graduate Software Engineer" /></label>
              <label className="field field-span"><span>Description</span><textarea required maxLength="8000" rows="4" value={job.description} onChange={(e) => setJob({ ...job, description: e.target.value })} /></label>
              <label className="field"><span>Location</span><input maxLength="120" value={job.location} onChange={(e) => setJob({ ...job, location: e.target.value })} /></label>
              <label className="field"><span>Employment type</span><select value={job.employmentType} onChange={(e) => setJob({ ...job, employmentType: e.target.value })}><option>FULL_TIME</option><option>INTERNSHIP</option><option>CONTRACT</option></select></label>
              <label className="field"><span>Salary (annual)</span><input type="number" min="0" value={job.salary} onChange={(e) => setJob({ ...job, salary: e.target.value })} /></label>
              <label className="field"><span>Minimum CGPA</span><input type="number" min="0" max="10" step="0.01" value={job.minimumCgpa} onChange={(e) => setJob({ ...job, minimumCgpa: e.target.value })} /></label>
              <label className="field"><span>Eligible branch</span><input maxLength="100" value={job.eligibleBranch} onChange={(e) => setJob({ ...job, eligibleBranch: e.target.value })} placeholder="Any branch" /></label>
              <label className="field"><span>Apply by</span><input required type="date" min={today} value={job.applicationDeadline} onChange={(e) => setJob({ ...job, applicationDeadline: e.target.value })} /></label>
              <label className="field field-span"><span>Required skills <small>Comma-separated</small></span><input value={job.requiredSkills} onChange={(e) => setJob({ ...job, requiredSkills: e.target.value })} placeholder="Java, SQL, React" /></label>
            </div>
            <p className="form-hint">New roles are reviewed by the placement office before students can see them.</p>
            <div className="applicant-actions"><button className="button button-primary" disabled={saving}>{saving ? "Saving…" : editingJobId ? "Save changes" : "Submit for approval"}</button>{editingJobId && <button className="button button-quiet" type="button" onClick={cancelJobEdit}>Cancel</button>}</div>
          </>}
        </form>
      </div>
      <section className="panel recruiter-jobs-panel">
        <div className="panel-heading"><div><span className="eyebrow">YOUR PIPELINE</span><h2>Job postings & applicants</h2></div></div>
        {jobs.length ? <div className="table-wrap"><table><thead><tr><th>Role</th><th>Company</th><th>Status</th><th>Apply by</th><th>Manage</th></tr></thead><tbody>
          {jobs.map((item) => <tr key={item.id}><td><strong>{item.title}</strong><small>{item.location || "Location not set"}</small></td><td>{item.companyName}</td><td><StatusBadge status={item.status} /></td><td>{item.applicationDeadline}</td><td><div className="applicant-actions"><button className="text-button" onClick={() => { setSelectedJob(String(item.id)); document.querySelector(".applicant-panel")?.scrollIntoView({ behavior: "smooth", block: "start" }); }}>Applicants</button>{item.status !== "CLOSED" && <><button className="text-button" onClick={() => editJob(item)}>Edit</button><button className="text-button" onClick={() => closeJob(item.id)}>Close</button></>}</div></td></tr>)}
        </tbody></table></div> : <EmptyState title="No roles posted yet">Add a job above to start building your campus pipeline.</EmptyState>}
      </section>
      <section className="panel applicant-panel">
        <div className="panel-heading"><div><span className="eyebrow">CANDIDATE REVIEW</span><h2>Applicants</h2></div><select aria-label="Select job applicants" value={selectedJob} onChange={(e) => setSelectedJob(e.target.value)}><option value="">Select one of your jobs</option>{jobs.map((item) => <option key={item.id} value={item.id}>{item.title} · {item.companyName}</option>)}</select></div>
        {selectedJob && applicants.length ? <div className="applicant-list">{applicants.map((applicant) => <article className="applicant-card" key={applicant.applicationId}>
          <div className="applicant-top"><div><h3>{applicant.fullName}</h3><a href={`mailto:${applicant.email}`}>{applicant.email}</a></div><StatusBadge status={applicant.status} /></div>
          <div className="applicant-meta"><span>{applicant.branch || "Branch not set"}</span><span>CGPA {applicant.cgpa ?? "—"}</span><span>Class of {applicant.graduationYear || "—"}</span></div>
          <p><strong>Skills:</strong> {applicant.skills || "Not provided"}</p>
          {applicant.projects && <p><strong>Projects:</strong> {applicant.projects}</p>}
          {applicant.resumeUrl && <a href={applicant.resumeUrl} target="_blank" rel="noreferrer" className="text-link">View resume ↗</a>}
          <div className="applicant-actions">
            {applicant.status === "APPLIED" && <><button className="button button-outline button-small" onClick={() => updateStatus(applicant.applicationId, "SHORTLISTED")}>Shortlist</button><button className="button button-danger-outline button-small" onClick={() => updateStatus(applicant.applicationId, "REJECTED")}>Reject</button></>}
            {applicant.status === "SHORTLISTED" && <>
              <label className="interview-time"><span>Interview time</span><input type="datetime-local" min={new Date(Date.now() - new Date().getTimezoneOffset() * 60_000).toISOString().slice(0, 16)} value={interviewTimes[applicant.applicationId] || ""} onChange={(e) => setInterviewTimes((current) => ({ ...current, [applicant.applicationId]: e.target.value }))} /></label>
              <button className="button button-primary button-small" onClick={() => scheduleInterview(applicant.applicationId)}>Schedule interview</button>
              <button className="button button-outline button-small" onClick={() => updateStatus(applicant.applicationId, "SELECTED")}>Select candidate</button>
              <button className="button button-danger-outline button-small" onClick={() => updateStatus(applicant.applicationId, "REJECTED")}>Reject</button>
            </>}
          </div>
        </article>)}</div> : selectedJob ? <EmptyState title="No applicants yet">Student applications will appear here after they apply.</EmptyState> : <EmptyState title="Choose a job to review">Select one of your roles above to see its applicants.</EmptyState>}
      </section>
    </section>
  );
}
