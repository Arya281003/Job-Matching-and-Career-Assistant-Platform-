const API_BASE = import.meta.env.VITE_API_BASE || "http://localhost:8080";

export async function registerUser({ email, password, fullName }) {
  const res = await fetch(`${API_BASE}/api/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password, fullName })
  });
  const body = await res.text();
  if (!res.ok) throw new Error(body || "Registration failed");
  return JSON.parse(body);
}

export async function loginUser({ email, password }) {
  const res = await fetch(`${API_BASE}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password })
  });
  const body = await res.text();
  if (!res.ok) throw new Error(body || "Login failed");
  return JSON.parse(body);
}

export async function matchResume(file) {
  const formData = new FormData();
  formData.append("resume", file);

  const res = await fetch(`${API_BASE}/api/matches`, {
    method: "POST",
    body: formData
  });
  const body = await res.text();
  if (!res.ok) throw new Error(body || "Matching failed");
  return JSON.parse(body);
}

export async function getProfile(userId) {
  const res = await fetch(`${API_BASE}/api/profile/${userId}`);
  const body = await res.text();
  if (!res.ok) {
    if (res.status === 404) return null;
    // body may be JSON from Spring. Extract a friendlier message if possible.
    try {
      const json = JSON.parse(body);
      const msg = json?.message || json?.error || `Request failed (${res.status})`;
      throw new Error(msg);
    } catch {
      throw new Error(body || `Request failed (${res.status})`);
    }
  }
  return JSON.parse(body);
}

export async function upsertProfile({ userId, education, skills, experience, careerPreferences }) {
  const res = await fetch(`${API_BASE}/api/profile`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userId, education, skills, experience, careerPreferences })
  });
  const body = await res.text();
  if (!res.ok) {
    try {
      const json = JSON.parse(body);
      const msg = json?.message || json?.error || `Request failed (${res.status})`;
      throw new Error(msg);
    } catch {
      throw new Error(body || `Request failed (${res.status})`);
    }
  }
  return JSON.parse(body);
}

