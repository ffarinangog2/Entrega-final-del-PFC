"""Utilidades compartidas por las campañas E3 prerregistradas."""

from __future__ import annotations

import hashlib
import json
import math
import os
import platform
import shutil
import statistics
import subprocess
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Iterable


REPETITIONS = (1, 2, 3)
WILSON_Z = 1.959963985
T_DF2 = 4.302652730


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")


def git(repo: Path, *args: str) -> str:
    result = subprocess.run(
        ["git", *args], cwd=repo, text=True, capture_output=True, check=True
    )
    return result.stdout.strip()


def git_identity(
    repo: Path, require_clean: bool = True,
    allowed_dirty_prefixes: tuple[str, ...] = (),
) -> dict[str, Any]:
    status = git(repo, "status", "--porcelain")
    dirty_lines = status.splitlines()
    disallowed = []
    for line in dirty_lines:
        path = line[3:].replace("\\", "/")
        if not any(path.startswith(prefix) for prefix in allowed_dirty_prefixes):
            disallowed.append(line)
    if require_clean and disallowed:
        raise ValueError("El árbol Git debe estar limpio antes de medir")
    return {
        "git_sha": git(repo, "rev-parse", "HEAD"),
        "git_branch": git(repo, "branch", "--show-current"),
        "git_worktree_clean": not bool(dirty_lines),
        "source_tree_clean": not bool(disallowed),
        "allowed_evidence_changes": [line for line in dirty_lines if line not in disallowed],
    }


def ensure_repetition(value: int) -> None:
    if value not in REPETITIONS:
        raise ValueError("La repetición debe ser 1, 2 o 3")


def prepare_repetition(
    root: Path,
    repetition: int,
    identity: dict[str, Any],
    parameters: dict[str, Any],
) -> Path:
    """Fija SHA/parámetros de campaña y crea una repetición sin sobrescribir."""
    ensure_repetition(repetition)
    campaign = root / "campaign.json"
    expected = {**identity, "parameters": parameters, "repetitions": list(REPETITIONS)}
    if campaign.exists():
        current = json.loads(campaign.read_text(encoding="utf-8"))
        for key in ("git_sha", "git_branch", "parameters", "repetitions"):
            if current.get(key) != expected.get(key):
                raise ValueError(f"La campaña existente no coincide en {key}")
    else:
        root.mkdir(parents=True, exist_ok=True)
        atomic_json(campaign, {**expected, "created_at_utc": utc_now()})
    repetition_dir = root / f"rep-{repetition:02d}"
    if repetition_dir.exists():
        raise FileExistsError(f"No se sobrescribe evidencia existente: {repetition_dir}")
    repetition_dir.mkdir()
    return repetition_dir


def atomic_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(
        json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    temporary.replace(path)


def run_capture(
    command: list[str], cwd: Path, stdout: Path, stderr: Path,
    env: dict[str, str] | None = None,
) -> int:
    stdout.parent.mkdir(parents=True, exist_ok=True)
    with stdout.open("wb") as out, stderr.open("wb") as err:
        completed = subprocess.run(command, cwd=cwd, env=env, stdout=out, stderr=err)
    return completed.returncode


def command_version(command: list[str], cwd: Path) -> str:
    try:
        result = subprocess.run(
            command, cwd=cwd, text=True, capture_output=True, timeout=30, check=False
        )
        return (result.stdout or result.stderr).strip()
    except (OSError, subprocess.TimeoutExpired) as error:
        return f"UNAVAILABLE: {error}"


def environment_metadata(repo: Path) -> dict[str, Any]:
    return {
        "os": platform.platform(),
        "python": platform.python_version(),
        "java": command_version(["java", "-version"], repo),
        "node": command_version(["node", "--version"], repo),
        "npm": command_version(["npm", "--version"], repo),
        "maven": command_version(["mvn", "--version"], repo),
        "gradle_wrapper": command_version(
            (["cmd", "/c", "gradlew.bat", "--version"] if os.name == "nt"
             else ["./gradlew", "--version"]),
            repo / "apps" / "mobile",
        ),
    }


def sha256_manifest(root: Path) -> None:
    rows: list[str] = []
    target = root / "SHA256SUMS"
    for path in sorted(p for p in root.rglob("*") if p.is_file() and p != target):
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        rows.append(f"{digest}  {path.relative_to(root).as_posix()}")
    target.write_text("\n".join(rows) + "\n", encoding="utf-8")


def verify_sha256_manifest(root: Path) -> None:
    target = root / "SHA256SUMS"
    if not target.is_file():
        raise ValueError(f"Falta SHA256SUMS en evidencia existente: {root}")
    expected: dict[str, str] = {}
    for line in target.read_text(encoding="utf-8").splitlines():
        digest, relative = line.split("  ", 1)
        expected[relative] = digest
    actual_paths = {
        path.relative_to(root).as_posix(): path
        for path in root.rglob("*") if path.is_file() and path != target
    }
    if set(actual_paths) != set(expected):
        raise ValueError(f"El inventario raw cambió en {root}")
    for relative, path in actual_paths.items():
        if hashlib.sha256(path.read_bytes()).hexdigest() != expected[relative]:
            raise ValueError(f"El raw fue modificado: {path}")


def copy_evidence(source: Path, target: Path) -> None:
    if not source.exists():
        raise FileNotFoundError(f"Falta el reporte esperado: {source}")
    if source.is_dir():
        shutil.copytree(source, target)
    else:
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, target)


def wilson(successes: int, total: int, z: float = WILSON_Z) -> tuple[float, float]:
    if total <= 0 or not 0 <= successes <= total:
        raise ValueError("Conteos no válidos para Wilson")
    p = successes / total
    denominator = 1 + z * z / total
    centre = p + z * z / (2 * total)
    radius = z * math.sqrt(p * (1 - p) / total + z * z / (4 * total * total))
    return (centre - radius) / denominator, (centre + radius) / denominator


def student_n3(values: Iterable[float]) -> dict[str, float]:
    sample = list(values)
    if len(sample) != 3:
        raise ValueError("El IC t prerregistrado exige exactamente n=3")
    mean = statistics.mean(sample)
    deviation = statistics.stdev(sample)
    margin = T_DF2 * deviation / math.sqrt(3)
    return {"mean": mean, "sample_sd": deviation, "ci95_low": mean - margin,
            "ci95_high": mean + margin}


def parse_playwright(report: dict[str, Any]) -> dict[str, int]:
    tests: list[dict[str, Any]] = []

    def visit_suite(suite: dict[str, Any]) -> None:
        for spec in suite.get("specs", []):
            tests.extend(spec.get("tests", []))
        for child in suite.get("suites", []):
            visit_suite(child)

    for suite in report.get("suites", []):
        visit_suite(suite)
    counts = {"total": len(tests), "passed": 0, "failed": 0, "skipped": 0,
              "flaky": 0, "interrupted": 0}
    for test in tests:
        results = test.get("results", [])
        statuses = [item.get("status") for item in results]
        final = statuses[-1] if statuses else "skipped"
        flaky = len(statuses) > 1 and any(status != "passed" for status in statuses[:-1])
        flaky = flaky or test.get("status") == "flaky"
        if flaky:
            counts["flaky"] += 1
        if final == "passed":
            counts["passed"] += 1
        elif final == "skipped":
            counts["skipped"] += 1
        elif final == "interrupted":
            counts["interrupted"] += 1
            counts["failed"] += 1
        else:
            counts["failed"] += 1
    return counts
