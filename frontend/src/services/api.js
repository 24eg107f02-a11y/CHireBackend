import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || (import.meta.env.PROD ? "/" : "http://localhost:8081"),
  headers: { "Content-Type": "application/json" },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("campushire-token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && localStorage.getItem("campushire-token")) {
      localStorage.removeItem("campushire-token");
      localStorage.removeItem("campushire-user");
      window.dispatchEvent(new Event("campushire:unauthorized"));
    }
    return Promise.reject(error);
  },
);

export function getErrorMessage(error) {
  const data = error?.response?.data;
  if (data?.detail) return data.detail;
  if (data?.message) return data.message;
  if (data?.errors) return Object.values(data.errors).flat().join(", ");
  if (error?.code === "ERR_NETWORK") {
    return "Cannot reach the CampusHire API. Check that the backend is running and try again.";
  }
  return error?.message || "Something went wrong. Please try again.";
}

export default api;
