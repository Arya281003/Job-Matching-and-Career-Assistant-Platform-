import { useEffect, useMemo, useState } from "react";
import { getProfile, loginUser, matchResume, registerUser, upsertProfile } from "./api";

export default function App() {
  const [authTab, setAuthTab] = useState("login"); // "login" | "register"
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [fullName, setFullName] = useState("");
  const [user, setUser] = useState(null);
  const [activePage, setActivePage] = useState("match"); // "profile" | "match" | "results"
  const [profile, setProfile] = useState(null);
  const [profileLoading, setProfileLoading] = useState(false);
  const [profileForm, setProfileForm] = useState({
    education: "",
    skills: "",
    experience: "",
    careerPreferences: ""
  });

  const [resumeFile, setResumeFile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [result, setResult] = useState(null);

  const canSubmitAuth = useMemo(() => {
    if (authTab === "register") return email && password && fullName;
    return email && password;
  }, [authTab, email, password, fullName]);

  const emptyProfileForm = useMemo(
    () => ({
      education: "",
      skills: "",
      experience: "",
      careerPreferences: ""
    }),
    []
  );

  async function handleAuth(e) {
    e.preventDefault();
    setError("");
    setResult(null);

    try {
      setLoading(true);
      if (authTab === "register") {
        const u = await registerUser({ email, password, fullName });
        setUser(u);
      } else {
        const u = await loginUser({ email, password });
        setUser(u);
      }
      setActivePage("match");
    } catch (err) {
      setError(err.message || "Auth failed");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    async function loadProfile() {
      if (!user?.id) return;
      setProfileLoading(true);
      setProfile(null);
      try {
        const p = await getProfile(user.id);
        setProfile(p);
        if (p) {
          setProfileForm({
            education: p.education || "",
            skills: p.skills || "",
            experience: p.experience || "",
            careerPreferences: p.careerPreferences || ""
          });
        } else {
          setProfileForm(emptyProfileForm);
        }
      } catch (err) {
        setError(err.message || "Failed to load profile");
      } finally {
        setProfileLoading(false);
      }
    }

    loadProfile();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user]);

  function handleLogout() {
    setUser(null);
    setProfile(null);
    setResult(null);
    setAuthTab("login");
    setEmail("");
    setPassword("");
    setFullName("");
    setProfileForm(emptyProfileForm);
    setActivePage("match");
    setError("");
  }

  async function handleSaveProfile(e) {
    e.preventDefault();
    setError("");
    setResult(null);
    try {
      setLoading(true);
      const saved = await upsertProfile({
        userId: user.id,
        education: profileForm.education,
        skills: profileForm.skills,
        experience: profileForm.experience,
        careerPreferences: profileForm.careerPreferences
      });
      setProfile(saved);
    } catch (err) {
      setError(err.message || "Profile save failed");
    } finally {
      setLoading(false);
    }
  }

  async function handleMatch(e) {
    e.preventDefault();
    setError("");
    setResult(null);

    if (!resumeFile) {
      setError("Please upload a resume file (PDF/DOCX).");
      return;
    }

    try {
      setLoading(true);
      const res = await matchResume(resumeFile);
      setResult(res);
      setActivePage("results");
    } catch (err) {
      setError(err.message || "Matching failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="appShell">
      {loading ? (
        <div className="loadingOverlay" aria-label="Loading">
          <div className="spinner" />
        </div>
      ) : null}

      {!user ? (
        <div className="loginWrap">
          <div className="loginHero">
            <div className="brand" style={{ marginBottom: 10 }}>
              <div className="logo" style={{ width: 52, height: 52, borderRadius: 18 }}>
                AI
              </div>
              <div>
                <div style={{ fontSize: 28, fontWeight: 1000, letterSpacing: "-0.4px" }}>
                  AI Job Matching
                </div>
                <div className="subtitle" style={{ fontSize: 13 }}>
                  SaaS dashboard for resumes, skill gaps, and interview prep.
                </div>
              </div>
            </div>
            <div className="heroPoints">
              <div className="heroPoint">
                <div className="heroIcon">01</div>
                <div>
                  <b>Semantic matching</b>
                  <div className="muted" style={{ marginTop: 4 }}>
                    Resume vs job descriptions using embeddings.
                  </div>
                </div>
              </div>
              <div className="heroPoint">
                <div className="heroIcon">02</div>
                <div>
                  <b>Skill gap analysis</b>
                  <div className="muted" style={{ marginTop: 4 }}>
                    Identify missing skills and generate learning suggestions.
                  </div>
                </div>
              </div>
              <div className="heroPoint">
                <div className="heroIcon">03</div>
                <div>
                  <b>Interview readiness</b>
                  <div className="muted" style={{ marginTop: 4 }}>
                    Role-based questions to help you prepare faster.
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div className="card" style={{ padding: 18 }}>
            <div className="row" style={{ marginBottom: 12 }}>
              <button
                className={`btn ${authTab === "login" ? "primaryBtn" : "secondaryBtn"}`}
                type="button"
                onClick={() => setAuthTab("login")}
                disabled={loading}
              >
                Login
              </button>
              <button
                className={`btn ${authTab === "register" ? "primaryBtn" : "secondaryBtn"}`}
                type="button"
                onClick={() => setAuthTab("register")}
                disabled={loading}
              >
                Register
              </button>
            </div>

            <form onSubmit={handleAuth} className="section">
              {authTab === "register" ? (
                <div className="field">
                  <label>Full Name</label>
                  <input
                    className="input"
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    placeholder="e.g., Arya Gupta"
                  />
                </div>
              ) : null}

              <div className="field">
                <label>Email</label>
                <input
                  className="input"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  type="email"
                  placeholder="you@example.com"
                />
              </div>

              <div className="field">
                <label>Password</label>
                <input
                  className="input"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  type="password"
                  placeholder="••••••••"
                />
              </div>

              <button className="btn primaryBtn" type="submit" disabled={!canSubmitAuth || loading}>
                {loading ? "Please wait..." : authTab === "register" ? "Create Account" : "Login"}
              </button>
            </form>

            {error ? (
              <div className="error" role="alert">
                {error}
              </div>
            ) : null}

            <div className="muted" style={{ marginTop: 12, fontSize: 12, lineHeight: 1.6 }}>
              Demo note: Profile and matching work even without MySQL/Mongo depending on backend availability.
            </div>
          </div>
        </div>
      ) : (
        <div className="dashboardLayout">
          <aside className="sidebar">
            <div className="sidebarTop">
              <div className="logoSmall">AI</div>
              <div>
                <div className="sidebarBrand">JobMatch</div>
                <div className="muted" style={{ fontSize: 12, marginTop: 2 }}>
                  Dashboard
                </div>
              </div>
            </div>

            <nav className="nav">
              <button
                className={`navItem ${activePage === "profile" ? "active" : ""}`}
                onClick={() => setActivePage("profile")}
                type="button"
              >
                Profile
              </button>
              <button
                className={`navItem ${activePage === "match" ? "active" : ""}`}
                onClick={() => setActivePage("match")}
                type="button"
              >
                Resume Matching
              </button>
              <button
                className={`navItem ${activePage === "results" ? "active" : ""}`}
                onClick={() => setActivePage("results")}
                type="button"
              >
                Results
              </button>
            </nav>

            <div className="sidebarFooter">
              Upload your resume, get top roles, and receive skill gaps + learning suggestions in one place.
            </div>
          </aside>

          <div className="dashboardMain">
            <header className="dashboardHeader">
              <div>
                <h2 className="dashboardTitle">
                  {activePage === "profile"
                    ? "User Profile"
                    : activePage === "match"
                      ? "Resume Matching"
                      : "Matching Results"}
                </h2>
                <div className="dashboardSubtitle">
                  {activePage === "profile"
                    ? "Education, skills, and career preferences."
                    : activePage === "match"
                      ? "Upload a resume (PDF/DOCX) to get role recommendations."
                      : "Top matches, skill gaps, learning suggestions, and interview prep."}
                </div>
              </div>

              <div className="headerUser">
                <div className="userPill">
                  {user.fullName} ({user.email})
                </div>
                <button className="btn secondaryBtn" type="button" onClick={handleLogout}>
                  Logout
                </button>
              </div>
            </header>

            <main className="dashboardContent">
              <div className="fadeInUp">
                {error ? (
                  <div className="error" role="alert">
                    {error}
                  </div>
                ) : null}

                {activePage === "profile" ? (
                  <div className="card">
                    <div className="sectionTitle" style={{ marginBottom: 6 }}>
                      Profile Details
                    </div>

                    <form onSubmit={handleSaveProfile}>
                      <div className="grid2">
                        <div className="field">
                          <label>Education</label>
                          <textarea
                            className="textarea"
                            value={profileForm.education}
                            onChange={(e) =>
                              setProfileForm((p) => ({ ...p, education: e.target.value }))
                            }
                            rows={3}
                            placeholder="e.g., B.Tech in CSE, 2022-2026"
                          />
                        </div>

                        <div className="field">
                          <label>Skills (comma-separated)</label>
                          <textarea
                            className="textarea"
                            value={profileForm.skills}
                            onChange={(e) =>
                              setProfileForm((p) => ({ ...p, skills: e.target.value }))
                            }
                            rows={3}
                            placeholder="e.g., Java, Spring Boot, React, MySQL"
                          />
                        </div>

                        <div className="field">
                          <label>Experience</label>
                          <textarea
                            className="textarea"
                            value={profileForm.experience}
                            onChange={(e) =>
                              setProfileForm((p) => ({ ...p, experience: e.target.value }))
                            }
                            rows={3}
                            placeholder="e.g., Internship / projects / roles"
                          />
                        </div>

                        <div className="field">
                          <label>Career Preferences</label>
                          <textarea
                            className="textarea"
                            value={profileForm.careerPreferences}
                            onChange={(e) =>
                              setProfileForm((p) => ({ ...p, careerPreferences: e.target.value }))
                            }
                            rows={2}
                            placeholder="e.g., Backend Developer, NLP Engineer"
                          />
                        </div>
                      </div>

                      <button className="btn primaryBtn" type="submit" disabled={loading || profileLoading}>
                        {loading ? "Saving..." : profileLoading ? "Saving..." : "Save Profile"}
                      </button>
                    </form>
                  </div>
                ) : null}

                {activePage === "match" ? (
                  <div className="card">
                    <div className="sectionTitle" style={{ marginBottom: 6 }}>
                      Upload Resume
                    </div>
                    <form onSubmit={handleMatch}>
                      <div className="field">
                        <label>Resume (PDF/DOCX)</label>
                        <input
                          className="input"
                          type="file"
                          accept=".pdf,.docx"
                          onChange={(e) => setResumeFile(e.target.files?.[0] || null)}
                        />
                      </div>

                      <button className="btn primaryBtn" type="submit" disabled={!resumeFile || loading}>
                        {loading ? "Matching..." : "Match Jobs"}
                      </button>
                    </form>

                    <div className="infoBanner">
                      <div className="infoDot" />
                      <div>
                        Your resume will be parsed for skills. Matching uses semantic similarity (embeddings) and returns skill gaps + career guidance.
                      </div>
                    </div>
                  </div>
                ) : null}

                {activePage === "results" ? (
                  <div className="card">
                    <div className="sectionTitle" style={{ marginBottom: 6 }}>
                      Results Summary
                    </div>

                    {!result ? (
                      <div className="muted">No results yet. Go to “Resume Matching” and upload a resume.</div>
                    ) : (
                      <div>
                        <div className="section">
                          <h2 className="sectionTitle">Top Matches</h2>
                          <div className="matchGrid">
                            {result.matches?.map((m) => (
                              <div key={m.title} className="matchCard">
                                <div className="matchTitle">{m.title}</div>
                                <div className="scorePill">Score: {m.score.toFixed(4)}</div>
                              </div>
                            ))}
                          </div>
                        </div>

                        <div className="section">
                          <h2 className="sectionTitle">Extracted Skills</h2>
                          {result.extractedSkills?.length ? (
                            <div className="chipWrap">
                              {result.extractedSkills.map((s) => (
                                <span key={s} className="chip">
                                  {s}
                                </span>
                              ))}
                            </div>
                          ) : (
                            <div className="muted">No skills extracted.</div>
                          )}
                        </div>

                        <div className="section">
                          <h2 className="sectionTitle">Skill Gaps</h2>
                          {result.skillGaps?.length ? (
                            <div className="chipWrap">
                              {result.skillGaps.map((g) => (
                                <span key={g} className="chip chipNeutral">
                                  {g}
                                </span>
                              ))}
                            </div>
                          ) : (
                            <div className="muted">No major gaps found in the extracted skills.</div>
                          )}
                        </div>

                        <div className="section">
                          <h2 className="sectionTitle">Learning Suggestions</h2>
                          {result.learningSuggestions?.length ? (
                            <ul className="list">
                              {result.learningSuggestions.map((s, i) => (
                                <li key={i}>{s}</li>
                              ))}
                            </ul>
                          ) : (
                            <div className="muted">No suggestions available.</div>
                          )}
                        </div>

                        {result.careerRecommendations?.length ? (
                          <div className="section">
                            <h2 className="sectionTitle">Career Recommendations</h2>
                            <ul className="list">
                              {result.careerRecommendations.map((s, i) => (
                                <li key={i}>{s}</li>
                              ))}
                            </ul>
                          </div>
                        ) : null}

                        {result.interviewQuestions?.length ? (
                          <div className="section">
                            <h2 className="sectionTitle">Interview Preparation</h2>
                            <ul className="list">
                              {result.interviewQuestions.map((q, i) => (
                                <li key={i}>{q}</li>
                              ))}
                            </ul>
                          </div>
                        ) : null}
                      </div>
                    )}
                  </div>
                ) : null}
              </div>
            </main>
          </div>
        </div>
      )}
    </div>
  );
}

