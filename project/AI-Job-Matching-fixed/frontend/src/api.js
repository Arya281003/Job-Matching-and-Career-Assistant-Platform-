const API_BASE = import.meta.env.VITE_API_BASE || "http://localhost:8080";

// ── Token helpers ──────────────────────────────────────────────────────────
export function saveToken(token) {
  sessionStorage.setItem("jm_token", token);
}
export function getToken() {
  return sessionStorage.getItem("jm_token");
}
export function clearToken() {
  sessionStorage.removeItem("jm_token");
}

function authHeaders(extra = {}) {
  const token = getToken();
  return {
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...extra,
  };
}

async function handleResponse(res) {
  const body = await res.text();
  if (!res.ok) {
    let msg = body;
    try { msg = JSON.parse(body)?.message || JSON.parse(body)?.error || body; } catch {}
    throw new Error(msg || `Request failed (${res.status})`);
  }
  return body ? JSON.parse(body) : null;
}

// ── Auth ───────────────────────────────────────────────────────────────────
export async function registerUser({ email, password, fullName }) {
  const res = await fetch(`${API_BASE}/api/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password, fullName }),
  });
  const data = await handleResponse(res);
  if (data.token) saveToken(data.token);
  return data;
}

export async function loginUser({ email, password }) {
  const res = await fetch(`${API_BASE}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });
  const data = await handleResponse(res);
  if (data.token) saveToken(data.token);
  return data;
}

// ── Resume matching ────────────────────────────────────────────────────────
export async function matchResume(file) {
  const formData = new FormData();
  formData.append("resume", file);
  const res = await fetch(`${API_BASE}/api/matches`, {
    method: "POST",
    headers: authHeaders(),
    body: formData,
  });
  return handleResponse(res);
}

// ── Profile ────────────────────────────────────────────────────────────────
export async function getProfile(userId) {
  const res = await fetch(`${API_BASE}/api/profile/${userId}`, {
    headers: authHeaders(),
  });
  if (res.status === 404) return null;
  return handleResponse(res);
}

export async function upsertProfile({ userId, education, skills, experience, careerPreferences }) {
  const res = await fetch(`${API_BASE}/api/profile`, {
    method: "POST",
    headers: authHeaders({ "Content-Type": "application/json" }),
    body: JSON.stringify({ userId, education, skills, experience, careerPreferences }),
  });
  return handleResponse(res);
}
