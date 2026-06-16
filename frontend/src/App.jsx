import { useEffect, useMemo, useState } from "react";
import { api, setAuthToken } from "./api";

const sampleProfile = {
  education: "B.Tech Computer Science",
  skills: "Java, Spring Boot, React, Python, MySQL",
  experience: "Built REST APIs, responsive React dashboards, and NLP prototypes.",
  careerPreferences: "Backend Developer, Full Stack Developer, NLP Engineer",
};

function list(items) {
  return Array.isArray(items) ? items.filter(Boolean) : [];
}

function scorePercent(score) {
  const value = Number(score || 0);
  const normalized = value <= 1 ? value * 100 : value;
  return Math.max(0, Math.min(100, Math.round(normalized)));
}

function searchUrl(base, skill) {
  return `${base}${encodeURIComponent(skill)}`;
}

function learningOptionsFor(skill, suggestions) {
  const suggestionText = list(suggestions)
    .filter((item) => item.toLowerCase().includes(skill.toLowerCase()))
    .slice(0, 2);

  return [
    {
      type: "YouTube videos",
      title: `${skill} beginner tutorials`,
      detail: "Watch practical video lessons and project walkthroughs.",
      url: searchUrl("https://www.youtube.com/results?search_query=", `${skill} tutorial for beginners`),
    },
    {
      type: "Free notes",
      title: `${skill} notes and examples`,
      detail: suggestionText[0] || "Read free notes, examples, and interview-focused explanations.",
      url: searchUrl("https://www.google.com/search?q=", `${skill} free notes tutorial`),
    },
    {
      type: "EdTech platform",
      title: `${skill} structured course`,
      detail: suggestionText[1] || "Follow a structured course path with exercises and certificates.",
      url: searchUrl("https://www.coursera.org/search?query=", skill),
    },
  ];
}

function App() {
  const [page, setPage] = useState(() => {
    const saved = localStorage.getItem("jobmatch:user");
    return saved ? "upload" : "auth";
  });
  const [mode, setMode] = useState("login");
  const [authForm, setAuthForm] = useState({
    fullName: "",
    email: "",
    password: "",
  });
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem("jobmatch:user");
    const parsed = saved ? JSON.parse(saved) : null;
    setAuthToken(parsed?.token || "");
    return parsed;
  });
  const [profile, setProfile] = useState(sampleProfile);
  const [jobs, setJobs] = useState([]);
  const [analyses, setAnalyses] = useState([]);
  const [resumeFile, setResumeFile] = useState(null);
  const [parseResult, setParseResult] = useState(null);
  const [matchResult, setMatchResult] = useState(null);
  const [busy, setBusy] = useState("");
  const [notice, setNotice] = useState("");

  const topMatch = useMemo(() => list(matchResult?.matches)[0], [matchResult]);

  useEffect(() => {
    if (!user?.id) return;
    setAuthToken(user.token || "");

    api.getProfile(user.id)
      .then((savedProfile) => {
        setProfile({
          education: savedProfile.education || "",
          skills: savedProfile.skills || "",
          experience: savedProfile.experience || "",
          careerPreferences: savedProfile.careerPreferences || "",
        });
      })
      .catch(() => {
        setProfile(sampleProfile);
      });

    api.listJobs().then(setJobs).catch(() => setJobs([]));
    api.listAnalyses().then(setAnalyses).catch(() => setAnalyses([]));
  }, [user?.id]);

  async function run(label, action) {
    setBusy(label);
    setNotice("");
    try {
      await action();
    } catch (error) {
      setNotice(error.message || "Something went wrong.");
    } finally {
      setBusy("");
    }
  }

  function updateAuthForm(event) {
    const { name, value } = event.target;
    setAuthForm((current) => ({ ...current, [name]: value }));
  }

  function updateProfile(event) {
    const { name, value } = event.target;
    setProfile((current) => ({ ...current, [name]: value }));
  }

  async function submitAuth(event) {
    event.preventDefault();
    await run(mode === "login" ? "Signing in" : "Creating account", async () => {
      const payload = mode === "login"
        ? { email: authForm.email, password: authForm.password }
        : authForm;
      const signedInUser = mode === "login"
        ? await api.login(payload)
        : await api.register(payload);

      setUser(signedInUser);
      setAuthToken(signedInUser.token || "");
      localStorage.setItem("jobmatch:user", JSON.stringify(signedInUser));
      setPage("upload");
      setNotice(`Welcome, ${signedInUser.fullName}.`);
    });
  }

  async function saveProfileOnly() {
    if (!user?.id) {
      setNotice("Sign in before saving a profile.");
      setPage("auth");
      return;
    }

    const saved = await api.saveProfile({ userId: user.id, ...profile });
    setProfile({
      education: saved.education || "",
      skills: saved.skills || "",
      experience: saved.experience || "",
      careerPreferences: saved.careerPreferences || "",
    });
  }

  async function analyzeResume(event) {
    event.preventDefault();
    if (!resumeFile) {
      setNotice("Choose a PDF or DOCX resume first.");
      return;
    }

    await run("Analyzing resume", async () => {
      await saveProfileOnly();
      const parsed = await api.parseResume(resumeFile);
      const matched = await api.matchResume(resumeFile, user?.id);
      setParseResult(parsed);
      setMatchResult(matched);
      api.listAnalyses().then(setAnalyses).catch(() => {});
      setPage("results");
      setNotice("Resume analysis is ready.");
    });
  }

  async function analyzeProfileOnly() {
    if (!user?.id) {
      setNotice("Sign in before finding job roles.");
      setPage("auth");
      return;
    }

    const hasProfileInput = Object.values(profile).some((value) => String(value || "").trim());
    if (!hasProfileInput) {
      setNotice("Fill the candidate profile before finding job roles.");
      return;
    }

    await run("Finding job roles", async () => {
      const matched = await api.matchProfile({ userId: user.id, ...profile });
      setParseResult(null);
      setMatchResult(matched);
      api.listAnalyses().then(setAnalyses).catch(() => {});
      setPage("results");
      setNotice("Profile-based job analysis is ready.");
    });
  }

  async function refreshLiveJobs() {
    await run("Refreshing live jobs", async () => {
      const result = await api.importLiveJobs("software");
      const latestJobs = await api.listJobs();
      setJobs(latestJobs);
      setNotice(`Imported ${result.saved} live jobs from ${result.source}.`);
    });
  }

  function signOut() {
    setUser(null);
    setAuthToken("");
    setPage("auth");
    setParseResult(null);
    setMatchResult(null);
    localStorage.removeItem("jobmatch:user");
    setNotice("Signed out.");
  }

  return (
    <main className="app-shell">
      <AppHeader user={user} onSignOut={signOut} />
      {notice && <div className="notice">{notice}</div>}

      {page === "auth" && (
        <AuthPage
          mode={mode}
          setMode={setMode}
          authForm={authForm}
          updateAuthForm={updateAuthForm}
          submitAuth={submitAuth}
          busy={busy}
        />
      )}

      {page === "upload" && (
        <UploadPage
          profile={profile}
          updateProfile={updateProfile}
          resumeFile={resumeFile}
          setResumeFile={setResumeFile}
          analyzeResume={analyzeResume}
          busy={busy}
          user={user}
          jobs={jobs}
          refreshLiveJobs={refreshLiveJobs}
          analyzeProfileOnly={analyzeProfileOnly}
        />
      )}

      {page === "results" && (
        <ResultsPage
          topMatch={topMatch}
          parseResult={parseResult}
          matchResult={matchResult}
          resumeFile={resumeFile}
          onBack={() => setPage("upload")}
          onOpenLearning={() => setPage("learning")}
        />
      )}

      {page === "learning" && (
        <LearningPage
          matchResult={matchResult}
          topMatch={topMatch}
          onBack={() => setPage("results")}
          onUpload={() => setPage("upload")}
        />
      )}
    </main>
  );
}

function AppHeader({ user, onSignOut }) {
  return (
    <header className="app-topbar">
      <div>
        <p className="eyebrow">AI career assistant</p>
        <h1>Smart Job Matching</h1>
      </div>
      {user && (
        <div className="header-actions">
          <button className="secondary-button compact-button" onClick={onSignOut}>
            Sign out
          </button>
        </div>
      )}
    </header>
  );
}

function AuthPage({ mode, setMode, authForm, updateAuthForm, submitAuth, busy }) {
  return (
    <section className="auth-page">
      <div className="auth-hero">
        <p className="eyebrow">Smart Job Matching + Career Assistant</p>
        <h2>AI-Powered Smart Job Matching</h2>
        <p>
          A resume analysis platform that extracts skills, compares candidate
          fit with job roles, highlights skill gaps, and suggests learning,
          career, and interview guidance.
        </p>
        <div className="feature-strip">
          <span>Resume parsing</span>
          <span>Skill gap analysis</span>
          <span>Career guidance</span>
        </div>
      </div>

      <form className="panel auth-card stack" onSubmit={submitAuth}>
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Account</p>
            <h2>{mode === "login" ? "Welcome back" : "Create account"}</h2>
          </div>
        </div>
        <div className="segmented">
          <button type="button" className={mode === "login" ? "active" : ""} onClick={() => setMode("login")}>
            Login
          </button>
          <button type="button" className={mode === "register" ? "active" : ""} onClick={() => setMode("register")}>
            Register
          </button>
        </div>
        {mode === "register" && (
          <label>
            Full name
            <input
              name="fullName"
              value={authForm.fullName}
              onChange={updateAuthForm}
              placeholder="Enter your full name"
              required
            />
          </label>
        )}
        <label>
          Email
          <input
            name="email"
            type="email"
            value={authForm.email}
            onChange={updateAuthForm}
            placeholder="Enter your email"
            required
          />
        </label>
        <label>
          Password
          <input
            name="password"
            type="password"
            value={authForm.password}
            onChange={updateAuthForm}
            placeholder="Enter password"
            minLength="6"
            required
          />
        </label>
        <button className="primary-button" disabled={Boolean(busy)}>
          {busy || (mode === "login" ? "Continue to resume" : "Create account")}
        </button>
      </form>
    </section>
  );
}

function UploadPage({
  profile,
  updateProfile,
  resumeFile,
  setResumeFile,
  analyzeResume,
  busy,
  user,
  jobs,
  refreshLiveJobs,
  analyzeProfileOnly,
}) {
  return (
    <section className="flow-page">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Resume workspace</p>
          <h2>Build your candidate profile and upload your resume</h2>
        </div>
        {user && (
          <div className="user-card slim-user-card">
            <strong>{user.fullName}</strong>
            <span>{user.email}</span>
          </div>
        )}
      </div>

      <div className="upload-layout">
        <form className="panel profile-panel" onSubmit={analyzeResume}>
          <div className="panel-heading">
            <div>
              <p className="eyebrow">Candidate profile</p>
              <h2>Profile Builder</h2>
            </div>
          </div>
          <div className="profile-form">
            <label>
              Education
              <input name="education" value={profile.education} onChange={updateProfile} />
            </label>
            <label>
              Skills
              <textarea name="skills" value={profile.skills} onChange={updateProfile} rows="3" />
            </label>
            <label>
              Experience
              <textarea name="experience" value={profile.experience} onChange={updateProfile} rows="4" />
            </label>
            <label>
              Career preferences
              <textarea name="careerPreferences" value={profile.careerPreferences} onChange={updateProfile} rows="3" />
            </label>
          </div>
          <button
            type="button"
            className="primary-button full-width profile-match-button"
            onClick={analyzeProfileOnly}
            disabled={Boolean(busy)}
          >
            {busy || "Find job roles from profile"}
          </button>
        </form>

        <div className="side-stack">
          <ResumeUploadCard
            resumeFile={resumeFile}
            setResumeFile={setResumeFile}
            analyzeResume={analyzeResume}
            busy={busy}
          />
          <AvailableJobsPanel jobs={jobs} refreshLiveJobs={refreshLiveJobs} busy={busy} />
        </div>
      </div>
    </section>
  );
}

function ResumeUploadCard({ resumeFile, setResumeFile, analyzeResume, busy }) {
  return (
    <form className="panel upload-panel" onSubmit={analyzeResume}>
      <div className="panel-heading">
        <div>
          <p className="eyebrow">Resume upload</p>
          <h2>Upload & analyze</h2>
        </div>
      </div>
      <label className="drop-zone">
        <span>{resumeFile ? resumeFile.name : "Choose resume file"}</span>
        <small>PDF or DOCX, up to 10MB</small>
        <input
          type="file"
          accept=".pdf,.doc,.docx"
          onChange={(event) => setResumeFile(event.target.files?.[0] || null)}
        />
      </label>
      <div className="upload-summary">
        <span>Skills will be extracted from the resume.</span>
        <span>Jobs are ranked against backend-managed roles.</span>
        <span>Profile + resume are used for recommendations.</span>
      </div>
      <button className="primary-button full-width" disabled={Boolean(busy)}>
        {busy || "Analyze resume"}
      </button>
    </form>
  );
}

function AvailableJobsPanel({ jobs, refreshLiveJobs, busy }) {
  return (
    <section className="panel jobs-panel">
      <div className="panel-heading">
        <div>
          <p className="eyebrow">Available roles</p>
          <h2>Backend-managed jobs</h2>
        </div>
        <button
          type="button"
          className="secondary-button compact-button"
          onClick={refreshLiveJobs}
          disabled={Boolean(busy)}
        >
          Refresh live jobs
        </button>
      </div>
      <div className="job-list">
        {list(jobs).map((job) => (
          <article className="job-card" key={job.id || job.title}>
            <strong>{job.title}</strong>
            {(job.company || job.location || job.source) && (
              <small>
                {[job.company, job.location, job.source].filter(Boolean).join(" - ")}
              </small>
            )}
            <span>{list(job.requiredSkills).join(", ") || "No skills listed"}</span>
          </article>
        ))}
        {!list(jobs).length && <p className="empty">No jobs available yet.</p>}
      </div>
    </section>
  );
}

function ResultsPage({ topMatch, parseResult, matchResult, resumeFile, onBack, onOpenLearning }) {
  const breakdown = matchResult?.scoreBreakdown || {};
  const chartItems = [
    ["Semantic match", breakdown.semanticMatch],
    ["Skills match", breakdown.skillsMatch],
    ["Experience match", breakdown.experienceMatch],
  ];

  return (
    <section className="results-page">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Analysis results</p>
          <h2>{topMatch ? topMatch.title : "Resume analysis"}</h2>
          <p className="page-copy">
            {resumeFile ? resumeFile.name : parseResult?.fileName || "Candidate profile match"}
          </p>
        </div>
        <div className="result-actions">
          {topMatch && <span className="score-pill">{scorePercent(topMatch.score)}%</span>}
          <button className="secondary-button compact-button" onClick={onBack}>Upload another</button>
        </div>
      </div>

      <div className="result-grid results-grid-wide">
        <ChartBlock title="Match score breakdown" items={chartItems} />
        <ChartBlock title="Skill gap chart" items={list(matchResult?.skillGaps).map((skill) => [skill, 1])} inverse />
        <ResultBlock title="Top matches" items={list(matchResult?.matches).map((match) => `${match.title} - ${scorePercent(match.score)}%`)} />
        <ResultBlock title="Extracted skills" items={list(matchResult?.extractedSkills || parseResult?.extractedSkills)} chip />
        <ResultBlock title="Skill gaps" items={list(matchResult?.skillGaps)} chip />
        <ResultBlock title="Learning suggestions" items={list(matchResult?.learningSuggestions)} />
        <ResultBlock title="Career path" items={list(matchResult?.careerRecommendations)} />
        <ResultBlock title="Interview prep" items={list(matchResult?.interviewQuestions)} />
      </div>

      <div className="learning-open-panel">
        <button className="primary-button learning-open-button" onClick={onOpenLearning}>
          Open
        </button>
        <p>
          Learning paths for your current skill gaps with YouTube videos, free notes,
          and course-platform options based on this candidate analysis.
        </p>
      </div>
    </section>
  );
}

function LearningPage({ matchResult, topMatch, onBack, onUpload }) {
  const skillGaps = list(matchResult?.skillGaps);
  const suggestions = list(matchResult?.learningSuggestions);

  return (
    <section className="learning-page">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Personalized learning path</p>
          <h2>{topMatch ? `Improve for ${topMatch.title}` : "Learning options"}</h2>
          <p className="page-copy">
            Resources are generated from the skill gaps found in the current candidate result.
          </p>
        </div>
        <div className="result-actions">
          <button className="secondary-button compact-button" onClick={onBack}>Back to results</button>
          <button className="secondary-button compact-button" onClick={onUpload}>New analysis</button>
        </div>
      </div>

      {skillGaps.length ? (
        <div className="learning-grid">
          {skillGaps.map((skill) => (
            <section className="learning-skill-card" key={skill}>
              <div className="learning-skill-heading">
                <p className="eyebrow">Skill gap</p>
                <h3>{skill}</h3>
              </div>
              <div className="learning-option-list">
                {learningOptionsFor(skill, suggestions).map((option) => (
                  <a
                    className="learning-option"
                    href={option.url}
                    target="_blank"
                    rel="noreferrer"
                    key={`${skill}-${option.type}`}
                  >
                    <span>{option.type}</span>
                    <strong>{option.title}</strong>
                    <small>{option.detail}</small>
                  </a>
                ))}
              </div>
            </section>
          ))}
        </div>
      ) : (
        <div className="panel learning-empty">
          <h3>No skill gaps found</h3>
          <p className="empty">
            This analysis did not return missing skills. Run another resume or profile match to build a learning path.
          </p>
        </div>
      )}
    </section>
  );
}

function ChartBlock({ title, items, inverse = false }) {
  const chartItems = list(items);
  return (
    <div className="result-block chart-block">
      <h3>{title}</h3>
      {chartItems.length ? chartItems.map(([label, rawValue]) => {
        const value = inverse ? 1 : Number(rawValue || 0);
        const percent = inverse ? 100 : scorePercent(value);
        return (
          <div className="chart-row" key={label}>
            <div className="chart-label">
              <span>{label}</span>
              <strong>{inverse ? "Gap" : `${percent}%`}</strong>
            </div>
            <div className="bar-track">
              <span style={{ width: `${Math.max(10, percent)}%` }} />
            </div>
          </div>
        );
      }) : (
        <p className="empty">No chart data yet</p>
      )}
    </div>
  );
}

function ResultBlock({ title, items, chip = false }) {
  return (
    <div className="result-block">
      <h3>{title}</h3>
      {items.length ? (
        <div className={chip ? "chip-list" : "text-list"}>
          {items.map((item) => (
            <span key={item}>{item}</span>
          ))}
        </div>
      ) : (
        <p className="empty">No data yet</p>
      )}
    </div>
  );
}

export default App;
