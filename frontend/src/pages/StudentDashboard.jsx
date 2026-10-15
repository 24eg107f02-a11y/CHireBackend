import { useCallback, useEffect, useState } from "react";
import api, { getErrorMessage } from "../services/api.js";
import { useAuth } from "../context/AuthContext.jsx";
import { EmptyState, ErrorMessage, Loading, SuccessMessage } from "../components/Feedback.jsx";
import StatusBadge from "../components/StatusBadge.jsx";
import { Link } from "react-router-dom";

const blankProfile = { fullName: "", branch: "", cgpa: "", graduationYear: "", skills: "", projects: "", resumeUrl: "" };

function profileForForm(data) {
  return {
    ...blankProfile,
    ...data,
    branch: data.branch ?? "",
    cgpa: data.cgpa ?? "",
    graduationYear: data.graduationYear ?? "",
    skills: data.skills ?? "",
    projects: data.projects ?? "",
    resumeUrl: data.resumeUrl ?? "",
  };
}

export default function StudentDashboard() {
  const { user } = useAuth();
  const [profile, setProfile] = useState(blankProfile);
  const [applications, setApplications] = useState([]);
  const [interviews, setInterviews] = useState([]);
  const [recommendations, setRecommendations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [profileResult, applicationResult, interviewResult, recommendationResult] = await Promise.all([
        api.get("/api/students/profile"),
        api.get("/api/students/applications"),
        api.get("/api/students/interviews"),
        api.get("/api/students/recommendations"),
      ]);
      setProfile(profileForForm(profileResult.data));
      setApplications(applicationResult.data);
      setInterviews(interviewResult.data);
      setRecommendations(recommendationResult.data);
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  function update(event) {
    setProfile((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function save(event) {
    event.preventDefault();
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      const request = {
        ...profile,
        cgpa: profile.cgpa === "" ? null : Number(profile.cgpa),
        graduationYear: profile.graduationYear === "" ? null : Number(profile.graduationYear),
      };
      const { data } = await api.put("/api/students/profile", request);
      setProfile(profileForForm(data));
      const recommendationResult = await api.get("/api/students/recommendations");
      setRecommendations(recommendationResult.data);
      setSuccess("Your profile was saved.");
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <Loading label="Loading your placement workspace" />;

  return (
    <section className="dashboard-page">
      <div className="dashboard-welcome">
        <div><span className="eyebrow">STUDENT WORKSPACE</span><h1>Welcome back, {user.fullName.split(" ")[0]}.</h1><p>Keep your profile fresh and stay close to your next opportunity.</p></div>
        <div className="welcome-mark">✦</div>
      </div>
      <ErrorMessage>{error}</ErrorMessage><SuccessMessage>{success}</SuccessMessage>
      <div className="dashboard-grid student-grid">
        <form className="panel profile-panel" onSubmit={save}>
          <div className="panel-heading"><div><span className="eyebrow">YOUR DETAILS</span><h2>Student profile</h2></div><span className="panel-icon">◎</span></div>
          <div className="form-grid">
            <label className="field field-span"><span>Full name</span><input required name="fullName" value={profile.fullName} onChange={update} maxLength="100" /></label>
            <label className="field field-span"><span>Email</span><input value={profile.email || user.email} readOnly /></label>
            <label className="field"><span>Branch</span><input name="branch" value={profile.branch} onChange={update} maxLength="100" placeholder="Computer Science" /></label>
            <label className="field"><span>CGPA (out of 10)</span><input type="number" name="cgpa" min="0" max="10" step="0.01" value={profile.cgpa} onChange={update} /></label>
            <label className="field"><span>Graduation year</span><input type="number" name="graduationYear" min="2000" max="2100" value={profile.graduationYear} onChange={update} /></label>
            <label className="field field-span"><span>Skills <small>Separate with commas</small></span><input name="skills" value={profile.skills} onChange={update} maxLength="2000" placeholder="Java, React, SQL" /></label>
            <label className="field field-span"><span>Projects</span><textarea name="projects" value={profile.projects} onChange={update} maxLength="4000" rows="3" placeholder="A few projects you're proud of…" /></label>
            <label className="field field-span"><span>Resume URL</span><input type="url" name="resumeUrl" value={profile.resumeUrl} onChange={update} maxLength="2048" placeholder="https://…" /></label>
          </div>
          <button className="button button-primary" disabled={saving}>{saving ? "Saving…" : "Save profile"}</button>
        </form>
        <aside className="student-side">
          <div className="panel">
            <div className="panel-heading"><div><span className="eyebrow">KEEP MOVING</span><h2>Applications</h2></div><span className="stat-number">{applications.length}</span></div>
            {applications.length ? <div className="compact-list">{applications.map((application) => <div className="compact-row" key={application.id}><div><strong>{application.jobTitle}</strong><span>{application.companyName}</span></div><StatusBadge status={application.status} /></div>)}</div> : <EmptyState title="No applications yet">Browse open roles and apply when you're eligible.</EmptyState>}
          </div>
          <div className="panel">
            <div className="panel-heading"><div><span className="eyebrow">ON YOUR CALENDAR</span><h2>Interviews</h2></div><span className="panel-icon">◷</span></div>
            {interviews.length ? <div className="compact-list">{interviews.map((interview) => <div className="compact-row" key={interview.id}><div><strong>{interview.jobTitle}</strong><span>{new Date(interview.scheduledAt).toLocaleString()}</span></div></div>)}</div> : <EmptyState title="Nothing scheduled yet">Your scheduled interviews will appear here.</EmptyState>}
          </div>
          <div className="panel">
            <div className="panel-heading"><div><span className="eyebrow">A BETTER-FIT SEARCH</span><h2>Recommended for you</h2></div><span className="panel-icon">✦</span></div>
            {recommendations.length ? <div className="recommendation-list">{recommendations.map((recommendation) => <article className="recommendation-card" key={recommendation.job.id}>
              <div className="recommendation-heading"><div><h3>{recommendation.job.title}</h3><p>{recommendation.job.companyName} · {recommendation.job.location || "Location not set"}</p></div><strong>{recommendation.matchScore}% match</strong></div>
              <span className={`eligibility-label ${recommendation.eligible ? "eligible" : "ineligible"}`}>{recommendation.eligible ? "Eligible to apply" : "Not currently eligible"}</span>
              <ul>{recommendation.recommendationReasons.map((reason) => <li key={reason}>{reason}</li>)}</ul>
              {!recommendation.eligible && recommendation.eligibilityReasons.length > 0 && <p className="eligibility-reasons"><strong>Eligibility check:</strong> {recommendation.eligibilityReasons.join(" ")}</p>}
              <Link className="text-link" to={`/jobs/${recommendation.job.id}`}>Review role and requirements →</Link>
            </article>)}</div> : <EmptyState title="No open roles to recommend">Approved roles that are still accepting applications will appear here.</EmptyState>}
            <p className="form-hint">Recommendations are ranked using a simple rule-based match of branch, CGPA, and required skills. Eligibility is checked separately; this is not machine learning.</p>
          </div>
        </aside>
      </div>
    </section>
  );
}
