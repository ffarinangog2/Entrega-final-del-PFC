"""Ejecuta una repetición de seguridad, mantenibilidad o compatibilidad E3."""

from __future__ import annotations

import argparse
import csv
import json
import os
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Any

from e3_instrumental import (
    atomic_json, command_version, copy_evidence, environment_metadata, git_identity,
    parse_playwright, prepare_repetition, run_capture, sha256_manifest, utc_now,
    verify_sha256_manifest,
)


REPO = Path(__file__).resolve().parents[1]
RAW = REPO / "experimentos" / "resultados" / "raw"
SECURITY_IDS = {
    "admin_login_permitido", "admin_login_invalido_401", "reservas_sin_token_401",
    "docente_login_permitido", "docente_crea_consulta_cancela",
    "admin_piso_scope_permitido", "admin_piso_fuera_scope_403",
}
COMPONENTS = {
    "auth": (REPO / "services/auth-service", 70.0),
    "usuarios": (REPO / "services/usuarios-service", 70.0),
    "academico": (REPO / "services/academico-laboratorios-service", 70.0),
    "reservas": (REPO / "services/reservas-solicitudes-service", 80.0),
    "gateway": (REPO / "services/api-gateway", 70.0),
    "web": (REPO / "apps/web", 70.0),
    "android": (REPO / "apps/mobile", 70.0),
}
MOTORS = ("chromium", "firefox", "webkit")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("campaign", choices=("seguridad", "mantenibilidad", "compatibilidad"))
    parser.add_argument("--repetition", type=int, required=True, choices=(1, 2, 3))
    parser.add_argument("--raw-root", type=Path, default=RAW)
    parser.add_argument("--environment-id", help="Identificador estable del despliegue/host")
    parser.add_argument("--dry-run", action="store_true")
    return parser.parse_args()


def executable(base: Path, wrapper: str, fallback: str) -> list[str]:
    windows = next((path for path in (base / f"{wrapper}.cmd", base / f"{wrapper}.bat")
                    if path.exists()), None)
    unix = base / wrapper
    if os.name == "nt" and windows is not None:
        return ["cmd", "/c", windows.name]
    if unix.exists():
        return [f"./{wrapper}"]
    return [fallback]


def planned_commands(campaign: str) -> list[str]:
    if campaign == "seguridad":
        return ["npm run --silent test:e2e -- --config=playwright.security-e3.config.ts --project=chromium --reporter=json"]
    if campaign == "compatibilidad":
        return [f"npm run --silent test:e2e -- --project={motor} --reporter=json" for motor in MOTORS]
    commands = []
    for name in ("auth", "usuarios", "academico", "reservas", "gateway"):
        base, _ = COMPONENTS[name]
        commands.append(" ".join(executable(base, "mvnw", "mvn") + ["--batch-mode", "clean", "verify"]))
    android, _ = COMPONENTS["android"]
    commands.extend(("npm run test:coverage", " ".join(executable(android, "gradlew", "gradle") +
                                                        ["clean", "testDebugUnitTest", "jacocoTestReport"])))
    return commands


def validate_required_env(names: tuple[str, ...]) -> None:
    missing = [name for name in names if not os.environ.get(name)]
    if missing:
        raise ValueError("Faltan variables obligatorias: " + ", ".join(missing))


def base_metadata(identity: dict[str, Any], repetition: int, campaign: str) -> dict[str, Any]:
    return {
        **identity, "campaign": campaign, "repetition": repetition,
        "started_at_utc": utc_now(), "status": "running", "evidence_complete": False,
        "environment": environment_metadata(REPO),
    }


def run_security(rep: Path, identity: dict[str, Any], repetition: int) -> None:
    validate_required_env((
        "GATEWAY_BASE_URL", "DEMO_DOCENTE_USERNAME", "DEMO_DOCENTE_PASSWORD",
        "DEMO_ADMIN_PISO_USERNAME", "DEMO_ADMIN_PISO_PASSWORD",
    ))
    metadata = base_metadata(identity, repetition, "seguridad_gateway")
    output = rep / "decisions"
    output.mkdir()
    env = os.environ.copy()
    env.update({"E3_SECURITY_OUTPUT_DIR": str(output), "E3_REPETITION": str(repetition),
                "PLAYWRIGHT_JSON_OUTPUT_NAME": str(rep / "playwright.json"),
                "PLAYWRIGHT_OUTPUT_DIR": str(rep / "test-results")})
    command = ["npm", "run", "--silent", "test:e2e", "--", "--config=playwright.security-e3.config.ts",
               "--project=chromium", "--reporter=json"]
    metadata["command"] = command
    metadata["gateway_base_url"] = os.environ["GATEWAY_BASE_URL"]
    code = run_capture(command, REPO / "apps/web", rep / "command.stdout.log",
                       rep / "playwright.stderr.log", env)
    attempts = sorted(output.glob("decisiones-attempt-*.json"))
    rows: list[dict[str, Any]] = []
    for attempt in attempts:
        rows.extend(json.loads(attempt.read_text(encoding="utf-8")))
    latest: dict[str, dict[str, Any]] = {}
    for row in rows:
        latest[row["decision_id"]] = row
    with (rep / "decisiones.csv").open("w", newline="", encoding="utf-8") as handle:
        fields = ["decision_id", "request", "endpoint", "method", "identity", "role",
                  "expected_http", "observed_http", "assertion_pass", "attempt"]
        writer = csv.DictWriter(handle, fieldnames=fields)
        writer.writeheader()
        writer.writerows([{key: row.get(key, "") for key in fields} for row in latest.values()])
    complete = set(latest) == SECURITY_IDS
    metadata.update({
        "finished_at_utc": utc_now(), "exit_code": code,
        "attempts_observed": len(attempts), "flaky": len(attempts) > 1,
        "decisions": len(latest), "evidence_complete": complete,
        "status": "completed" if code == 0 and complete else "failed",
    })
    atomic_json(rep / "manifest.json", metadata)
    sha256_manifest(rep)
    if code != 0 or not complete:
        raise RuntimeError("Seguridad terminó con fallo o matriz incompleta; se conservó el raw")


def jacoco_metrics(xml_path: Path) -> dict[str, float]:
    root = ET.parse(xml_path).getroot()
    values: dict[str, float] = {}
    for counter in root.findall("counter"):
        name = counter.attrib["type"].lower()
        missed, covered = int(counter.attrib["missed"]), int(counter.attrib["covered"])
        total = missed + covered
        values[name] = 100.0 * covered / total if total else 0.0
    return values


def web_metrics(path: Path) -> dict[str, float]:
    total = json.loads(path.read_text(encoding="utf-8"))["total"]
    return {name: float(total[name]["pct"]) for name in ("lines", "branches", "functions", "statements")}


def run_maintenance(rep: Path, identity: dict[str, Any], repetition: int) -> None:
    metadata = base_metadata(identity, repetition, "mantenibilidad")
    outcomes: dict[str, Any] = {}
    rows: list[dict[str, Any]] = []
    for name, (base, threshold) in COMPONENTS.items():
        destination = rep / name
        destination.mkdir()
        if name in {"auth", "usuarios", "academico", "reservas", "gateway"}:
            command = executable(base, "mvnw", "mvn") + ["--batch-mode", "clean", "verify"]
            report = base / "target/site/jacoco/jacoco.xml"
            report_root = base / "target/site/jacoco"
        elif name == "web":
            command = ["npm", "run", "test:coverage"]
            report = base / "coverage/coverage-summary.json"
            report_root = base / "coverage"
        else:
            command = executable(base, "gradlew", "gradle") + [
                "clean", "testDebugUnitTest", "jacocoTestReport"
            ]
            report = base / "app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"
            report_root = base / "app/build/reports/jacoco/jacocoTestReport"
        code = run_capture(command, base, destination / "command.stdout.log",
                           destination / "command.stderr.log")
        try:
            metrics = web_metrics(report) if name == "web" else jacoco_metrics(report)
            copy_evidence(report_root, destination / "report")
        except (FileNotFoundError, KeyError, ET.ParseError, json.JSONDecodeError) as error:
            metrics = {}
            (destination / "extraction-error.txt").write_text(str(error), encoding="utf-8")
        version_command = command[:3] + ["--version"] if command[:2] == ["cmd", "/c"] else command[:1] + ["--version"]
        outcomes[name] = {"command": command, "exit_code": code, "metrics": metrics,
                          "tool_version": command_version(version_command, base)}
        selected = ["lines"]
        if name == "reservas": selected.append("branch")
        if name == "web": selected.extend(("branches", "functions", "statements"))
        aliases = {"branch": "branch", "branches": "branches"}
        for metric in selected:
            source = aliases.get(metric, metric)
            if source in metrics:
                metric_threshold = 48.0 if name == "reservas" and metric == "branch" else (70.0 if metric != "lines" or name != "reservas" else threshold)
                rows.append({"component": name, "metric": metric,
                             "percentage": metrics[source], "threshold": metric_threshold,
                             "exit_code": code})
    with (rep / "metricas.csv").open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=("component", "metric", "percentage", "threshold", "exit_code"))
        writer.writeheader(); writer.writerows(rows)
    complete = len(rows) == 11 and all(item["exit_code"] == 0 for item in outcomes.values())
    metadata.update({"finished_at_utc": utc_now(), "components": outcomes,
                     "evidence_complete": len(rows) == 11,
                     "status": "completed" if complete else "failed",
                     "exit_code": 0 if complete else 1})
    atomic_json(rep / "manifest.json", metadata); sha256_manifest(rep)
    if not complete:
        raise RuntimeError("Mantenibilidad terminó con fallo o evidencia incompleta; se conservó el raw")


def run_compatibility(rep: Path, identity: dict[str, Any], repetition: int) -> None:
    metadata = base_metadata(identity, repetition, "compatibilidad_web")
    results: dict[str, Any] = {}
    for motor in MOTORS:
        target = rep / motor
        target.mkdir()
        env = os.environ.copy()
        env["PLAYWRIGHT_OUTPUT_DIR"] = str(target / "test-results")
        env["PLAYWRIGHT_JSON_OUTPUT_NAME"] = str(target / "playwright.json")
        command = ["npm", "run", "--silent", "test:e2e", "--", f"--project={motor}", "--reporter=json"]
        code = run_capture(command, REPO / "apps/web", target / "command.stdout.log",
                           target / "playwright.stderr.log", env)
        try:
            report = json.loads((target / "playwright.json").read_text(encoding="utf-8"))
            counts = parse_playwright(report)
        except (OSError, json.JSONDecodeError, KeyError) as error:
            counts = {"total": 0, "passed": 0, "failed": 1, "skipped": 0,
                      "flaky": 0, "interrupted": 0, "parse_error": str(error)}
        counts.update({"exit_code": code, "command": command})
        atomic_json(target / "summary.json", counts)
        results[motor] = counts
    complete = all(value["total"] > 0 for value in results.values())
    operational_pass = complete and all(
        value["exit_code"] == 0 and value["failed"] == 0 and value["skipped"] == 0
        and value["flaky"] == 0 for value in results.values()
    )
    metadata.update({"finished_at_utc": utc_now(), "motors": results,
                     "evidence_complete": complete, "operational_pass": operational_pass,
                     "status": "completed" if complete else "failed",
                     "exit_code": 0 if operational_pass else 1})
    atomic_json(rep / "manifest.json", metadata); sha256_manifest(rep)
    if not complete:
        raise RuntimeError("Compatibilidad produjo evidencia incompleta; se conservó el raw")


def main() -> int:
    args = parse_args()
    commands = planned_commands(args.campaign)
    if args.dry_run:
        print(json.dumps({"campaign": args.campaign, "repetition": args.repetition,
                          "commands": commands, "creates_raw": False}, ensure_ascii=False, indent=2))
        return 0
    if not args.environment_id:
        raise ValueError("--environment-id es obligatorio en una ejecución medida")
    if args.campaign == "seguridad":
        validate_required_env((
            "GATEWAY_BASE_URL", "DEMO_DOCENTE_USERNAME", "DEMO_DOCENTE_PASSWORD",
            "DEMO_ADMIN_PISO_USERNAME", "DEMO_ADMIN_PISO_PASSWORD",
        ))
    identity = git_identity(
        REPO, require_clean=True,
        allowed_dirty_prefixes=("experimentos/resultados/raw/e3_",),
    )
    if identity["git_branch"] != "feature/entrega-4":
        raise ValueError("La campaña E3 está fijada a la rama feature/entrega-4")
    identity["environment_id"] = args.environment_id
    study_path = args.raw_root / "e3_study.json"
    study_identity = {"git_sha": identity["git_sha"], "git_branch": identity["git_branch"]}
    if study_path.exists():
        if json.loads(study_path.read_text(encoding="utf-8")) != study_identity:
            raise ValueError("El SHA o la rama difieren de las otras campañas E3")
    else:
        atomic_json(study_path, study_identity)
    parameters = {"repetitions": 3, "commands": commands,
                  "environment_id": args.environment_id}
    if args.campaign == "seguridad":
        parameters["gateway_base_url"] = os.environ["GATEWAY_BASE_URL"]
    root = args.raw_root / f"e3_{args.campaign}"
    for existing in sorted(root.glob("rep-*")):
        if existing.is_dir():
            verify_sha256_manifest(existing)
    rep = prepare_repetition(root, args.repetition, identity, parameters)
    try:
        if args.campaign == "seguridad": run_security(rep, identity, args.repetition)
        elif args.campaign == "mantenibilidad": run_maintenance(rep, identity, args.repetition)
        else: run_compatibility(rep, identity, args.repetition)
    except Exception as error:
        print(str(error), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
