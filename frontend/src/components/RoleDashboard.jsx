import { useAuth } from "../context/AuthContext.jsx";
import StudentDashboard from "../pages/StudentDashboard.jsx";
import RecruiterDashboard from "../pages/RecruiterDashboard.jsx";
import OfficerDashboard from "../pages/OfficerDashboard.jsx";

export default function RoleDashboard() {
  const { user } = useAuth();
  if (user.role === "STUDENT") return <StudentDashboard />;
  if (user.role === "RECRUITER") return <RecruiterDashboard />;
  if (user.role === "PLACEMENT_OFFICER") return <OfficerDashboard />;
  return <p className="feedback feedback-error">This account does not have a supported role.</p>;
}
