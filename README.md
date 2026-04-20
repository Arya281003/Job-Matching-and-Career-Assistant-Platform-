# AI-Powered Smart Job Matching + Career Assistant

This project is built on the tech stack described in your PPT:

- Frontend: React (React + HTML/CSS)
- Backend: Java Spring Boot
- AI & NLP: Python (spaCy + BERT-style embeddings)
- Database: MySQL + MongoDB

## Folder structure

- `frontend/` - React UI
- `backend/` - Spring Boot REST API
- `nlp-service/` - Python NLP microservice (resume parsing, skill extraction, job matching)

## Local setup (quick start)

### 1) Start databases

#### Option A: Using Docker (recommended if available)

If Docker Desktop is installed and your Docker CLI works:

```bash
docker compose up -d
```

MySQL: `jobmatch_db` (port `3306`)

MongoDB: database used by the Spring Boot config (port `27017`)

#### Option B: No Docker (run DBs locally)

1. Install and start **MySQL 8** on port `3306`
2. Create:
   - Database: `jobmatch_db`
   - User: `jobmatch`
   - Password: `jobmatchpassword`
3. Install and start **MongoDB** on port `27017`
4. MongoDB DB name expected by the app is `jobmatch_db` (it can be created automatically on first write).

## Demo mode (run without MySQL/Mongo right now)

If you just want to run the project UI + matching logic without setting up MySQL/Mongo:

- The backend defaults to **in-memory H2** for login/profile (so MySQL is not required).
- If MongoDB is not running, job matching falls back to a small built-in job set.

So you can skip the DB setup and do steps 2-4 below.

### 2) Start Python NLP service

```bash
cd nlp-service
python -m venv .venv
./.venv/Scripts/activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
```

If spaCy model is not installed, run:

```bash
python -m spacy download en_core_web_sm
```

### 3) Start Spring Boot backend

```bash
cd backend
mvn spring-boot:run
```

If `mvn` is not recognized, install **Apache Maven** and ensure it’s added to PATH (Java is already required).

### 4) Start React frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on `http://localhost:5173` by default.

## What you can demo

- Register/Login (simple demo flow stored in MySQL)
- Upload a resume to extract skills (NLP service)
- Match candidate resume with job descriptions (NLP service)
- Display skill gaps + learning path suggestions

