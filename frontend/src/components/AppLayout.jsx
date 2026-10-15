import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";

const roleNames = {
  STUDENT: "Student workspace",
  RECRUITER: "Recruiter workspace",
  PLACEMENT_OFFICER: "Placement office",
};

export default function AppLayout() {
  const { user, logout } = useAuth();
  const items = [
    { to: "/", label: "Explore opportunities", end: true },
    ...(user ? [{ to: "/dashboard", label: "My dashboard" }] : []),
  ];

  return (
    <div className="app-shell">
      <header className="topbar">
        <NavLink to="/" className="brand" aria-label="CampusHire home">
          <span className="brand-mark">C</span>
          <span>campus<span className="brand-light">hire</span></span>
        </NavLink>
        <nav className="main-nav" aria-label="Main navigation">
          {items.map((item) => (
            <NavLink key={item.to} to={item.to} end={item.end} className={({ isActive }) => isActive ? "nav-link active" : "nav-link"}>
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="account-area">
          {user ? (
            <>
              <div className="account-copy">
                <strong>{user.fullName}</strong>
                <span>{roleNames[user.role] || user.role}</span>
              </div>
              <button className="button button-quiet button-small" onClick={logout}>Sign out</button>
            </>
          ) : <NavLink className="button button-primary button-small" to="/login">Sign in</NavLink>}
        </div>
      </header>
      <main className="main-content"><Outlet /></main>
      <footer className="footer">
        <span>CampusHire</span><span>Connecting campus talent with great teams.</span>
      </footer>
    </div>
  );
}
