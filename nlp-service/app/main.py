import io
import json
import os
import re
import hashlib
from typing import Any, Dict, List, Optional

import numpy as np
import pdfplumber
import spacy
from fastapi import FastAPI, File, UploadFile
from pydantic import BaseModel

try:
    import docx  # python-docx
except Exception:
    docx = None


APP_DIR = os.path.dirname(os.path.abspath(__file__))


def _load_json(path: str, default):
    if not os.path.exists(path):
        return default
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


SKILL_ONTOLOGY: List[str] = _load_json(
    os.path.join(APP_DIR, "skill_ontology.json"),
    default=[
        "Java",
        "Spring Boot",
        "REST",
        "MySQL",
        "MongoDB",
        "React",
        "HTML",
        "CSS",
        "JavaScript",
        "Python",
        "NLP",
        "BERT",
        "spaCy",
        "Machine Learning",
        "Security",
        "API Integration",
    ],
)

LEARNING_PATHS: Dict[str, List[str]] = _load_json(
    os.path.join(APP_DIR, "learning_paths.json"),
    default={},
)

CAREER_PATHS: Dict[str, List[str]] = _load_json(
    os.path.join(APP_DIR, "career_paths.json"),
    default={},
)

INTERVIEW_QUESTIONS: Dict[str, List[str]] = _load_json(
    os.path.join(APP_DIR, "interview_questions.json"),
    default={},
)

IGNORED_SKILLS = {"resume parsing"}


def normalize_text(text: str) -> str:
    text = text.lower()
    text = re.sub(r"[^a-z0-9+.#\s-]+", " ", text)
    return " ".join(text.split())


def extract_skills_from_text(text: str) -> List[str]:
    norm = normalize_text(text)
    found: List[str] = []
    for skill in SKILL_ONTOLOGY:
        if normalize_text(skill) in IGNORED_SKILLS:
            continue
        s_norm = normalize_text(skill)
        if not s_norm:
            continue
        if s_norm in norm:
            found.append(skill)
    # De-duplicate while preserving order
    seen = set()
    deduped = []
    for s in found:
        if s not in seen:
            seen.add(s)
            deduped.append(s)
    return deduped


def extract_text_from_pdf(file_bytes: bytes) -> str:
    out: List[str] = []
    with pdfplumber.open(io.BytesIO(file_bytes)) as pdf:
        for page in pdf.pages:
            txt = page.extract_text() or ""
            out.append(txt)
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
    if lower.endswith(".docx") or lower.endswith(".doc"):
        return extract_text_from_docx(file_bytes)
    # Fallback: just return empty; we expect PDF/DOCX for this MVP.
    return ""


class JobPayload(BaseModel):
    title: str
    description: str
    requiredSkills: List[str] = []


class MatchJobsRequest(BaseModel):
    resumeText: str
    profileText: str = ""
    jobs: List[JobPayload]


class TopMatch(BaseModel):
    title: str
    score: float


class ScoreBreakdown(BaseModel):
    semanticMatch: float
    skillsMatch: float
    experienceMatch: float


class MatchJobsResponse(BaseModel):
    topMatches: List[TopMatch]
    extractedSkills: List[str]
    skillGaps: List[str]
    learningSuggestions: List[str]
    careerRecommendations: List[str]
    interviewQuestions: List[str]
    scoreBreakdown: ScoreBreakdown


app = FastAPI(title="NLP Job Matching Service")

spacy_nlp: Optional[object] = None
embedder: Optional[Any] = None


@app.on_event("startup")
def _startup():
    global spacy_nlp, embedder
    # Load spaCy model if available; otherwise use a lightweight tokenizer.
    try:
        spacy_nlp = spacy.load("en_core_web_sm")
    except Exception:
        spacy_nlp = spacy.blank("en")

    # BERT-style semantic embeddings are enabled by default. If the model is not
    # cached or cannot load, the service falls back to lexical vectors.
    if os.getenv("NLP_USE_TRANSFORMER", "true").lower() == "true":
        try:
            from sentence_transformers import SentenceTransformer
            embedder = SentenceTransformer("sentence-transformers/all-MiniLM-L6-v2")
        except Exception:
            embedder = None


@app.post("/extract-skills")
def extract_skills(file: UploadFile = File(...)):
    file_bytes = file.file.read()
    filename = file.filename or "resume"

    text = extract_resume_text(file_bytes, filename)
    if not text:
        # If parsing fails, still try to create a minimal response
        # by using an empty string (matching will be weak).
        text = ""

    skills = extract_skills_from_text(text)

    return {"text": text, "skills": skills}


def cosine_similarity_matrix(a: np.ndarray, b: np.ndarray) -> np.ndarray:
    # a: (1, dim), b: (n, dim)
    a_norm = a / (np.linalg.norm(a, axis=1, keepdims=True) + 1e-12)
    b_norm = b / (np.linalg.norm(b, axis=1, keepdims=True) + 1e-12)
    return np.dot(a_norm, b_norm.T).squeeze(0)


def lexical_vector(text: str, dimensions: int = 384) -> np.ndarray:
    vector = np.zeros(dimensions, dtype=float)
    tokens = normalize_text(text).split()
    for token in tokens:
        digest = hashlib.sha256(token.encode("utf-8")).digest()
        index = int.from_bytes(digest[:4], "big") % dimensions
        vector[index] += 1.0
    return vector


def encode_texts(texts: List[str]) -> np.ndarray:
    if not texts:
        return np.zeros((0, 384), dtype=float)
    if embedder is not None:
        return embedder.encode(texts, normalize_embeddings=False)
    return np.vstack([lexical_vector(text) for text in texts])


@app.post("/match-jobs", response_model=MatchJobsResponse)
def match_jobs(req: MatchJobsRequest):
    resume_text = req.resumeText or ""
    profile_text = req.profileText or ""
    candidate_text = "\n".join([resume_text, profile_text]).strip()
    jobs = req.jobs or []
    if not jobs:
        return {
            "topMatches": [],
            "extractedSkills": extract_skills_from_text(candidate_text),
            "skillGaps": [],
            "learningSuggestions": [],
            "careerRecommendations": [],
            "interviewQuestions": [],
            "scoreBreakdown": {"semanticMatch": 0.0, "skillsMatch": 0.0, "experienceMatch": 0.0},
        }

    job_descriptions = [j.description or "" for j in jobs]
    titles = [j.title for j in jobs]
    required_skills_by_job = [j.requiredSkills or [] for j in jobs]

    resume_emb = encode_texts([candidate_text])
    jobs_emb = encode_texts(job_descriptions)

    sims = cosine_similarity_matrix(resume_emb, jobs_emb)
    # Rank by similarity score
    ranked_idx = np.argsort(-sims)[: min(3, len(jobs))]

    top_matches: List[Dict[str, float]] = []
    for idx in ranked_idx:
        top_matches.append({"title": titles[idx], "score": float(sims[idx])})

    # Skill gap analysis: compare extracted skills vs required skills.
    extracted_skills = extract_skills_from_text(candidate_text)
    extracted_norm = {normalize_text(s) for s in extracted_skills}

    top_indices = [int(i) for i in ranked_idx[: min(3, len(ranked_idx))]]  # top 1..3

    # Union required skills across top matches (keeps order).
    required_union_ordered: List[str] = []
    required_seen_norm = set()
    for idx in top_indices:
        job_required = required_skills_by_job[idx] if idx < len(required_skills_by_job) else []
        for rs in job_required or []:
            rs_norm = normalize_text(rs)
            if rs_norm in IGNORED_SKILLS:
                continue
            if rs_norm and rs_norm not in required_seen_norm:
                required_seen_norm.add(rs_norm)
                required_union_ordered.append(rs)

    gaps: List[str] = []
    for rs in required_union_ordered:
        if normalize_text(rs) not in extracted_norm:
            gaps.append(rs)

    learning_suggestions: List[str] = []
    for gap in gaps:
        suggestions = LEARNING_PATHS.get(gap) or LEARNING_PATHS.get(normalize_text(gap)) or []
        if suggestions:
            learning_suggestions.extend(suggestions)
        else:
            learning_suggestions.append(f"Learn '{gap}' with a beginner-to-intermediate course and practice projects.")

    primary_required = required_skills_by_job[top_indices[0]] if top_indices else []
    primary_title: str = titles[top_indices[0]] if len(titles) > 0 and top_indices else "General"
    career_recommendations = CAREER_PATHS.get(primary_title) or CAREER_PATHS.get(normalize_text(primary_title)) or []
    if not career_recommendations:
        career_recommendations = [f"Target role: {primary_title}"] + [
            f"Build/strengthen: {s}" for s in (primary_required[:3] if primary_required else [])
        ]

    interview_questions = INTERVIEW_QUESTIONS.get(primary_title) or INTERVIEW_QUESTIONS.get(normalize_text(primary_title)) or []
    if not interview_questions:
        interview_questions = [
            f"Walk me through a project that demonstrates {primary_title}.",
            "What is your experience with NLP/ML-based matching or embeddings (if any)?",
        ]
        if gaps:
            interview_questions.append(f"How will you close these skill gaps: {', '.join(gaps)}?")

    semantic_match = float(sims[top_indices[0]]) if top_indices else 0.0
    primary_required_norm = {normalize_text(s) for s in primary_required or [] if normalize_text(s) and normalize_text(s) not in IGNORED_SKILLS}
    matched_required = primary_required_norm.intersection(extracted_norm)
    skills_match = (len(matched_required) / len(primary_required_norm)) if primary_required_norm else 0.0
    experience_keywords = ["experience", "built", "developed", "implemented", "project", "internship", "worked"]
    experience_match = sum(1 for word in experience_keywords if word in normalize_text(candidate_text)) / len(experience_keywords)

    if career_recommendations:
        career_recommendations = [
            f"Best-fit target: {primary_title}",
            f"Profile-informed recommendation: strengthen {', '.join(gaps[:3]) if gaps else 'your strongest matched skills'}."
        ] + career_recommendations

    return {
        "topMatches": top_matches,
        "extractedSkills": extracted_skills,
        "skillGaps": gaps,
        "learningSuggestions": learning_suggestions,
        "careerRecommendations": career_recommendations,
        "interviewQuestions": interview_questions,
        "scoreBreakdown": {
            "semanticMatch": semantic_match,
            "skillsMatch": float(skills_match),
            "experienceMatch": float(experience_match),
        },
    }

