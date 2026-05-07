import { useEffect, useMemo, useState } from "react";
import {
  clearToken, getProfile, getToken, loginUser,
  matchResume, registerUser, upsertProfile
} from "./api";

// ── small helpers ─────────────────────────────────────────────────────────
function initials(name) {
  return (name || "?")
    .split(" ")
    .map((w) => w[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();
}

function ScoreBar({ score }) {
  // score is cosine similarity 0..1
  const pct = Math.min(100, Math.round(score * 100));
  return (
    <div>
      <div className="scoreBar">
        <div className="scoreBarFill" style={{ width: `${pct}%` }} />
      </div>
      <span className="scorePill">{pct}% match</span>
    </div>
  );
}

// ── App ───────────────────────────────────────────────────────────────────
export default function App() {
  const [authTab, setAuthTab] = useState("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [fullName, setFullName] = useState("");
  const [user, setUser] = useState(null);
  const [activePage, setActivePage] = useState("match");

  const [profile, setProfile] = useState(null);
  const [profileLoading, setProfileLoading] = useState(false);
  const emptyForm = useMemo(() => ({ education: "", skills: "", experience: "", careerPreferences: "" }), []);
  const [profileForm, setProfileForm] = useState(emptyForm);

  const [resumeFile, setResumeFile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [result, setResult] = useState(null);

  const canAuth = authTab === "register"
    ? email && password && fullName
    : email && password;

  async function handleAuth(e) {
    e.preventDefault();
    setError("");
    try {
      setLoading(true);
      const u = authTab === "register"
        ? await registerUser({ email, password, fullName })
        : await loginUser({ email, password });
      setUser(u);
      setActivePage("match");
    } catch (err) {
      setError(err.message || "Auth failed");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (!user?.id) return;
    setProfileLoading(true);
    getProfile(user.id)
      .then((p) => {
        setProfile(p);
        if (p) setProfileForm({ education: p.education || "", skills: p.skills || "", experience: p.experience || "", careerPreferences: p.careerPreferences || "" });
        else setProfileForm(emptyForm);
      })
      .catch((err) => setError(err.message))
      .finally(() => setProfileLoading(false));
  }, [user, emptyForm]);

  function handleLogout() {
    clearToken();
    setUser(null); setProfile(null); setResult(null);
    setAuthTab("login"); setEmail(""); setPassword(""); setFullName("");
    setProfileForm(emptyForm); setActivePage("match"); setError("");
  }

  async function handleSaveProfile(e) {
    e.preventDefault(); setError("");
    try {
      setLoading(true);
      const saved = await upsertProfile({ userId: user.id, ...profileForm });
      setProfile(saved);
    } catch (err) { setError(err.message); }
    finally { setLoading(false); }
  }

  async function handleMatch(e) {
    e.preventDefault(); setError("");
    if (!resumeFile) { setError("Please select a resume file (PDF or DOCX)."); return; }
    try {
      setLoading(true);
      const res = await matchResume(resumeFile);
      setResult(res);
      setActivePage("results");
    } catch (err) { setError(err.message || "Matching failed"); }
    finally { setLoading(false); }
  }

  // ── Login / Register page ────────────────────────────────────────
  if (!user) return (
    <div className="loginWrap">
      {loading && <div className="loadingOverlay"><div className="spinner" /></div>}

      {/* Hero */}
      <div className="loginHero">
        <div className="heroLogo">
          <div className="logoIcon">AI</div>
          <div>
            <div className="heroTitle">AI Job Matching</div>
            <div className="heroSub">Smart resume analysis & career guidance</div>
          </div>
        </div>
        <div className="heroPoints">
          {[
            ["01", "Semantic Matching", "Compares your resume with job descriptions using sentence embeddings — not just keywords."],
            ["02", "Skill Gap Analysis", "Highlights missing skills and generates targeted learning suggestions for each role."],
            ["03", "Interview Readiness", "Role-specific questions so you walk into every interview prepared."],
          ].map(([n, title, desc]) => (
            <div className="heroPoint" key={n}>
              <div className="heroNum">{n}</div>
              <div>
                <div className="heroPointTitle">{title}</div>
                <div className="heroPointDesc">{desc}</div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Form */}
      <div className="loginFormWrap">
        <div className="formHeading">{authTab === "login" ? "Welcome back" : "Create account"}</div>
        <div className="formSub">{authTab === "login" ? "Sign in to your dashboard" : "Get started for free"}</div>

        <div className="tabRow">
          <button className={`tabBtn ${authTab === "login" ? "active" : ""}`} onClick={() => { setAuthTab("login"); setError(""); }}>Sign In</button>
          <button className={`tabBtn ${authTab === "register" ? "active" : ""}`} onClick={() => { setAuthTab("register"); setError(""); }}>Sign Up</button>
        </div>

        <form onSubmit={handleAuth}>
          {authTab === "register" && (
            <div className="field">
              <label>Full Name</label>
              <input className="input" value={fullName} onChange={e => setFullName(e.target.value)} placeholder="Arya Gupta" />
            </div>
          )}
          <div className="field">
            <label>Email</label>
            <input className="input" type="email" value={email} onChange={e => setEmail(e.target.value)} placeholder="you@example.com" />
          </div>
          <div className="field">
            <label>Password</label>
            <input className="input" type="password" value={password} onChange={e => setPassword(e.target.value)} placeholder="••••••••" />
          </div>
          {error && <div className="error">{error}</div>}
          <button className="btn primaryBtn" type="submit" disabled={!canAuth || loading} style={{ width: "100%", marginTop: 8 }}>
            {loading ? "Please wait…" : authTab === "register" ? "Create Account" : "Sign In"}
          </button>
        </form>
        <div style={{ marginTop: 16, fontSize: 12, color: "var(--muted)", lineHeight: 1.6 }}>
          Demo: runs without MySQL/MongoDB using in-memory H2. Token stored in sessionStorage.
        </div>
      </div>
    </div>
  );

  // ── Dashboard ────────────────────────────────────────────────────
  const nav = [
    { id: "match",   label: "Resume Matching", icon: "📄" },
    { id: "results", label: "Results",          icon: "📊" },
    { id: "profile", label: "My Profile",       icon: "👤" },
  ];

  const pageTitles = {
    match:   ["Resume Matching",   "Upload a PDF or DOCX to find your best-fit roles"],
    results: ["Matching Results",  "Top roles, skill gaps, learning paths & interview prep"],
    profile: ["My Profile",        "Education, skills, experience and career preferences"],
  };

  return (
    <div className="appShell">
      {loading && <div className="loadingOverlay"><div className="spinner" /></div>}

      <div className="dashboardLayout">
        {/* Sidebar */}
        <aside className="sidebar">
          <div className="sidebarBrandRow">
            <div className="logoSmall">AI</div>
            <div>
              <div className="sidebarBrand">JobMatch</div>
              <div className="sidebarSub">AI Career Assistant</div>
            </div>
          </div>

          <div className="navSection">
            <div className="navLabel">Navigation</div>
            {nav.map(({ id, label, icon }) => (
              <button key={id} className={`navItem ${activePage === id ? "active" : ""}`} onClick={() => setActivePage(id)}>
                <span className="navIcon">{icon}</span>
                {label}
              </button>
            ))}
          </div>

          <div className="sidebarFooter">
            <p>Upload your resume → get matched roles, skill gaps and learning suggestions instantly.</p>
          </div>
        </aside>

        {/* Main */}
        <div className="dashboardMain">
          <header className="dashboardHeader">
            <div>
              <div className="pageTitle">{pageTitles[activePage][0]}</div>
              <div className="pageSub">{pageTitles[activePage][1]}</div>
            </div>
            <div className="headerRight">
              <div className="userBadge">
                <div className="userAvatar">{initials(user.fullName)}</div>
                <span style={{ fontSize: 13 }}>{user.fullName}</span>
              </div>
              <button className="btn secondaryBtn" onClick={handleLogout}>Sign Out</button>
            </div>
          </header>

          <main className="dashboardContent">
            <div className="fadeInUp">
              {error && <div className="error">{error}</div>}

              {/* ── Match Page ── */}
              {activePage === "match" && (
                <div className="card">
                  <div className="cardHeader">
                    <div className="cardIconWrap">📄</div>
                    <div>
                      <div className="cardTitle">Upload Your Resume</div>
                      <div className="cardSub">PDF or DOCX — max 10 MB</div>
                    </div>
                  </div>

                  <form onSubmit={handleMatch}>
                    <div className="uploadZone">
                      <input type="file" accept=".pdf,.docx" onChange={e => setResumeFile(e.target.files?.[0] || null)} />
                      <div className="uploadIcon">☁️</div>
                      <div className="uploadText">Drag & drop or click to browse</div>
                      <div className="uploadSub">Supports PDF and DOCX</div>
                      {resumeFile && <div className="uploadedName">✅ {resumeFile.name}</div>}
                    </div>

                    <div style={{ marginTop: 16 }}>
                      <button className="btn primaryBtn" type="submit" disabled={!resumeFile || loading}>
                        {loading ? "Analyzing…" : "🔍 Analyze & Match Jobs"}
                      </button>
                    </div>
                  </form>

                  <div className="infoBanner">
                    ℹ️ Your resume is parsed using spaCy + sentence-transformers for semantic matching — skills are extracted and compared against 150+ known technologies.
                  </div>
                </div>
              )}

              {/* ── Results Page ── */}
              {activePage === "results" && (
                <div>
                  {!result ? (
                    <div className="card">
                      <div className="emptyState">
                        <div className="emptyIcon">📊</div>
                        <p>No results yet. Go to <b>Resume Matching</b> and upload your resume first.</p>
                      </div>
                    </div>
                  ) : (
                    <>
                      {/* Top matches */}
                      <div className="card">
                        <div className="cardHeader">
                          <div className="cardIconWrap">🎯</div>
                          <div><div className="cardTitle">Top Job Matches</div><div className="cardSub">Ranked by semantic similarity</div></div>
                        </div>
                        <div className="matchGrid">
                          {(result.matches || []).map((m, i) => (
                            <div key={m.title} className="matchCard">
                              <div className="matchRank">#{i + 1} Match</div>
                              <div className="matchTitle">{m.title}</div>
                              <ScoreBar score={m.score} />
                            </div>
                          ))}
                        </div>
                      </div>

                      {/* Extracted skills */}
                      <div className="card" style={{ marginTop: 14 }}>
                        <div className="cardHeader">
                          <div className="cardIconWrap">⚡</div>
                          <div><div className="cardTitle">Extracted Skills</div><div className="cardSub">Found in your resume</div></div>
                        </div>
                        {result.extractedSkills?.length ? (
                          <div className="chipWrap">
                            {result.extractedSkills.map(s => <span key={s} className="chip">{s}</span>)}
                          </div>
                        ) : <div style={{ color: "var(--muted)", fontSize: 13 }}>No skills found. Try a more detailed resume.</div>}
                      </div>

                      {/* Skill gaps */}
                      {result.skillGaps?.length > 0 && (
                        <div className="card" style={{ marginTop: 14 }}>
                          <div className="cardHeader">
                            <div className="cardIconWrap">🔍</div>
                            <div><div className="cardTitle">Skill Gaps</div><div className="cardSub">Skills required by top matches that were not found in your resume</div></div>
                          </div>
                          <div className="chipWrap">
                            {result.skillGaps.map(g => <span key={g} className="chip chipGap">{g}</span>)}
                          </div>
                        </div>
                      )}

                      {/* Learning suggestions */}
                      {result.learningSuggestions?.length > 0 && (
                        <div className="card" style={{ marginTop: 14 }}>
                          <div className="cardHeader">
                            <div className="cardIconWrap">📚</div>
                            <div><div className="cardTitle">Learning Suggestions</div><div className="cardSub">How to close your skill gaps</div></div>
                          </div>
                          <ul className="list">
                            {result.learningSuggestions.map((s, i) => <li key={i}>{s}</li>)}
                          </ul>
                        </div>
                      )}

                      {/* Career recommendations */}
                      {result.careerRecommendations?.length > 0 && (
                        <div className="card" style={{ marginTop: 14 }}>
                          <div className="cardHeader">
                            <div className="cardIconWrap">🚀</div>
                            <div><div className="cardTitle">Career Recommendations</div></div>
                          </div>
                          <ul className="list">
                            {result.careerRecommendations.map((s, i) => <li key={i}>{s}</li>)}
                          </ul>
                        </div>
                      )}

                      {/* Interview questions */}
                      {result.interviewQuestions?.length > 0 && (
                        <div className="card" style={{ marginTop: 14 }}>
                          <div className="cardHeader">
                            <div className="cardIconWrap">🎤</div>
                            <div><div className="cardTitle">Interview Preparation</div><div className="cardSub">Questions tailored to your top matched role</div></div>
                          </div>
                          <ul className="list">
                            {result.interviewQuestions.map((q, i) => <li key={i}>{q}</li>)}
                          </ul>
                        </div>
                      )}
                    </>
                  )}
                </div>
              )}

              {/* ── Profile Page ── */}
              {activePage === "profile" && (
                <div>
                  {/* Stats summary */}
                  <div className="statRow">
                    <div className="statCard">
                      <div className="statLabel">Skills Listed</div>
                      <div className="statValue">{profileForm.skills ? profileForm.skills.split(",").filter(Boolean).length : 0}</div>
                    </div>
                    <div className="statCard">
                      <div className="statLabel">Experience</div>
                      <div className="statValue">{profileForm.experience ? "✓" : "—"}</div>
                    </div>
                    <div className="statCard">
                      <div className="statLabel">Preferences</div>
                      <div className="statValue">{profileForm.careerPreferences ? "✓" : "—"}</div>
                    </div>
                  </div>

                  <div className="card">
                    <div className="cardHeader">
                      <div className="cardIconWrap">👤</div>
                      <div><div className="cardTitle">Profile Details</div><div className="cardSub">This info may be used to personalise future matching</div></div>
                    </div>

                    <form onSubmit={handleSaveProfile}>
                      <div className="grid2">
                        <div className="field">
                          <label>Education</label>
                          <textarea className="textarea" rows={3}
                            value={profileForm.education}
                            onChange={e => setProfileForm(p => ({ ...p, education: e.target.value }))}
                            placeholder="e.g. B.Tech in CSE, 2022–2026" />
                        </div>
                        <div className="field">
                          <label>Skills (comma-separated)</label>
                          <textarea className="textarea" rows={3}
                            value={profileForm.skills}
                            onChange={e => setProfileForm(p => ({ ...p, skills: e.target.value }))}
                            placeholder="e.g. Java, React, Python, MySQL" />
                        </div>
                        <div className="field">
                          <label>Experience</label>
                          <textarea className="textarea" rows={3}
                            value={profileForm.experience}
                            onChange={e => setProfileForm(p => ({ ...p, experience: e.target.value }))}
                            placeholder="e.g. Internships, projects, part-time roles" />
                        </div>
                        <div className="field">
                          <label>Career Preferences</label>
                          <textarea className="textarea" rows={3}
                            value={profileForm.careerPreferences}
                            onChange={e => setProfileForm(p => ({ ...p, careerPreferences: e.target.value }))}
                            placeholder="e.g. Backend Developer, NLP Engineer" />
                        </div>
                      </div>

                      <button className="btn primaryBtn" type="submit" disabled={loading || profileLoading}>
                        {loading || profileLoading ? "Saving…" : "💾 Save Profile"}
                      </button>
                    </form>
                  </div>
                </div>
              )}
            </div>
          </main>
        </div>
      </div>
    </div>
  );
}
