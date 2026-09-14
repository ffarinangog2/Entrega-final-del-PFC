#!/usr/bin/env python3
"""Deriva el censo HTTP ARBITER directamente de los manifiestos raw."""
from __future__ import annotations

import argparse
from collections import Counter
import json
from pathlib import Path
from typing import Any, Iterable


CENTRAL_REPETITIONS = range(2, 10)
COMPARISON_SCENARIOS = ("esc2", "esc3")
STRATEGIES = ("s0", "s1", "s2", "s3", "s4")


def raw_runs(raw: Path) -> Iterable[dict[str, Any]]:
    """Lee únicamente los manifiestos JSON de corrida, no resúmenes derivados."""
    for path in sorted(raw.glob("*.json")):
        yield json.loads(path.read_text(encoding="utf-8"))


def request_status(record: dict[str, Any]) -> int:
    """Recupera el HTTP preservado; una respuesta normal del endpoint es 200."""
    allocation = record.get("allocation") or {}
    response = allocation.get("backend_response") or {}
    if "httpStatus" in response:
        return int(response["httpStatus"])
    if allocation.get("status") == "HTTP_ERROR":
        raise ValueError("HTTP_ERROR sin httpStatus preservado")
    return 200


def central_http_census(raw: Path) -> dict[tuple[str, str], Counter]:
    """Cuenta la población r2-r9 usada por Esc-2/Esc-3 en las comparaciones."""
    result = {(scenario, strategy): Counter()
              for scenario in COMPARISON_SCENARIOS for strategy in STRATEGIES}
    for run in raw_runs(raw):
        scenario = run.get("scenario")
        strategy = run.get("strategy")
        repetition = int(run.get("repetition", 0))
        if scenario not in COMPARISON_SCENARIOS or repetition not in CENTRAL_REPETITIONS:
            continue
        key = (scenario, strategy)
        if key not in result:
            raise ValueError(f"Estrategia inesperada en raw: {key}")
        for record in run.get("records", []):
            if record.get("type") != "REQUEST":
                continue
            code = request_status(record)
            result[key]["requests"] += 1
            if 200 <= code < 300:
                result[key]["2xx"] += 1
            elif 400 <= code < 500:
                result[key]["4xx"] += 1
            elif 500 <= code < 600:
                result[key]["5xx"] += 1
            else:
                result[key]["other"] += 1
    return result


def rendered_census(raw: Path) -> list[dict[str, Any]]:
    rows = []
    for (scenario, strategy), counts in central_http_census(raw).items():
        errors = counts["4xx"] + counts["5xx"] + counts["other"]
        rows.append({"scenario": scenario, "strategy": strategy,
                     "requests": counts["requests"], "2xx": counts["2xx"],
                     "4xx": counts["4xx"], "5xx": counts["5xx"],
                     "other": counts["other"],
                     "errors": errors,
                     "error_percentage": 100 * errors / counts["requests"]})
    return rows


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("raw", type=Path)
    args = parser.parse_args()
    print(json.dumps(rendered_census(args.raw), indent=2, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
