import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import api from "../services/api.js";

const AuthContext = createContext(null);

function readStoredUser() {
  try {
    const value = localStorage.getItem("campushire-user");
    return value ? JSON.parse(value) : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readStoredUser);
  const [loading, setLoading] = useState(Boolean(localStorage.getItem("campushire-token")));

  const saveSession = useCallback((result) => {
    localStorage.setItem("campushire-token", result.accessToken);
    const nextUser = {
      id: result.userId,
      fullName: result.fullName,
      email: result.email,
      role: result.role,
    };
    localStorage.setItem("campushire-user", JSON.stringify(nextUser));
    setUser(nextUser);
    return nextUser;
  }, []);

  const login = useCallback(async (credentials) => {
    const { data } = await api.post("/api/auth/login", credentials);
    return saveSession(data);
  }, [saveSession]);

  const register = useCallback(async (details) => {
    const { data } = await api.post("/api/auth/register", details);
    return saveSession(data);
  }, [saveSession]);

  const logout = useCallback(() => {
    localStorage.removeItem("campushire-token");
    localStorage.removeItem("campushire-user");
    setUser(null);
  }, []);

  useEffect(() => {
    const token = localStorage.getItem("campushire-token");
    if (!token) {
      setLoading(false);
      return undefined;
    }
    let active = true;
    api.get("/api/auth/me")
      .then(({ data }) => {
        if (active) {
          localStorage.setItem("campushire-user", JSON.stringify(data));
          setUser(data);
        }
      })
      .catch(() => {
        if (active) logout();
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, [logout]);

  useEffect(() => {
    window.addEventListener("campushire:unauthorized", logout);
    return () => window.removeEventListener("campushire:unauthorized", logout);
  }, [logout]);

  const value = useMemo(() => ({ user, loading, login, register, logout }), [
    user, loading, login, register, logout,
  ]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within AuthProvider");
  return context;
}
