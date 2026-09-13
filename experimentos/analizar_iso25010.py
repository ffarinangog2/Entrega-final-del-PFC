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
    "p99_ms",
    "valida",
    "observacion",
}
VALID_VALUES = {"1", "true", "si", "sí", "yes"}
T_CRITICAL_95_DF7 = 2.364624251
EXPECTED_REPETITIONS = set(range(1, 11))
ANALYZED_REPETITIONS = set(range(2, 10))
EFFICIENCY_SCENARIO = "eficiencia_nominal_50u_5m"
EFFICIENCY_REQUESTS = {
    ("GET", "GET /api/v1/reservas"),
    ("GET", "GET /api/v1/reservas/{id}"),
}
EFFICIENCY_EXCLUDED_REQUESTS = {
    ("POST", "POST /api/v1/auth/login"),
}
LOCUST_REQUIRED_COLUMNS = {
    "Type",
    "Name",
    "Request Count",
    "Failure Count",
    "95%",
    "99%",
}


def parse_args() -> argparse.Namespace:
    default_csv = Path(__file__).parent / "resultados" / "iso25010.csv"
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("csv_path", nargs="?", type=Path, default=default_csv)
    parser.add_argument(
        "--raw-root",
        type=Path,
        help="Raíz de evidencia raw; por defecto, el directorio raw junto al CSV.",
    )
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
        and row["p99_ms"].strip()
    ]


def summarize(values: list[float]) -> tuple[float, float, float, float]:
    mean = statistics.fmean(values)
    standard_deviation = statistics.stdev(values)
    margin = T_CRITICAL_95_DF7 * standard_deviation / math.sqrt(len(values))
    return mean, standard_deviation, mean - margin, mean + margin


def read_efficiency_population(
    raw_root: Path, repetitions: list[int]
) -> dict[int, dict[str, float | int | str]]:
    """Lee percentiles de GET de Reservas sin filtrar por estado ni latencia."""

    measurements: dict[int, dict[str, float | int | str]] = {}
    for repetition in repetitions:
        stats_path = (
            raw_root
            / EFFICIENCY_SCENARIO
            / f"rep-{repetition:02d}"
            / "locust_stats.csv"
        )
        with stats_path.open(encoding="utf-8-sig", newline="") as csv_file:
            reader = csv.DictReader(csv_file)
            missing = LOCUST_REQUIRED_COLUMNS.difference(reader.fieldnames or [])
            if missing:
                raise ValueError(
                    f"{stats_path}: faltan columnas Locust: {', '.join(sorted(missing))}"
                )
            stats_rows = list(reader)

        active_rows: list[tuple[dict[str, str], int]] = []
        for line_number, row in enumerate(stats_rows, start=2):
            try:
                request_count = int(row["Request Count"])
            except ValueError as error:
                raise ValueError(
                    f"{stats_path}:{line_number}: Request Count inválido"
                ) from error
            if request_count < 0:
                raise ValueError(
                    f"{stats_path}:{line_number}: Request Count no puede ser negativo"
                )
            if row["Name"] != "Aggregated" and request_count > 0:
                active_rows.append((row, line_number))

        unexpected = [
            (row["Type"], row["Name"])
            for row, _ in active_rows
            if (row["Type"], row["Name"])
            not in EFFICIENCY_REQUESTS | EFFICIENCY_EXCLUDED_REQUESTS
        ]
        if unexpected:
            raise ValueError(
                f"{stats_path}: requests activos sin clasificación poblacional: "
                + ", ".join(f"{method} {name}" for method, name in unexpected)
            )

        included = [
            (row, line_number)
            for row, line_number in active_rows
            if (row["Type"], row["Name"]) in EFFICIENCY_REQUESTS
        ]
        if not included:
            raise ValueError(
                f"{stats_path}: no hay observaciones de GET de Reservas/Solicitudes"
            )
        if len(included) > 1:
            names = ", ".join(row["Name"] for row, _ in included)
            raise ValueError(
                f"{stats_path}: hay varias filas GET activas ({names}); "
                "los percentiles por fila no permiten reconstruir el percentil conjunto "
                "sin una distribución raw combinable"
            )

        row, line_number = included[0]
        try:
            request_count = int(row["Request Count"])
            failure_count = int(row["Failure Count"])
            p95 = float(row["95%"])
            p99 = float(row["99%"])
        except ValueError as error:
            raise ValueError(
                f"{stats_path}:{line_number}: métricas poblacionales inválidas"
            ) from error
        if failure_count < 0 or failure_count > request_count:
            raise ValueError(
                f"{stats_path}:{line_number}: Failure Count inválido"
            )
        if p95 < 0 or p99 < p95:
            raise ValueError(f"{stats_path}:{line_number}: percentiles inválidos")

        measurements[repetition] = {
            "request": row["Name"],
            "line": line_number,
            "total_requests": request_count,
            "failures": failure_count,
            "p95_ms": p95,
            "p99_ms": p99,
            "source": str(stats_path),
        }
    return measurements


def analyze_scenario(
    scenario: str, rows: list[dict[str, str]], raw_root: Path
) -> None:
    validate_design(scenario, rows)
    selected = valid_measurements(rows)
    if len(selected) != 8:
        print(f"{scenario}: SIN DATOS SUFICIENTES ({len(selected)}/8 muestras válidas completas)")
        return

    failure_rates = [float(row["failure_rate_percent"]) for row in selected]
    historical_p95_values = [float(row["p95_ms"]) for row in selected]
    historical_p99_values = [float(row["p99_ms"]) for row in selected]
    p95_values = historical_p95_values
    p99_values = historical_p99_values
    total_requests = [int(row["total_requests"]) for row in selected]
    failures = [int(row["failures"]) for row in selected]
    if any(value < 0 for value in failure_rates + p95_values + p99_values):
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
    if scenario == EFFICIENCY_SCENARIO:
        repetitions = [int(row["_repetition"]) for row in selected]
        population = read_efficiency_population(raw_root, repetitions)
        p95_values = [float(population[repetition]["p95_ms"]) for repetition in repetitions]
        p99_values = [float(population[repetition]["p99_ms"]) for repetition in repetitions]
        print(
            "  procedencia oficial de percentiles: filas identificadas por Type=GET y "
            "Name=GET /api/v1/reservas o GET /api/v1/reservas/{id} en los "
            "locust_stats.csv raw"
        )
        print(
            "  exclusión poblacional: POST /api/v1/auth/login pertenece a Auth y no "
            "a las consultas de solo lectura de Reservas/Solicitudes; no se filtra "
            "por código HTTP, latencia ni éxito/fallo"
        )
        for repetition in repetitions:
            measurement = population[repetition]
            print(
                f"  r{repetition:02d}: {measurement['request']}; "
                f"n={measurement['total_requests']}; fallos Locust={measurement['failures']}; "
                f"p95={measurement['p95_ms']:.6f} ms; "
                f"p99={measurement['p99_ms']:.6f} ms; "
                f"fuente={measurement['source']}:{measurement['line']}"
            )
        print(
            "  análisis histórico no oficial para PI1: percentiles de la fila "
            "Aggregated (GET de Reservas/Solicitudes + POST /api/v1/auth/login)"
        )
        print_summary("historical_aggregated_p95_ms", historical_p95_values, 500.0, "ms")
        print_summary("historical_aggregated_p99_ms", historical_p99_values, 750.0, "ms")

    print_summary("failure_rate_percent", failure_rates, 1.0, "%")
    print_summary("p95_ms", p95_values, 500.0, "ms")
    print_summary("p99_ms", p99_values, 750.0, "ms")


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
        raw_root = args.raw_root or args.csv_path.parent / "raw"
        for scenario, rows in sorted(scenarios.items()):
            analyze_scenario(scenario, rows, raw_root)
    except (OSError, ValueError) as error:
        print(f"ERROR: {error}")
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
