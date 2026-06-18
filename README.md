---
title: UGrow Career Assistant
emoji: 🤗
colorFrom: blue
colorTo: green
sdk: docker
app_port: 7860
pinned: false
---

# AI-Powered Smart Job Matching + Career Assistant
## Live Demo

[Open UGrow Career Assistant](https://aryaagg-ugrow-career-assistant.hf.space)

A full-stack job matching platform that parses resumes, extracts skills, compares candidates with job roles, identifies skill gaps, and generates learning/career/interview guidance.

## Tech Stack

- **Frontend:** React + Vite
- **Backend:** Java Spring Boot REST API
- **NLP Service:** Python FastAPI, spaCy, sentence-transformers, NumPy
- **Relational Database:** MySQL for users, authentication, and profiles
- **Document Database:** MongoDB for jobs, resumes, and saved analysis results
- **Authentication:** JWT bearer tokens

## Core Features

- Login/register with JWT authentication
- Profile creation and persistence in MySQL
- Resume upload and text extraction from PDF/DOCX
- Skill extraction using an extensible skill ontology
- Semantic job matching using sentence-transformer embeddings with lexical fallback
- Match score breakdown: semantic match, skills match, and experience match
- Skill gap analysis with learning suggestions
- Career path recommendations and interview questions
- MongoDB persistence for resumes, jobs, and analysis history
- Backend job-management endpoints for adding/editing/deleting roles
- Evaluation benchmark for Top-1, Top-3, MRR, skill precision, and skill recall

## Architecture

```text
React Frontend
    |
    | REST + JWT
    v
Spring Boot Backend
    |             |
    |             +-- MySQL: users and profiles
    |             +-- MongoDB: jobs, resumes, analyses
    |
    | REST
    v
FastAPI NLP Service
    |
    +-- Resume parsing
    +-- Skill extraction
    +-- Semantic matching
    +-- Score breakdowns
```

## Folder Structure

- `frontend/` - React UI
- `backend/` - Spring Boot REST API
- `nlp-service/` - Python NLP microservice and evaluation benchmark
- `nlp-service/evaluation_dataset.csv` - labeled benchmark dataset
- `nlp-service/evaluate.py` - evaluation script
- `.env.example` - example environment variables

## Database Design

The project uses two databases:

- **MySQL** stores relational data:
  - users
  - password hashes
  - profiles

- **MongoDB** stores document data:
  - job descriptions
  - uploaded resume text
  - extracted skills
  - saved analysis results

## Sample Data

The project includes:

- 15 seeded job roles
- 66 skills in the skill ontology
- 32 learning suggestions
- 15 career-role mappings
- 75 interview questions
- 75 labeled benchmark resume samples for evaluation

## Local Setup

### 1. Start Databases

If Docker Desktop is available:

```bash
docker compose up -d
```

MySQL defaults:

- Database: `jobmatch_db`
- Port: `3306`
- User: `jobmatch`
- Password: `jobmatchpassword`

MongoDB defaults:

- Database: `jobmatch_db`
- Port: `27017`

If you run databases manually, make sure MySQL and MongoDB use the same ports and credentials.

### 2. Start NLP Service

```bash
cd nlp-service
python -m venv .venv
./.venv/Scripts/activate
pip install -r requirements.txt
python -m uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
```

For faster local startup without transformer embeddings:

```bash
$env:NLP_USE_TRANSFORMER="false"
python -m uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
```

### 3. Start Backend

```bash
cd backend
mvn spring-boot:run
```

### 4. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```

## Evaluation

The project does not claim fake accuracy. It includes a labeled benchmark dataset and reports measurable metrics.

Run fast lexical evaluation:

```bash
cd nlp-service
python evaluate.py
```

Run transformer-based evaluation:

```bash
cd nlp-service
python evaluate.py --use-transformer
```

Metrics reported:

- **Top-1 Accuracy:** correct role is ranked first
- **Top-3 Accuracy:** correct role appears in the top 3
- **Mean Reciprocal Rank:** ranking quality across predictions
- **Skill Precision:** extracted skills that are correct
- **Skill Recall:** expected skills that were found

Use the printed metrics in your report instead of guessing an accuracy rate.

## Professional Positioning

This project is best described as:

> An AI-assisted resume-to-job matching platform that combines semantic embeddings, skill ontology matching, skill-gap analysis, and a full-stack production-style architecture.

It is suitable for portfolio/job interviews because it demonstrates:

- full-stack engineering
- API design
- JWT authentication
- relational + document database modeling
- NLP/ML integration
- evaluation metrics
- clean UI workflow
- realistic system architecture

## What You Can Demo

- Register/login
- Create a candidate profile
- Upload a resume
- View extracted skills
- View match score breakdown
- View top job matches
- View skill gaps
- View learning suggestions
- View career path recommendations
- View interview questions
- Discuss evaluation metrics from `evaluate.py`

## Deploy on Hugging Face Spaces

Use a **Docker** Space. The root `Dockerfile` builds the React frontend, Spring Boot backend, and FastAPI NLP service into one container. The public Space port is `7860`; nginx serves the frontend and forwards `/api/*` to the backend.

Hugging Face Spaces does not run `docker-compose.yml`, so use managed databases instead of local containers. Add these Space secrets:

```text
MYSQL_URL=jdbc:mysql://<host>:<port>/<database>?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC
MYSQL_USER=<mysql-user>
MYSQL_PASSWORD=<mysql-password>
MONGODB_URI=mongodb+srv://<user>:<password>@<cluster>/<database>?retryWrites=true&w=majority
```

Optional secrets/settings:

```text
NLP_USE_TRANSFORMER=false
LIVE_JOBS_SCHEDULED=false
```

`NLP_USE_TRANSFORMER=false` keeps startup lighter for the free CPU tier. Set it to `true` only if your Space has enough memory and you want sentence-transformer embeddings.

### Upload to a Space

```bash
git lfs install
git remote add space https://huggingface.co/spaces/<your-username>/<your-space-name>
git push space main
```

If you already use `origin` for GitHub, keep it and add Hugging Face as the separate `space` remote shown above.
