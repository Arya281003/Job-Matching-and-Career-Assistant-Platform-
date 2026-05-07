# AI-Powered Smart Job Matching + Career Assistant

**Stack:** React (Vite) · Java Spring Boot · Python FastAPI · MySQL/MongoDB

---

## What's new in this version

### Bug Fixes
- **`topMatches` deserialization fix** — Added `@JsonProperty("topMatches")` to `NlpMatchResponse.java` so Spring Boot correctly maps the Python NLP service response. The frontend now receives `result.matches` properly.

### JWT Authentication
- Backend now issues a **JWT token** on login/register (24h expiry, configurable via `app.jwt.secret` and `app.jwt.expiration-ms` in `application.yml`)
- `AuthResponse` includes a `token` field
- All API routes except `/api/auth/**` now require a valid `Authorization: Bearer <token>` header
- Token is stored in `sessionStorage` on the frontend and sent automatically with every request
- `JwtService.java` — token generation & validation using `jjwt 0.12.5`
- `JwtFilter.java` — servlet filter that validates the token on every protected route

### Expanded Skill Ontology
- **17 → 150+ skills** across: Programming Languages, Frontend, Backend & Frameworks, Databases, Cloud & DevOps, AI/ML/Data Science, Security, Data Engineering, Mobile, Tools & Practices

### Redesigned UI (Dark Theme)
- Professional dark dashboard: indigo + emerald accent palette
- Visual score bar on job match cards (percentage-based)
- Drag-and-drop upload zone for resumes
- Stat summary cards on the Profile page
- Skill gap chips in amber, extracted skill chips in indigo
- Responsive sidebar navigation with icons
- Improved login/register page with side-by-side hero panel

### NLP Service Improvements
- Replaced deprecated `@app.on_event("startup")` with modern FastAPI `lifespan` context manager
- Cleaner cosine similarity code
- Better fallback interview questions mentioning specific skill gaps

---

## Project Structure

```
.
├── frontend/          React + Vite UI
├── backend/           Java Spring Boot REST API
│   └── src/main/java/com/example/jobmatch/
│       ├── controller/    Auth, Matching, Profile, Resume controllers
│       ├── security/      JwtService, JwtFilter   ← NEW
│       ├── config/        CORS, DataSeeder, SecurityFilterConfig  ← UPDATED
│       ├── dto/           DTOs (AuthResponse now includes token)
│       ├── model/         JPA + MongoDB entities
│       ├── repository/    Spring Data repositories
│       └── service/       MatchingService, NlpClientService
└── nlp-service/       Python FastAPI NLP microservice
    └── app/
        ├── main.py              ← UPDATED (lifespan, cleaner code)
        ├── skill_ontology.json  ← UPDATED (150+ skills)
        ├── learning_paths.json
        ├── career_paths.json
        └── interview_questions.json
```

---

## Local Setup

### 1. Start databases (optional — app has demo/in-memory fallback)

```bash
docker compose up -d
```

### 2. Start Python NLP service

```bash
cd nlp-service
python -m venv .venv && .venv/Scripts/activate   # Windows
# OR: source .venv/bin/activate                  # Mac/Linux
pip install -r requirements.txt
python -m spacy download en_core_web_sm
uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
```

### 3. Start Spring Boot backend

```bash
cd backend
mvn spring-boot:run
```

### 4. Start React frontend

```bash
cd frontend
npm install
npm run dev
# → http://localhost:5173
```

---

## JWT Config (application.yml)

```yaml
app:
  jwt:
    secret: your-secret-key-min-32-chars
    expiration-ms: 86400000   # 24 hours
  cors:
    allowed-origins: http://localhost:5173
```

Change `app.jwt.secret` before deploying to production.
