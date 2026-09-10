"""Registra una repetición real verificada en la plantilla ISO 25010."""

from __future__ import annotations

import argparse
import csv
import json
import math
import os
import tempfile
from pathlib import Path


SCENARIOS = {"eficiencia_nominal_50u_5m", "fiabilidad_nominal_50u_1h"}
RELIABILITY_EVIDENCE = {
    "metadata.json",
    "environment.txt",
    "locust_stats.csv",
    "locust_stats_history.csv",
    "locust_failures.csv",
    "locust_exceptions.csv",
    "locust-report.html",
    "locust.log",
    "prometheus-5xx-count.promql",
    "prometheus-5xx-percent.promql",
    "prometheus-p95.promql",
    "prometheus-5xx-result.txt",
    "prometheus-5xx-percent-result.txt",
    "prometheus-p95-result.txt",
    "prometheus-health-before.txt",
    "prometheus-health-after.txt",
    "gateway-health-before.json",
    "gateway-health-after.json",
    "reservas-health-before.json",
    "reservas-health-after.json",
    "docker-stats-before.txt",
    "docker-stats-after.txt",
    "cockroach-containers-before.txt",
    "cockroach-containers-after.txt",
    "deployment-state-before.txt",
    "deployment-state-after.txt",
    "reservas-service.log",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--scenario", required=True, choices=sorted(SCENARIOS))
    parser.add_argument("--repetition", required=True, type=int, choices=range(1, 11))
    parser.add_argument("--total-requests", required=True, type=int)
    parser.add_argument("--http-5xx", required=True, type=int)
    parser.add_argument("--p95-ms", required=True, type=float)
    parser.add_argument("--p99-ms", required=True, type=float)
    parser.add_argument("--evidence-dir", required=True, type=Path)
    parser.add_argument("--observation", default="")
    parser.add_argument(
        "--csv-path",
        type=Path,
        default=Path(__file__).parent / "resultados" / "iso25010.csv",
    )
    return parser.parse_args()


def validate_evidence(args: argparse.Namespace) -> None:
    expected_suffix = Path(args.scenario) / f"rep-{args.repetition:02d}"
    if not args.evidence_dir.resolve().as_posix().endswith(expected_suffix.as_posix()):
        raise ValueError("La ruta de evidencia no coincide con escenario/repetición")
    required_names = {"metadata.json", "locust_stats.csv", "prometheus-5xx-result.txt"}
    if args.scenario == "fiabilidad_nominal_50u_1h":
        required_names = RELIABILITY_EVIDENCE
    for path in (args.evidence_dir / name for name in sorted(required_names)):
        if not path.is_file() or path.stat().st_size == 0:
            raise ValueError(f"Falta evidencia real: {path}")
    metadata_path = args.evidence_dir / "metadata.json"
    metadata = json.loads(metadata_path.read_text(encoding="utf-8-sig"))
    if metadata.get("scenario") != args.scenario or metadata.get("repetition") != args.repetition:
        raise ValueError("Los metadatos no coinciden con escenario/repetición")
    if args.scenario == "fiabilidad_nominal_50u_1h":
        if metadata.get("status") != "completed" or metadata.get("execution_completed") is not True:
            raise ValueError("La ejecución experimental no consta como completada")
        if metadata.get("duration_completed") is not True:
            raise ValueError("La ejecución no completó la duración planificada")
        if metadata.get("evidence_complete") is not True:
            raise ValueError("La recolección de evidencia no consta como completa")
        if metadata.get("environment_consistent") is not True:
            raise ValueError("El entorno o despliegue cambió durante la repetición")
        if metadata.get("git_worktree_clean_before") is not True:
            raise ValueError("El árbol Git no estaba limpio al iniciar la repetición")
        if metadata.get("users") != 50 or metadata.get("spawn_rate") != 10:
            raise ValueError("Los metadatos no corresponden a 50 usuarios y spawn-rate 10")
        if metadata.get("planned_duration") != "1h" or metadata.get("planned_duration_seconds") != 3600:
            raise ValueError("Los metadatos no corresponden a la duración oficial de una hora")
        if not isinstance(metadata.get("locust_exit_code"), int):
            raise ValueError("Falta el código de salida real de Locust")
        for field in (
            "started_at_utc", "finished_at_utc", "git_branch", "git_sha",
            "python_version", "locust_version", "deployment_fingerprint_before",
            "deployment_fingerprint_after",
        ):
            if not metadata.get(field):
                raise ValueError(f"Falta metadata obligatoria: {field}")
    elif metadata.get("status") != "completed" or metadata.get("exit_code") != 0:
        raise ValueError("La ejecución Locust no consta como completada correctamente")


def update_csv(args: argparse.Namespace) -> float:
    if args.total_requests <= 0:
        raise ValueError("total_requests debe ser mayor que cero")
    if args.http_5xx < 0 or args.http_5xx > args.total_requests:
        raise ValueError("http_5xx debe estar entre cero y total_requests")
    if not math.isfinite(args.p95_ms) or args.p95_ms < 0:
        raise ValueError("p95_ms debe ser un número real no negativo")
    if not math.isfinite(args.p99_ms) or args.p99_ms < args.p95_ms:
        raise ValueError("p99_ms debe ser válido y mayor o igual que p95_ms")
    validate_evidence(args)

    with args.csv_path.open(encoding="utf-8-sig", newline="") as csv_file:
        reader = csv.DictReader(csv_file)
        fieldnames = reader.fieldnames
        rows = list(reader)
    if not fieldnames:
        raise ValueError("El CSV no contiene encabezado")

    matches = [
        row
        for row in rows
        if row["escenario"] == args.scenario
        and int(row["repeticion"]) == args.repetition
    ]
    if len(matches) != 1:
        raise ValueError("La plantilla no contiene una única fila para la repetición")
    row = matches[0]
    measured_fields = ("total_requests", "failures", "failure_rate_percent", "p95_ms", "p99_ms", "valida")
    if any(row[field].strip() for field in measured_fields):
        raise ValueError("La fila ya contiene mediciones; no se sobrescribirá")

    failure_rate = 100.0 * args.http_5xx / args.total_requests
    row.update(
        total_requests=str(args.total_requests),
        failures=str(args.http_5xx),
        failure_rate_percent=f"{failure_rate:.6f}",
        p95_ms=f"{args.p95_ms:.6f}",
        p99_ms=f"{args.p99_ms:.6f}",
        valida="si",
        observacion=args.observation,
    )

    args.csv_path.parent.mkdir(parents=True, exist_ok=True)
    file_descriptor, temporary_name = tempfile.mkstemp(
        prefix="iso25010-", suffix=".csv", dir=args.csv_path.parent, text=True
    )
    try:
        with os.fdopen(file_descriptor, "w", encoding="utf-8", newline="") as csv_file:
            writer = csv.DictWriter(csv_file, fieldnames=fieldnames)
            writer.writeheader()
            writer.writerows(rows)
        os.replace(temporary_name, args.csv_path)
    except BaseException:
        Path(temporary_name).unlink(missing_ok=True)
        raise
    return failure_rate


def main() -> int:
    args = parse_args()
    try:
        failure_rate = update_csv(args)
    except (OSError, ValueError, json.JSONDecodeError) as error:
        print(f"ERROR: {error}")
        return 2
    print(
        f"Registrada {args.scenario} r{args.repetition}: "
        f"HTTP 5xx={failure_rate:.6f} %, p95={args.p95_ms:.6f} ms"
    )
    if args.repetition in (1, 10):
        print("La evidencia se conserva, pero esta repetición se excluye del análisis.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
