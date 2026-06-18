const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

let authToken = "";

export function setAuthToken(token) {
  authToken = token || "";
}

function authHeaders(extra = {}) {
  return authToken ? { ...extra, Authorization: `Bearer ${authToken}` } : extra;
}

async function parseResponse(response) {
  const contentType = response.headers.get("content-type") || "";
  const isJson = contentType.includes("application/json");
  const body = isJson ? await response.json() : await response.text();

  if (!response.ok) {
    const message = typeof body === "string" && body.trim()
      ? body
      : `Request failed with status ${response.status}`;
    throw new Error(message);
  }

  return body;
}

async function postJson(path, payload) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: "POST",
    headers: authHeaders({ "Content-Type": "application/json" }),
    body: JSON.stringify(payload),
  });
  return parseResponse(response);
}

async function getJson(path) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: authHeaders(),
  });
  return parseResponse(response);
}

async function postResume(path, file, userId) {
  const formData = new FormData();
  formData.append("resume", file);
  if (userId) {
    formData.append("userId", userId);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: "POST",
    headers: authHeaders(),
    body: formData,
  });
  return parseResponse(response);
}

export const api = {
  login: (payload) => postJson("/api/auth/login", payload),
  register: (payload) => postJson("/api/auth/register", payload),
  getProfile: (userId) => getJson(`/api/profile/${userId}`),
  saveProfile: (payload) => postJson("/api/profile", payload),
  matchProfile: (payload) => postJson("/api/matches/profile", payload),
  parseResume: (file) => postResume("/api/resumes/parse", file),
  matchResume: (file, userId) => postResume("/api/matches", file, userId),
  listJobs: () => getJson("/api/jobs"),
  importLiveJobs: (query = "software") => postJson(`/api/jobs/import/live?query=${encodeURIComponent(query)}`, {}),
  listAnalyses: () => getJson("/api/analyses"),
};
