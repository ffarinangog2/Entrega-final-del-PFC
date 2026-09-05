"""Genera boxplots reproducibles de p95, p99 y HTTP 5xx desde el CSV final."""

from __future__ import annotations

import argparse
import csv
from pathlib import Path


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("csv_path", nargs="?", type=Path,
                        default=Path(__file__).parent / "resultados" / "iso25010.csv")
    parser.add_argument("--output", type=Path,
                        default=Path(__file__).parent / "resultados" / "boxplots.png")
    args = parser.parse_args()
    try:
        import matplotlib.pyplot as plt
        with args.csv_path.open(encoding="utf-8-sig", newline="") as source:
            rows = [row for row in csv.DictReader(source)
                    if row.get("valida", "").strip().lower() in {"si", "sí", "true", "1"}]
        metrics = {
            "p95 (ms)": [float(row["p95_ms"]) for row in rows],
            "p99 (ms)": [float(row["p99_ms"]) for row in rows],
            "HTTP 5xx (%)": [float(row["failure_rate_percent"]) for row in rows],
        }
        if not rows or any(not values for values in metrics.values()):
            raise ValueError("faltan mediciones válidas completas")
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
