import { Navigate, Route, Routes } from "react-router-dom";
import AppLayout from "./components/AppLayout.jsx";
import ProtectedRoute from "./components/ProtectedRoute.jsx";
import RoleDashboard from "./components/RoleDashboard.jsx";
import AccessDeniedPage from "./pages/AccessDeniedPage.jsx";
import JobDetailPage from "./pages/JobDetailPage.jsx";
import LandingPage from "./pages/LandingPage.jsx";
import LoginPage from "./pages/LoginPage.jsx";
import NotFoundPage from "./pages/NotFoundPage.jsx";
import RegisterPage from "./pages/RegisterPage.jsx";
import { useAuth } from "./context/AuthContext.jsx";
import { Loading } from "./components/Feedback.jsx";

function DashboardRedirect() {
  const { loading, user } = useAuth();
  if (loading) return <Loading label="Restoring your session" />;
  return user ? <RoleDashboard /> : <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<LandingPage />} />
        <Route path="jobs/:id" element={<JobDetailPage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        <Route path="dashboard" element={<DashboardRedirect />} />
        <Route element={<ProtectedRoute roles={["STUDENT"]} />}>
          <Route path="student/*" element={<Navigate to="/dashboard" replace />} />
        </Route>
        <Route element={<ProtectedRoute roles={["RECRUITER"]} />}>
          <Route path="recruiter/*" element={<Navigate to="/dashboard" replace />} />
        </Route>
        <Route element={<ProtectedRoute roles={["PLACEMENT_OFFICER"]} />}>
          <Route path="officer/*" element={<Navigate to="/dashboard" replace />} />
        </Route>
        <Route path="access-denied" element={<AccessDeniedPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
