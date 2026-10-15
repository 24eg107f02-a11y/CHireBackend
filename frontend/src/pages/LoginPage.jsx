import { useState } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import { ErrorMessage } from "../components/Feedback.jsx";
import { getErrorMessage } from "../services/api.js";

export default function LoginPage() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  if (user) return <Navigate to="/dashboard" replace />;

  async function submit(event) {
    event.preventDefault();
    setSubmitting(true);
    setError("");
    try {
      await login(form);
      navigate(location.state?.from || "/dashboard", { replace: true });
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="auth-page">
      <div className="auth-aside"><span className="eyebrow">WELCOME BACK</span><h1>Your next step<br />is waiting.</h1><p>Pick up your placement journey right where you left off.</p><div className="auth-aside-shape">✦</div></div>
      <div className="auth-form-wrap"><form className="form-card" onSubmit={submit}>
        <span className="eyebrow">CAMPUSHIRE ACCOUNT</span><h2>Sign in</h2><p className="muted">Use the email and password you registered with.</p>
        <ErrorMessage>{error}</ErrorMessage>
        <label className="field"><span>Email address</span><input required type="email" autoComplete="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} placeholder="you@college.edu" /></label>
        <label className="field"><span>Password</span><input required type="password" autoComplete="current-password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} placeholder="Enter your password" /></label>
        <button className="button button-primary button-wide" disabled={submitting}>{submitting ? "Signing in…" : "Sign in"} <span aria-hidden="true">→</span></button>
        <p className="auth-switch">New to CampusHire? <Link to="/register">Create an account</Link></p>
      </form></div>
    </section>
  );
}
