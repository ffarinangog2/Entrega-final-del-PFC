#!/usr/bin/env python3
"""Genera boxplots y SHA256 únicamente a partir de evidencia real existente."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path


def manifests(raw: Path) -> list[dict]:
    required = {"run_id", "strategy", "scenario", "repetition", "seed", "sha", "records"}
    rows = []
    for path in sorted(raw.glob("*.json")):
        value = json.loads(path.read_text(encoding="utf-8"))
        missing = required - value.keys()
        if missing: raise ValueError(f"{path}: metadata incompleta: {sorted(missing)}")
        rows.append(value)
    return rows


def sha256sums(root: Path, output: Path) -> None:
    files = [p for p in root.rglob("*") if p.is_file() and p.resolve() != output.resolve()]
    lines = [f"{hashlib.sha256(path.read_bytes()).hexdigest()}  {path.relative_to(root).as_posix()}"
             for path in sorted(files)]
    output.write_text("\n".join(lines) + ("\n" if lines else ""), encoding="utf-8")


def boxplot(raw: Path, output: Path) -> None:
    rows = manifests(raw)
    if not rows: raise ValueError("No hay corridas reales; no se genera una figura vacía")
    try:
        import matplotlib.pyplot as plt
    except ImportError as exc:
        raise RuntimeError("matplotlib debe estar disponible en el entorno de análisis") from exc
    grouped: dict[str, list[float]] = {}
    for run in rows:
        key = f"{run['scenario']}-{run['strategy']}"
        grouped.setdefault(key, []).extend(r["latency_ms"] for r in run["records"] if r.get("type") == "REQUEST")
    output.parent.mkdir(parents=True, exist_ok=True)
    plt.figure(figsize=(max(8, len(grouped)), 5)); plt.boxplot(grouped.values(), tick_labels=grouped.keys())
    plt.ylabel("Latencia (ms)"); plt.xticks(rotation=45); plt.tight_layout(); plt.savefig(output, dpi=160); plt.close()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path("experimentos/resultados/arbiter"))
    parser.add_argument("--boxplot", action="store_true")
    parser.add_argument("--hashes", action="store_true")
    args = parser.parse_args()
    if args.boxplot: boxplot(args.root / "raw", args.root / "analysis" / "boxplot_latencia.png")
    if args.hashes: sha256sums(args.root, args.root / "SHA256SUMS")


if __name__ == "__main__": main()
