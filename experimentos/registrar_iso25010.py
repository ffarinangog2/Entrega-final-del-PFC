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


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--scenario", required=True, choices=sorted(SCENARIOS))
    parser.add_argument("--repetition", required=True, type=int, choices=range(1, 11))
    parser.add_argument("--total-requests", required=True, type=int)
    parser.add_argument("--http-5xx", required=True, type=int)
    parser.add_argument("--p95-ms", required=True, type=float)
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
    metadata_path = args.evidence_dir / "metadata.json"
    stats_path = args.evidence_dir / "locust_stats.csv"
    prometheus_path = args.evidence_dir / "prometheus-5xx-result.txt"
    for path in (metadata_path, stats_path, prometheus_path):
        if not path.is_file() or path.stat().st_size == 0:
            raise ValueError(f"Falta evidencia real: {path}")
    metadata = json.loads(metadata_path.read_text(encoding="utf-8-sig"))
    if metadata.get("status") != "completed" or metadata.get("exit_code") != 0:
        raise ValueError("La ejecución Locust no consta como completada correctamente")
    if metadata.get("scenario") != args.scenario or metadata.get("repetition") != args.repetition:
        raise ValueError("Los metadatos no coinciden con escenario/repetición")


def update_csv(args: argparse.Namespace) -> float:
    if args.total_requests <= 0:
        raise ValueError("total_requests debe ser mayor que cero")
    if args.http_5xx < 0 or args.http_5xx > args.total_requests:
        raise ValueError("http_5xx debe estar entre cero y total_requests")
    if not math.isfinite(args.p95_ms) or args.p95_ms < 0:
        raise ValueError("p95_ms debe ser un número real no negativo")
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
    measured_fields = ("total_requests", "failures", "failure_rate_percent", "p95_ms", "valida")
    if any(row[field].strip() for field in measured_fields):
        raise ValueError("La fila ya contiene mediciones; no se sobrescribirá")

    failure_rate = 100.0 * args.http_5xx / args.total_requests
    row.update(
        total_requests=str(args.total_requests),
        failures=str(args.http_5xx),
        failure_rate_percent=f"{failure_rate:.6f}",
        p95_ms=f"{args.p95_ms:.6f}",
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
