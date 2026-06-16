import argparse
import csv
import os
import re
from pathlib import Path
from typing import Iterable, List, Tuple

try:
    import docx
except Exception as exc:
    raise SystemExit("python-docx is required. Run: python -m pip install python-docx") from exc

from app.main import extract_skills_from_text


NAME_ROLE_RULES: List[Tuple[str, List[str]]] = [
    ("QA Automation Engineer", [" qa", "_qa", "-qa", "testing", "tester", "quality assurance"]),
    ("DevOps Engineer", ["devops", "ci cd", "jenkins", "docker", "kubernetes"]),
    ("Data Analyst", ["data analyst", "power bi", "tableau", "excel analyst"]),
    ("Data Scientist / NLP Engineer", ["hadoop", "data scientist", "nlp", "machine learning", "spark"]),
    ("Java Backend Developer", ["sr java", "java developer", "j2ee", "java dev", "_java", " java"]),
    ("Full Stack Developer", ["fullstack", "full stack", "php", "ui dev", "developer"]),
    ("Mobile App Developer", ["mobile", "android", "ios", "react native"]),
    ("Cybersecurity Analyst", ["cybersecurity", "security analyst", "owasp"]),
    ("Database Developer", ["database", "sql developer", "dba", "oracle"]),
    ("Product Analyst", ["business analyst", " bsa", "_bsa", "-bsa", " ba", "_ba", "-ba", "scrum", "project manager", "pmp", " pm"]),
]

CONTENT_ROLE_RULES: List[Tuple[str, List[str]]] = [
    ("DevOps Engineer", ["devops", "kubernetes", "docker", "jenkins", "github actions"]),
    ("Data Scientist / NLP Engineer", ["hadoop", "spark", "machine learning", "nlp", "data science"]),
    ("Java Backend Developer", ["spring boot", "j2ee", "java developer", "hibernate", "microservices"]),
    ("Database Developer", ["query optimization", "database administrator", "sql developer", "stored procedure"]),
    ("Product Analyst", ["business analyst", "requirements gathering", "scrum master", "project manager"]),
    ("QA Automation Engineer", ["selenium webdriver", "test automation", "quality assurance", "api testing"]),
    ("Cloud Engineer", ["aws", "azure", "cloud infrastructure"]),
    ("Mobile App Developer", ["android", "ios", "react native"]),
]


def read_docx(path: Path) -> str:
    document = docx.Document(str(path))
    parts = [paragraph.text for paragraph in document.paragraphs if paragraph.text.strip()]
    return "\n".join(parts)


def redact(text: str) -> str:
    text = re.sub(r"\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b", "[EMAIL]", text, flags=re.I)
    text = re.sub(r"\b(?:\+?\d[\s().-]*){10,}\b", "[PHONE]", text)
    text = re.sub(r"https?://\S+|www\.\S+", "[URL]", text, flags=re.I)
    return text


def infer_role(path: Path, text: str) -> str:
    name = " " + re.sub(r"[_().-]+", " ", path.stem.lower()) + " "
    for role, keywords in NAME_ROLE_RULES:
        if any(keyword in name for keyword in keywords):
            return role

    content = text[:2500].lower()
    for role, keywords in CONTENT_ROLE_RULES:
        if any(keyword in content for keyword in keywords):
            return role
    return "Technical Support Engineer"


def iter_resume_files(source_dir: Path) -> Iterable[Path]:
    for path in sorted(source_dir.rglob("*")):
        if path.suffix.lower() == ".docx":
            yield path


def main() -> None:
    parser = argparse.ArgumentParser(description="Import real DOCX resumes into the project evaluation dataset.")
    parser.add_argument("--source", required=True, help="Folder containing downloaded resume .docx files.")
    parser.add_argument("--output", default="evaluation_dataset.csv", help="Output CSV path.")
    parser.add_argument("--max", type=int, default=0, help="Optional maximum number of resumes to import.")
    args = parser.parse_args()

    source_dir = Path(args.source)
    output_path = Path(args.output)
    if not source_dir.exists():
        raise SystemExit(f"Source folder not found: {source_dir}")

    rows = []
    for path in iter_resume_files(source_dir):
        if args.max and len(rows) >= args.max:
            break
        try:
            text = redact(read_docx(path))
        except Exception:
            continue
        if len(text.strip()) < 100:
            continue

        role = infer_role(path, text)
        rows.append({
            "resume_text": " ".join(text.split()),
            "expected_role": role,
            "expected_skills": "",
        })

    output_path.parent.mkdir(parents=True, exist_ok=True)
    with open(output_path, "w", encoding="utf-8", newline="") as f:
      writer = csv.DictWriter(f, fieldnames=["resume_text", "expected_role", "expected_skills"])
      writer.writeheader()
      writer.writerows(rows)

    counts = {}
    for row in rows:
        counts[row["expected_role"]] = counts.get(row["expected_role"], 0) + 1

    print(f"Imported resumes: {len(rows)}")
    for role, count in sorted(counts.items()):
        print(f"{role}: {count}")


if __name__ == "__main__":
    main()
