#!/usr/bin/env python3
"""Construye el plan determinista de 130 corridas; no ejecuta ninguna carga."""
from __future__ import annotations

import argparse
import json
from pathlib import Path


def build_plan(seed: int = 20260904) -> list[dict]:
    rows = []
    for repetition in range(1, 11):
        rows.append({"scenario": "esc1", "strategy": "production", "repetition": repetition,
                     "users": 50, "duration": "5m", "simultaneous": False, "seed": seed + repetition,
                     "analysis": 2 <= repetition <= 9})
    for scenario, agents in (("esc2", 50), ("esc3", 200)):
        base = ["s0", "s1", "s2", "s3", "s4"]
        for repetition in range(1, 11):
            order = base if repetition % 2 else list(reversed(base))
            for position, strategy in enumerate(order):
                rows.append({"scenario": scenario, "strategy": strategy, "repetition": repetition,
                             "execution_order": position + 1, "users": agents, "simultaneous": True,
                             "seed": seed + repetition, "analysis": 2 <= repetition <= 9})
    for repetition in range(1, 11):
        order = ["s3", "s4"] if repetition % 2 else ["s4", "s3"]
        for position, strategy in enumerate(order):
            rows.append({"scenario": "esc4", "strategy": strategy, "repetition": repetition,
                         "execution_order": position + 1, "users": 200, "simultaneous": True,
                         "failure_at_fraction": 0.5, "seed": seed + repetition,
                         "analysis": 2 <= repetition <= 9})
    assert len(rows) == 130
    return rows


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--seed", type=int, default=20260904)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    args.output.write_text(json.dumps({"status": "PLANNED_NOT_EXECUTED", "runs": build_plan(args.seed)},
                                      indent=2), encoding="utf-8")


if __name__ == "__main__": main()
