import io
import json
import os
import re
from contextlib import asynccontextmanager
from typing import Dict, List, Optional

import numpy as np
import pdfplumber
import spacy
from fastapi import FastAPI, File, UploadFile
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer

try:
    import docx
except Exception:
    docx = None

APP_DIR = os.path.dirname(os.path.abspath(__file__))

def _load_json(path: str, default):
    if not os.path.exists(path):
        return default
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)

SKILL_ONTOLOGY: List[str] = _load_json(os.path.join(APP_DIR, "skill_ontology.json"), default=["Java", "Python", "React"])
LEARNING_PATHS: Dict[str, List[str]] = _load_json(os.path.join(APP_DIR, "learning_paths.json"), default={})
CAREER_PATHS: Dict[str, List[str]] = _load_json(os.path.join(APP_DIR, "career_paths.json"), default={})
INTERVIEW_QUESTIONS: Dict[str, List[str]] = _load_json(os.path.join(APP_DIR, "interview_questions.json"), default={})

spacy_nlp: Optional[object] = None
embedder: Optional[SentenceTransformer] = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Load models on startup (modern FastAPI lifespan — replaces deprecated @app.on_event)."""
    global spacy_nlp, embedder
    try:
        spacy_nlp = spacy.load("en_core_web_sm")
    except Exception:
        spacy_nlp = spacy.blank("en")
    embedder = SentenceTransformer("sentence-transformers/all-MiniLM-L6-v2")
    yield
    # (shutdown cleanup could go here if needed)


app = FastAPI(title="NLP Job Matching Service", lifespan=lifespan)


def normalize_text(text: str) -> str:
    text = text.lower()
    text = re.sub(r"[^a-z0-9+.#\s/-]+", " ", text)
    return " ".join(text.split())


def extract_skills_from_text(text: str) -> List[str]:
    norm = normalize_text(text)
    found, seen = [], set()
    for skill in SKILL_ONTOLOGY:
        s_norm = normalize_text(skill)
        if s_norm and s_norm in norm and skill not in seen:
            seen.add(skill)
            found.append(skill)
    return found


def extract_text_from_pdf(file_bytes: bytes) -> str:
    out = []
    with pdfplumber.open(io.BytesIO(file_bytes)) as pdf:
        for page in pdf.pages:
            out.append(page.extract_text() or "")
    return "\n".join(out).strip()


def extract_text_from_docx(file_bytes: bytes) -> str:
    if docx is None:
        return ""
    document = docx.Document(io.BytesIO(file_bytes))
    return "\n".join([p.text for p in document.paragraphs]).strip()


def extract_resume_text(file_bytes: bytes, filename: str) -> str:
    lower = (filename or "").lower()
    if lower.endswith(".pdf"):
        return extract_text_from_pdf(file_bytes)
    if lower.endswith((".docx", ".doc")):
        return extract_text_from_docx(file_bytes)
    return ""


class JobPayload(BaseModel):
    title: str
    description: str
    requiredSkills: List[str] = []


class MatchJobsRequest(BaseModel):
    resumeText: str
    jobs: List[JobPayload]


class TopMatch(BaseModel):
    title: str
    score: float


class MatchJobsResponse(BaseModel):
    # NOTE: field name is "topMatches" — Spring Boot DTO uses @JsonProperty("topMatches")
    topMatches: List[TopMatch]
    extractedSkills: List[str]
    skillGaps: List[str]
    learningSuggestions: List[str]
    careerRecommendations: List[str]
    interviewQuestions: List[str]


def cosine_sim(a: np.ndarray, b: np.ndarray) -> np.ndarray:
    a_n = a / (np.linalg.norm(a, axis=1, keepdims=True) + 1e-12)
    b_n = b / (np.linalg.norm(b, axis=1, keepdims=True) + 1e-12)
    return np.dot(a_n, b_n.T).squeeze(0)


@app.post("/extract-skills")
def extract_skills(file: UploadFile = File(...)):
    file_bytes = file.file.read()
    text = extract_resume_text(file_bytes, file.filename or "resume")
    skills = extract_skills_from_text(text)
    return {"text": text, "skills": skills}


@app.post("/match-jobs", response_model=MatchJobsResponse)
def match_jobs(req: MatchJobsRequest):
    if embedder is None:
        raise RuntimeError("Embedding model not loaded yet.")

    resume_text = req.resumeText or ""
    jobs = req.jobs or []
    titles = [j.title for j in jobs]
    descriptions = [j.description or "" for j in jobs]
    required_by_job = [j.requiredSkills or [] for j in jobs]

    resume_emb = embedder.encode([resume_text], normalize_embeddings=False)
    jobs_emb = embedder.encode(descriptions, normalize_embeddings=False)
    sims = cosine_sim(resume_emb, jobs_emb)

    top_n = min(3, len(jobs))
    ranked = np.argsort(-sims)[:top_n]
    top_matches = [TopMatch(title=titles[i], score=float(sims[i])) for i in ranked]

    extracted = extract_skills_from_text(resume_text)
    extracted_norm = {normalize_text(s) for s in extracted}

    # Union required skills from top matched jobs
    req_union, req_seen = [], set()
    for idx in ranked:
        for rs in required_by_job[idx]:
            rn = normalize_text(rs)
            if rn and rn not in req_seen:
                req_seen.add(rn)
                req_union.append(rs)

    gaps = [rs for rs in req_union if normalize_text(rs) not in extracted_norm]

    learning_suggestions = []
    for gap in gaps:
        sug = LEARNING_PATHS.get(gap) or LEARNING_PATHS.get(normalize_text(gap)) or []
        if sug:
            learning_suggestions.extend(sug)
        else:
            learning_suggestions.append(f"Study '{gap}': take a beginner-to-intermediate course and build a hands-on project.")

    primary_title = titles[ranked[0]] if ranked.size > 0 and titles else "General"
    primary_required = required_by_job[ranked[0]] if ranked.size > 0 else []

    career_recs = CAREER_PATHS.get(primary_title) or CAREER_PATHS.get(normalize_text(primary_title)) or []
    if not career_recs:
        career_recs = [f"Target role: {primary_title}"] + [f"Strengthen: {s}" for s in primary_required[:3]]

    interview_qs = INTERVIEW_QUESTIONS.get(primary_title) or INTERVIEW_QUESTIONS.get(normalize_text(primary_title)) or []
    if not interview_qs:
        interview_qs = [
            f"Walk me through a project that demonstrates your {primary_title} skills.",
            "How do you stay up-to-date with new technologies in your field?",
        ]
        if gaps:
            interview_qs.append(f"You appear to be missing: {', '.join(gaps[:3])}. How are you planning to develop these?")

    return MatchJobsResponse(
        topMatches=top_matches,
        extractedSkills=extracted,
        skillGaps=gaps,
        learningSuggestions=learning_suggestions,
        careerRecommendations=career_recs,
        interviewQuestions=interview_qs,
    )
