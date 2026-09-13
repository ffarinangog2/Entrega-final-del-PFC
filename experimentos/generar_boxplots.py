"""Genera boxplots: E1 desde GET raw delimitados y las demás métricas desde el CSV."""

from __future__ import annotations

import argparse
import csv
from pathlib import Path

try:
    from experimentos.analizar_iso25010 import (
        ANALYZED_REPETITIONS,
        EFFICIENCY_SCENARIO,
        VALID_VALUES,
        read_efficiency_population,
    )
except ModuleNotFoundError:  # Permite ejecutar directamente este archivo desde el repo.
    from analizar_iso25010 import (  # type: ignore[no-redef]
        ANALYZED_REPETITIONS,
        EFFICIENCY_SCENARIO,
        VALID_VALUES,
        read_efficiency_population,
    )


def load_plot_metrics(csv_path: Path, raw_root: Path) -> dict[str, list[float]]:
    """Prepara series sin reutilizar los percentiles Aggregated históricos de E1."""

    with csv_path.open(encoding="utf-8-sig", newline="") as source:
        rows = [
            row
            for row in csv.DictReader(source)
            if row.get("valida", "").strip().lower() in VALID_VALUES
        ]
    if not rows:
        raise ValueError("faltan mediciones válidas completas")

    efficiency_rows = [
        row
        for row in rows
        if row.get("escenario") == EFFICIENCY_SCENARIO
        and int(row["repeticion"]) in ANALYZED_REPETITIONS
    ]
    efficiency_repetitions = [int(row["repeticion"]) for row in efficiency_rows]
    efficiency_population = read_efficiency_population(raw_root, efficiency_repetitions)

    p95_values: list[float] = []
    p99_values: list[float] = []
    for row in rows:
        if row.get("escenario") == EFFICIENCY_SCENARIO:
            repetition = int(row["repeticion"])
            if repetition not in ANALYZED_REPETITIONS:
                continue
            measurement = efficiency_population[repetition]
            p95_values.append(float(measurement["p95_ms"]))
            p99_values.append(float(measurement["p99_ms"]))
        else:
            # E2 conserva por ahora exactamente sus valores procedentes del CSV.
            p95_values.append(float(row["p95_ms"]))
            p99_values.append(float(row["p99_ms"]))

    metrics = {
        "p95 (ms)": p95_values,
        "p99 (ms)": p99_values,
        "HTTP 5xx (%)": [float(row["failure_rate_percent"]) for row in rows],
    }
    if any(not values for values in metrics.values()):
        raise ValueError("faltan mediciones válidas completas")
    return metrics


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("csv_path", nargs="?", type=Path,
                        default=Path(__file__).parent / "resultados" / "iso25010.csv")
    parser.add_argument("--output", type=Path,
                        default=Path(__file__).parent / "resultados" / "boxplots.png")
    parser.add_argument(
        "--raw-root",
        type=Path,
        help="Raíz raw; por defecto, el directorio raw junto al CSV.",
    )
    args = parser.parse_args()
    try:
        import matplotlib.pyplot as plt
        raw_root = args.raw_root or args.csv_path.parent / "raw"
        metrics = load_plot_metrics(args.csv_path, raw_root)
        _, axes = plt.subplots(1, 3, figsize=(12, 4))
        for axis, (label, values) in zip(axes, metrics.items()):
            axis.boxplot(values, showmeans=True)
            axis.set_title(label)
            axis.grid(axis="y", alpha=.3)
        args.output.parent.mkdir(parents=True, exist_ok=True)
        plt.tight_layout()
        plt.savefig(args.output, dpi=160)
    except (OSError, ValueError, ImportError) as error:
        print(f"ERROR: {error}")
        return 2
    print(args.output)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
