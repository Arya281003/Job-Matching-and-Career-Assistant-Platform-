import argparse
import csv
import os
from dataclasses import dataclass
from typing import Dict, List

os.environ.setdefault("NLP_USE_TRANSFORMER", "false")

from app.main import (  # noqa: E402
    JobPayload,
    MatchJobsRequest,
    extract_skills_from_text,
    match_jobs,
    normalize_text,
    _startup,
)


JOBS = [
    JobPayload(title="Java Backend Developer", description="Build secure REST APIs using Java, Spring Boot, MySQL, MongoDB, JWT, and cloud-ready backend practices.", requiredSkills=["Java", "Spring Boot", "REST", "MySQL", "MongoDB", "JWT", "Security", "Docker"]),
    JobPayload(title="Frontend Developer (React)", description="Create responsive React interfaces, reusable components, stateful forms, file upload flows, and polished dashboards.", requiredSkills=["React", "JavaScript", "HTML", "CSS", "Responsive Design", "API Integration", "UI/UX"]),
    JobPayload(title="Full Stack Developer", description="Develop end-to-end web features across React, Spring Boot, MySQL, MongoDB, authentication, and deployment workflows.", requiredSkills=["React", "Java", "Spring Boot", "MySQL", "MongoDB", "REST", "Git", "Docker"]),
    JobPayload(title="Data Scientist / NLP Engineer", description="Develop NLP pipelines for resume parsing, skill extraction, semantic matching, embeddings, and model evaluation.", requiredSkills=["Python", "NLP", "BERT", "spaCy", "Machine Learning", "Embeddings"]),
    JobPayload(title="Machine Learning Engineer", description="Build ML pipelines, train models, evaluate performance, serve predictions, and monitor model quality in production.", requiredSkills=["Python", "Machine Learning", "Scikit-learn", "Pandas", "NumPy", "Model Evaluation", "MLOps"]),
    JobPayload(title="Data Analyst", description="Analyze business datasets, build dashboards, write SQL queries, and communicate insights through visual reporting.", requiredSkills=["SQL", "Excel", "Power BI", "Tableau", "Data Visualization", "Statistics", "Python"]),
    JobPayload(title="DevOps Engineer", description="Automate builds, deployments, containerization, monitoring, CI/CD pipelines, and cloud infrastructure operations.", requiredSkills=["Docker", "Kubernetes", "CI/CD", "Linux", "AWS", "Monitoring", "GitHub Actions"]),
    JobPayload(title="Cloud Engineer", description="Design and maintain cloud services, networking, storage, security, deployments, and scalable application infrastructure.", requiredSkills=["AWS", "Azure", "Cloud Computing", "Docker", "Kubernetes", "Networking", "Security"]),
    JobPayload(title="QA Automation Engineer", description="Create automated test suites for web APIs and frontend workflows using Selenium, API testing, and CI integration.", requiredSkills=["Testing", "Selenium", "JUnit", "Postman", "API Testing", "Java", "CI/CD"]),
    JobPayload(title="Cybersecurity Analyst", description="Monitor security risks, analyze vulnerabilities, secure APIs, review authentication flows, and improve application security.", requiredSkills=["Cybersecurity", "Security", "OWASP", "JWT", "API Security", "Linux", "Networking"]),
    JobPayload(title="Database Developer", description="Design schemas, optimize SQL queries, model relational data, tune indexes, and manage database-backed applications.", requiredSkills=["MySQL", "SQL", "Database Design", "Indexing", "JPA", "Query Optimization"]),
    JobPayload(title="Mobile App Developer", description="Build mobile applications, integrate APIs, manage app state, create responsive screens, and publish production builds.", requiredSkills=["React Native", "Mobile Development", "JavaScript", "API Integration", "UI/UX", "Git"]),
    JobPayload(title="Product Analyst", description="Use product data, funnels, experiments, dashboards, and user behavior analysis to guide product decisions.", requiredSkills=["SQL", "Analytics", "Excel", "Data Visualization", "Statistics", "Product Thinking"]),
    JobPayload(title="AI Application Developer", description="Build AI-powered applications using embeddings, prompt workflows, APIs, vector search, and full-stack integrations.", requiredSkills=["Python", "AI", "Embeddings", "Vector Search", "React", "API Integration", "Prompt Engineering"]),
    JobPayload(title="Technical Support Engineer", description="Troubleshoot customer issues, analyze logs, reproduce bugs, write SQL checks, and communicate technical solutions.", requiredSkills=["Troubleshooting", "SQL", "Linux", "Networking", "Communication", "API Testing"]),
]


@dataclass
class Metrics:
    samples: int
    top1: float
    top3: float
    mrr: float
    skill_precision: float | None
    skill_recall: float | None
    skill_samples: int


def split_skills(value: str) -> List[str]:
    return [skill.strip() for skill in (value or "").split(";") if skill.strip()]


def normalized_set(values: List[str]) -> set[str]:
    return {normalize_text(value) for value in values if normalize_text(value)}


def evaluate(dataset_path: str) -> Metrics:
    rows: List[Dict[str, str]] = []
    with open(dataset_path, "r", encoding="utf-8", newline="") as f:
        rows = list(csv.DictReader(f))

    top1_hits = 0
    top3_hits = 0
    reciprocal_ranks: List[float] = []
    precision_values: List[float] = []
    recall_values: List[float] = []

    for row in rows:
        resume_text = row["resume_text"]
        expected_role = row["expected_role"]
        expected_skills = normalized_set(split_skills(row.get("expected_skills", "")))

        response = match_jobs(MatchJobsRequest(resumeText=resume_text, profileText="", jobs=JOBS))
        top_matches = response["topMatches"] if isinstance(response, dict) else response.topMatches
        predictions = [match["title"] if isinstance(match, dict) else match.title for match in top_matches]

        if predictions and predictions[0] == expected_role:
            top1_hits += 1
        if expected_role in predictions[:3]:
            top3_hits += 1

        if expected_role in predictions:
            reciprocal_ranks.append(1.0 / (predictions.index(expected_role) + 1))
        else:
            reciprocal_ranks.append(0.0)

        predicted_skills = normalized_set(extract_skills_from_text(resume_text))
        if expected_skills:
            if predicted_skills:
                precision_values.append(len(predicted_skills & expected_skills) / len(predicted_skills))
            else:
                precision_values.append(0.0)
            recall_values.append(len(predicted_skills & expected_skills) / len(expected_skills))

    samples = len(rows)
    return Metrics(
        samples=samples,
        top1=top1_hits / samples if samples else 0.0,
        top3=top3_hits / samples if samples else 0.0,
        mrr=sum(reciprocal_ranks) / samples if samples else 0.0,
        skill_precision=sum(precision_values) / len(precision_values) if precision_values else None,
        skill_recall=sum(recall_values) / len(recall_values) if recall_values else None,
        skill_samples=len(recall_values),
    )


def main() -> None:
    parser = argparse.ArgumentParser(description="Evaluate job matching quality on a labeled benchmark CSV.")
    parser.add_argument("--dataset", default=os.path.join(os.path.dirname(__file__), "evaluation_dataset.csv"))
    parser.add_argument("--use-transformer", action="store_true", help="Use sentence-transformers instead of fast lexical fallback.")
    args = parser.parse_args()

    if args.use_transformer:
        os.environ["NLP_USE_TRANSFORMER"] = "true"
    _startup()

    metrics = evaluate(args.dataset)
    print(f"Samples: {metrics.samples}")
    print(f"Top-1 Accuracy: {metrics.top1:.2%}")
    print(f"Top-3 Accuracy: {metrics.top3:.2%}")
    print(f"Mean Reciprocal Rank: {metrics.mrr:.3f}")
    if metrics.skill_precision is None or metrics.skill_recall is None:
        print("Skill Precision: N/A (no manual skill labels in dataset)")
        print("Skill Recall: N/A (no manual skill labels in dataset)")
    else:
        print(f"Skill Precision: {metrics.skill_precision:.2%} over {metrics.skill_samples} labeled samples")
        print(f"Skill Recall: {metrics.skill_recall:.2%} over {metrics.skill_samples} labeled samples")


if __name__ == "__main__":
    main()
