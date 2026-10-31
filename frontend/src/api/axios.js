/**
 * Centralised Axios API client for Brightloop frontend.
 *
 * - Base URL: http://localhost:8080/api
 * - Request interceptor: attaches JWT Bearer token from localStorage
 * - Response interceptor: handles 401 (auto logout) and 403 responses
 *
 * Usage: import api from './api/axios'
 *        api.get('/products')
 *        api.post('/auth/login', { email, password })
 */

import axios from "axios";

const BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

const api = axios.create({
  baseURL: BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 15000, // 15s timeout — prevents hanging requests
});

// ── Request interceptor ──────────────────────────────────────────────────────
// Attach JWT from localStorage to every request's Authorization header.
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("bl_token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// ── Response interceptor ─────────────────────────────────────────────────────
// 401: token expired or invalid → clear storage and reload to login screen.
// 403: forbidden → let the caller handle it.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem("bl_token");
      localStorage.removeItem("bl_role");
      localStorage.removeItem("bl_user");
      // Reload page — App.jsx reads localStorage on mount and shows Auth screen
      window.location.reload();
    }
    return Promise.reject(error);
  }
);

// ── Auth helpers ─────────────────────────────────────────────────────────────

/** Persist login session to localStorage. */
export function saveSession(authResponse) {
  localStorage.setItem("bl_token", authResponse.token);
  localStorage.setItem("bl_role", authResponse.role.toLowerCase());
  localStorage.setItem(
    "bl_user",
    JSON.stringify({
      id: authResponse.userId,
      fullName: authResponse.fullName,
      email: authResponse.email,
      role: authResponse.role,
    })
  );
}

/** Clear login session from localStorage. */
export function clearSession() {
  localStorage.removeItem("bl_token");
  localStorage.removeItem("bl_role");
  localStorage.removeItem("bl_user");
}

/** Returns the stored user role string ("admin" | "staff" | "customer") or null. */
export function getStoredRole() {
  return localStorage.getItem("bl_role");
}

/** Returns the stored user object or null. */
export function getStoredUser() {
  const raw = localStorage.getItem("bl_user");
  try {
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export default api;
