import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import { ErrorMessage } from "../components/Feedback.jsx";
import { getErrorMessage } from "../services/api.js";

export default function RegisterPage() {
  const { user, register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: "", email: "", password: "", role: "STUDENT" });
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  if (user) return <Navigate to="/dashboard" replace />;

  async function submit(event) {
    event.preventDefault();
    setSubmitting(true);
    setError("");
    try {
      await register(form);
      navigate("/dashboard", { replace: true });
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="auth-page">
      <div className="auth-aside auth-aside-green"><span className="eyebrow">MAKE YOUR MOVE</span><h1>Meet the future<br />you've been working for.</h1><p>Build your profile, explore openings, and bring your next opportunity closer.</p><div className="auth-aside-shape">✳</div></div>
      <div className="auth-form-wrap"><form className="form-card" onSubmit={submit}>
        <span className="eyebrow">GET STARTED</span><h2>Create your account</h2><p className="muted">Join your campus placement community.</p>
        <ErrorMessage>{error}</ErrorMessage>
        <label className="field"><span>Full name</span><input required maxLength="100" autoComplete="name" value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} placeholder="Your full name" /></label>
        <label className="field"><span>Email address</span><input required type="email" maxLength="190" autoComplete="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} placeholder="you@college.edu" /></label>
        <label className="field"><span>Password <small>At least 8 characters</small></span><input required type="password" minLength="8" maxLength="72" autoComplete="new-password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} placeholder="Create a strong password" /></label>
        <label className="field"><span>I am joining as</span><select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}><option value="STUDENT">Student</option><option value="RECRUITER">Recruiter</option></select></label>
        <p className="form-hint">Placement officer accounts are created securely by the placement team.</p>
        <button className="button button-primary button-wide" disabled={submitting}>{submitting ? "Creating account…" : "Create account"} <span aria-hidden="true">→</span></button>
        <p className="auth-switch">Already have an account? <Link to="/login">Sign in</Link></p>
      </form></div>
    </section>
  );
}
