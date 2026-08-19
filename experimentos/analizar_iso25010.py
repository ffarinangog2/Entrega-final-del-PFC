"""Calcula estadísticas ISO 25010 solo a partir de mediciones reales completas."""

from __future__ import annotations

import argparse
import csv
import math
import statistics
from collections import defaultdict
from pathlib import Path


REQUIRED_COLUMNS = {
    "escenario",
    "repeticion",
    "usuarios",
    "duracion",
    "total_requests",
    "failures",
    "failure_rate_percent",
    "p95_ms",
    "valida",
    "observacion",
}
VALID_VALUES = {"1", "true", "si", "sí", "yes"}
T_CRITICAL_95_DF7 = 2.364624251
EXPECTED_REPETITIONS = set(range(1, 11))
ANALYZED_REPETITIONS = set(range(2, 10))


def parse_args() -> argparse.Namespace:
    default_csv = Path(__file__).parent / "resultados" / "iso25010.csv"
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("csv_path", nargs="?", type=Path, default=default_csv)
    return parser.parse_args()


def read_rows(csv_path: Path) -> dict[str, list[dict[str, str]]]:
    with csv_path.open(encoding="utf-8-sig", newline="") as csv_file:
        reader = csv.DictReader(csv_file)
        missing = REQUIRED_COLUMNS.difference(reader.fieldnames or [])
        if missing:
            raise ValueError(f"Faltan columnas requeridas: {', '.join(sorted(missing))}")

        scenarios: dict[str, list[dict[str, str]]] = defaultdict(list)
        for line_number, row in enumerate(reader, start=2):
            scenario = row["escenario"].strip()
            if not scenario:
                raise ValueError(f"Fila {line_number}: escenario vacío")
            try:
                repetition = int(row["repeticion"])
            except ValueError as error:
                raise ValueError(f"Fila {line_number}: repetición inválida") from error
            if repetition not in EXPECTED_REPETITIONS:
                raise ValueError(f"Fila {line_number}: repetición fuera del rango 1..10")
            row["_repetition"] = str(repetition)
            scenarios[scenario].append(row)
    return scenarios


def validate_design(scenario: str, rows: list[dict[str, str]]) -> None:
    repetitions = [int(row["_repetition"]) for row in rows]
    if len(repetitions) != len(set(repetitions)):
        raise ValueError(f"{scenario}: existen repeticiones duplicadas")
    missing = EXPECTED_REPETITIONS.difference(repetitions)
    if missing:
        raise ValueError(f"{scenario}: faltan repeticiones {sorted(missing)}")


def valid_measurements(rows: list[dict[str, str]]) -> list[dict[str, str]]:
    return [
        row
        for row in rows
        if int(row["_repetition"]) in ANALYZED_REPETITIONS
        and row["valida"].strip().lower() in VALID_VALUES
        and row["total_requests"].strip()
        and row["failures"].strip()
        and row["failure_rate_percent"].strip()
        and row["p95_ms"].strip()
    ]


def summarize(values: list[float]) -> tuple[float, float, float, float]:
    mean = statistics.fmean(values)
    standard_deviation = statistics.stdev(values)
    margin = T_CRITICAL_95_DF7 * standard_deviation / math.sqrt(len(values))
    return mean, standard_deviation, mean - margin, mean + margin


def analyze_scenario(scenario: str, rows: list[dict[str, str]]) -> None:
    validate_design(scenario, rows)
    selected = valid_measurements(rows)
    if len(selected) != 8:
        print(f"{scenario}: SIN DATOS SUFICIENTES ({len(selected)}/8 muestras válidas completas)")
        return

    failure_rates = [float(row["failure_rate_percent"]) for row in selected]
    p95_values = [float(row["p95_ms"]) for row in selected]
    total_requests = [int(row["total_requests"]) for row in selected]
    failures = [int(row["failures"]) for row in selected]
    if any(value < 0 for value in failure_rates + p95_values):
        raise ValueError(f"{scenario}: las métricas no pueden ser negativas")
    for row, total, failed, rate in zip(selected, total_requests, failures, failure_rates):
        repetition = row["_repetition"]
        if total <= 0 or failed < 0 or failed > total:
            raise ValueError(f"{scenario} r{repetition}: requests/failures inválidos")
        calculated_rate = 100.0 * failed / total
        if not math.isclose(rate, calculated_rate, abs_tol=0.01):
            raise ValueError(
                f"{scenario} r{repetition}: failure_rate_percent no coincide con "
                "100 * failures / total_requests"
            )

    print(f"{scenario}: 8 muestras válidas (repeticiones 2..9)")
    print_summary("failure_rate_percent", failure_rates, 1.0, "%")
    print_summary("p95_ms", p95_values, 500.0, "ms")


def print_summary(name: str, values: list[float], threshold: float, unit: str) -> None:
    mean, standard_deviation, lower, upper = summarize(values)
    compliance = "CUMPLE" if upper < threshold else "NO CUMPLE"
    print(
        f"  {name}: media={mean:.6f} {unit}; s={standard_deviation:.6f} {unit}; "
        f"IC95=[{lower:.6f}, {upper:.6f}] {unit}; {compliance} (< {threshold:g} {unit})"
    )


def main() -> int:
    args = parse_args()
    try:
        scenarios = read_rows(args.csv_path)
        if not scenarios:
            print("SIN DATOS: la plantilla no contiene escenarios")
            return 0
        for scenario, rows in sorted(scenarios.items()):
            analyze_scenario(scenario, rows)
    except (OSError, ValueError) as error:
        print(f"ERROR: {error}")
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
