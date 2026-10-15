import { useCallback, useEffect, useState } from "react";
import api, { getErrorMessage } from "../services/api.js";
import { ErrorMessage, EmptyState, Loading, SuccessMessage } from "../components/Feedback.jsx";
import StatusBadge from "../components/StatusBadge.jsx";

const today = new Date().toISOString().slice(0, 10);
const initialDrive = { name: "", description: "", startDate: "", endDate: "", venue: "" };

export default function OfficerDashboard() {
  const [dashboard, setDashboard] = useState(null);
  const [jobs, setJobs] = useState([]);
  const [users, setUsers] = useState([]);
  const [drives, setDrives] = useState([]);
  const [report, setReport] = useState(null);
  const [roleFilter, setRoleFilter] = useState("");
  const [drive, setDrive] = useState(initialDrive);
  const [editingDriveId, setEditingDriveId] = useState(null);
  const [reviewNotes, setReviewNotes] = useState({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const params = roleFilter ? { role: roleFilter } : {};
      const [dashboardResult, jobsResult, usersResult, drivesResult, reportResult] = await Promise.all([
        api.get("/api/officer/dashboard"),
        api.get("/api/officer/jobs"),
        api.get("/api/officer/users", { params }),
        api.get("/api/officer/drives"),
        api.get("/api/officer/reports/placements"),
      ]);
      setDashboard(dashboardResult.data);
      setJobs(jobsResult.data);
      setUsers(usersResult.data);
      setDrives(drivesResult.data);
      setReport(reportResult.data);
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, [roleFilter]);

  useEffect(() => { load(); }, [load]);

  async function reviewJob(jobId, decision) {
    setError("");
    setSuccess("");
    try {
      const path = decision === "approve" ? "approve" : "reject";
      await api.post(`/api/officer/jobs/${jobId}/${path}`, { notes: reviewNotes[jobId] || "" });
      setSuccess(`Job ${decision === "approve" ? "approved" : "rejected"}.`);
      await load();
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    }
  }

  async function changeUserStatus(user) {
    setError("");
    setSuccess("");
    try {
      await api.patch(`/api/officer/users/${user.id}/status`, { enabled: !user.enabled });
      setSuccess(`${user.fullName}'s account ${user.enabled ? "disabled" : "enabled"}.`);
      await load();
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    }
  }

  function editDrive(item) {
    setEditingDriveId(item.id);
    setDrive({
      name: item.name || "",
      description: item.description || "",
      startDate: item.startDate || "",
      endDate: item.endDate || "",
      venue: item.venue || "",
    });
  }

  function cancelDriveEdit() {
    setEditingDriveId(null);
    setDrive(initialDrive);
  }

  async function saveDrive(event) {
    event.preventDefault();
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      if (editingDriveId) {
        await api.put(`/api/officer/drives/${editingDriveId}`, drive);
      } else {
        await api.post("/api/officer/drives", drive);
      }
      const wasEditing = Boolean(editingDriveId);
      setEditingDriveId(null);
      setDrive(initialDrive);
      setSuccess(wasEditing ? "Placement drive updated." : "Placement drive created.");
      await load();
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setSaving(false);
    }
  }

  async function removeDrive(id) {
    setError("");
    setSuccess("");
    try {
      await api.delete(`/api/officer/drives/${id}`);
      setSuccess("Placement drive deleted.");
      await load();
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    }
  }

  if (loading) return <Loading label="Loading placement office" />;

  const statistics = [
    ["Students", dashboard?.students, "◉"],
    ["Recruiters", dashboard?.recruiters, "◎"],
    ["Companies", dashboard?.companies, "◈"],
    ["Pending jobs", dashboard?.pendingJobs, "◷"],
    ["Approved jobs", dashboard?.approvedJobs, "✓"],
    ["Applications", dashboard?.applications, "↗"],
    ["Selected", dashboard?.selectedApplications, "✦"],
    ["Placement drives", dashboard?.placementDrives, "▦"],
  ];

  return (
    <section className="dashboard-page">
      <div className="dashboard-welcome">
        <div><span className="eyebrow">PLACEMENT OFFICE</span><h1>Campus, connected.</h1><p>Keep opportunities moving and your placement community in sync.</p></div>
        <div className="welcome-mark">✳</div>
      </div>
      <ErrorMessage>{error}</ErrorMessage><SuccessMessage>{success}</SuccessMessage>
      <div className="officer-stat-grid">{statistics.map(([label, value, icon]) => <div className="officer-stat" key={label}><span className="stat-icon">{icon}</span><span>{label}</span><strong>{value ?? 0}</strong></div>)}</div>
      <div className="officer-columns">
        <section className="panel">
          <div className="panel-heading"><div><span className="eyebrow">REVIEW QUEUE</span><h2>Jobs awaiting approval</h2></div><span className="stat-number">{jobs.length}</span></div>
          {jobs.length ? <div className="review-list">{jobs.map(({ job: item, reviewNotes: notes }) => <article className="review-card" key={item.id}>
            <div className="review-heading"><div><h3>{item.title}</h3><p>{item.companyName} · {item.location || "Location not specified"}</p></div><StatusBadge status={item.status} /></div>
            <p className="review-description">{item.description}</p>
            <div className="job-meta"><span>Branch: {item.eligibleBranch || "All"}</span><span>Minimum CGPA: {item.minimumCgpa ?? "Not set"}</span><span>Deadline: {item.applicationDeadline}</span></div>
            {notes && <p className="muted">Previous review: {notes}</p>}
            {item.status === "PENDING_APPROVAL" && <><label className="field"><span>Review note <small>Optional</small></span><input value={reviewNotes[item.id] || ""} maxLength="1000" onChange={(e) => setReviewNotes((current) => ({ ...current, [item.id]: e.target.value }))} placeholder="Add context for the recruiter" /></label>
              <div className="applicant-actions"><button className="button button-primary button-small" onClick={() => reviewJob(item.id, "approve")}>Approve job</button><button className="button button-danger-outline button-small" onClick={() => reviewJob(item.id, "reject")}>Reject job</button></div></>}
          </article>)}</div> : <EmptyState title="The review queue is clear">Jobs submitted by recruiters will be listed here.</EmptyState>}
        </section>
        <form className="panel drive-panel" onSubmit={saveDrive}>
          <div className="panel-heading"><div><span className="eyebrow">CAMPUS CALENDAR</span><h2>Placement drives</h2></div><span className="panel-icon">▦</span></div>
          <div className="form-grid">
            <label className="field field-span"><span>Drive name</span><input required maxLength="150" value={drive.name} onChange={(e) => setDrive({ ...drive, name: e.target.value })} placeholder="Autumn Placement Week" /></label>
            <label className="field field-span"><span>Description</span><textarea maxLength="1000" rows="2" value={drive.description} onChange={(e) => setDrive({ ...drive, description: e.target.value })} /></label>
            <label className="field"><span>Start date</span><input required type="date" min={editingDriveId ? undefined : today} value={drive.startDate} onChange={(e) => setDrive({ ...drive, startDate: e.target.value })} /></label>
            <label className="field"><span>End date</span><input required type="date" min={drive.startDate || (editingDriveId ? undefined : today)} value={drive.endDate} onChange={(e) => setDrive({ ...drive, endDate: e.target.value })} /></label>
            <label className="field field-span"><span>Venue</span><input maxLength="120" value={drive.venue} onChange={(e) => setDrive({ ...drive, venue: e.target.value })} placeholder="Main campus" /></label>
          </div>
          <div className="applicant-actions"><button className="button button-primary" disabled={saving}>{saving ? "Saving…" : editingDriveId ? "Save drive" : "Create placement drive"}</button>{editingDriveId && <button className="button button-quiet" type="button" onClick={cancelDriveEdit}>Cancel</button>}</div>
          <div className="drive-list">{drives.map((item) => <div className="drive-item" key={item.id}><div><strong>{item.name}</strong><span>{item.startDate} — {item.endDate}{item.venue ? ` · ${item.venue}` : ""}</span><button className="text-button" type="button" onClick={() => editDrive(item)}>Edit</button></div><button type="button" className="icon-button" aria-label={`Delete ${item.name}`} title="Delete drive" onClick={() => removeDrive(item.id)}>×</button></div>)}</div>
          {!drives.length && <p className="muted">No placement drives scheduled.</p>}
        </form>
      </div>
      <section className="panel user-panel">
        <div className="panel-heading"><div><span className="eyebrow">CAMPUS COMMUNITY</span><h2>Manage users</h2></div><select aria-label="Filter users by role" value={roleFilter} onChange={(e) => setRoleFilter(e.target.value)}><option value="">All roles</option><option value="STUDENT">Students</option><option value="RECRUITER">Recruiters</option><option value="PLACEMENT_OFFICER">Placement officers</option></select></div>
        {users.length ? <div className="table-wrap"><table><thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Joined</th><th>Status</th><th>Action</th></tr></thead><tbody>{users.map((item) => <tr key={item.id}><td><strong>{item.fullName}</strong></td><td>{item.email}</td><td><span className="role-pill">{item.role.replaceAll("_", " ")}</span></td><td>{new Date(item.createdAt).toLocaleDateString()}</td><td><span className={`user-status ${item.enabled ? "enabled" : "disabled"}`}>{item.enabled ? "Enabled" : "Disabled"}</span></td><td><button className="text-button" onClick={() => changeUserStatus(item)}>{item.enabled ? "Disable" : "Enable"}</button></td></tr>)}</tbody></table></div> : <EmptyState title="No accounts in this view">Registered users will appear here.</EmptyState>}
      </section>
      <section className="panel report-panel">
        <div className="panel-heading"><div><span className="eyebrow">PLACEMENT OUTCOMES</span><h2>Placement report</h2></div><span className="stat-number">{report?.selectedStudents || 0}</span></div>
        <p className="muted">Students selected by company, based on recruiter-reported application outcomes.</p>
        {report?.placementsByCompany?.length ? <div className="report-grid">{report.placementsByCompany.map((row) => <div className="report-company" key={row.companyId}><div className="company-avatar">{row.companyName.slice(0, 1)}</div><div><strong>{row.companyName}</strong><span>Selected students</span></div><b>{row.selectedStudents}</b></div>)}</div> : <EmptyState title="No selections reported yet">Selected applications will be included in this report.</EmptyState>}
      </section>
    </section>
  );
}
