#!/usr/bin/env python3
"""Análisis pre-registrado para resultados ARBITER; no genera datos sintéticos."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from arbiter_core import a12, descriptive, mann_whitney


FAVORABLE_DIRECTION = {
    "double_allocation_rate": "lower",
    "unnecessary_rejections": "lower",
    "latency_ms": "lower",
    "recovery_ms": "lower",
    "jain_users": "higher",
    "jain_equipment": "higher",
}


def valid_samples(rows: list[dict]) -> list[dict]:
    """Conserva evidencia completa, pero analiza repeticiones 2..9."""
    return [r for r in rows if 2 <= int(r["repetition"]) <= 9 and r.get("valid", True)]


def compare(rows: list[dict], metric: str, group_a: str, group_b: str) -> dict:
    if metric not in FAVORABLE_DIRECTION: raise ValueError("Métrica sin dirección pre-registrada")
    samples = valid_samples(rows)
    a = [float(r[metric]) for r in samples if r["strategy"] == group_a]
    b = [float(r[metric]) for r in samples if r["strategy"] == group_b]
    u, p = mann_whitney(a, b)
    return {"metric": metric, "group_a": group_a, "group_b": group_b,
            "favorable_direction": FAVORABLE_DIRECTION[metric],
            "group_a_summary": descriptive(a), "group_b_summary": descriptive(b),
            "mann_whitney_u_a": u, "p_two_sided_normal_approximation": p,
            "a12_probability_a_greater_than_b": a12(a, b)}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("input", type=Path, help="JSON con resumen de corridas reales")
    parser.add_argument("--metric", required=True, choices=sorted(FAVORABLE_DIRECTION))
    parser.add_argument("--group-a", required=True)
    parser.add_argument("--group-b", required=True)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    rows = json.loads(args.input.read_text(encoding="utf-8"))
    result = compare(rows, args.metric, args.group_a, args.group_b)
    rendered = json.dumps(result, indent=2)
    if args.output: args.output.write_text(rendered, encoding="utf-8")
    else: print(rendered)


if __name__ == "__main__": main()
