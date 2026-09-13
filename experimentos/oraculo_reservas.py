#!/usr/bin/env python3
"""Oráculo automático de los cinco invariantes oficiales del experimento."""
from __future__ import annotations

import argparse
from collections import Counter, defaultdict
import json
from pathlib import Path

from arbiter_core import jain, overlaps, SlotRequest, wilson_interval


def allocations(manifest: dict) -> list[dict]:
    return [r["allocation"] for r in manifest.get("records", []) if r.get("type") == "REQUEST"]


def evaluate(manifest: dict, equipment_fixture: dict, events: list[dict] | None = None) -> dict:
    rows = allocations(manifest); events = events or manifest.get("events", [])
    confirmed = [r for r in rows if r["status"] == "CONFIRMED"]
    violations: dict[str, list] = defaultdict(list)

    # I1: ningún equipo posee confirmaciones solapadas.
    opportunities: dict[tuple[str, str, str], set[str]] = defaultdict(set)
    for i, left in enumerate(confirmed):
        a = SlotRequest(**left["request"])
        opportunities[(a.equipment_id, a.starts_at, a.ends_at)].add(a.user_id)
        for right in confirmed[i + 1:]:
            b = SlotRequest(**right["request"])
            if overlaps(a, b) and a.user_id != b.user_id:
                violations["I1_NO_DOUBLE_ALLOCATION"].append([a.request_id, b.request_id])

    # I2: toda confirmación tiene exactamente un adjudicatario.
    for row in confirmed:
        if not row["request"].get("user_id"):
            violations["I2_EXACTLY_ONE_ASSIGNEE"].append(row["request"]["request_id"])

    # I3: no se adjudica equipo inactivo/en mantenimiento.
    equipment = {e["equipmentId"]: e for e in equipment_fixture.get("equipment", [])}
    for row in confirmed:
        item = equipment.get(row["request"]["equipment_id"])
        if item is None or not item.get("active") or item.get("status") == "MANTENIMIENTO":
            violations["I3_NOT_IN_MAINTENANCE"].append(row["request"]["request_id"])

    # I4: cada CANCELLED debe tener exactamente un RELEASED asociado.
    cancelled = [e for e in events if e.get("type") == "CANCELLED"]
    releases = Counter(e.get("allocation_id") for e in events if e.get("type") == "RELEASED")
    for event in cancelled:
        if releases[event.get("allocation_id")] != 1:
            violations["I4_RELEASE_EXACTLY_ONCE"].append(event.get("allocation_id"))

    # I5: ACCESS_GRANTED requiere adjudicación vigente del mismo usuario/equipo/franja.
    access = [e for e in events if e.get("type") == "ACCESS_GRANTED"]
    for event in access:
        valid = any(r["request"]["user_id"] == event.get("user_id")
                    and r["request"]["equipment_id"] == event.get("equipment_id")
                    and r["request"]["starts_at"] <= event.get("at") < r["request"]["ends_at"]
                    for r in confirmed)
        if not valid: violations["I5_VALID_ACCESS_ONLY"].append(event)

    user_counts = Counter(r["request"]["user_id"] for r in confirmed)
    equipment_counts = Counter(r["request"]["equipment_id"] for r in confirmed)
    rejected = [r for r in rows if r["status"] == "REJECTED"]
    unnecessary = [r for r in rejected if not any(
        overlaps(SlotRequest(**r["request"]), SlotRequest(**confirmed_row["request"]))
        for confirmed_row in confirmed)]
    doubles = sum(len(users) > 1 for users in opportunities.values())
    total_slots = max(1, len(opportunities))
    low, high = wilson_interval(min(doubles, total_slots), total_slots)
    invariant_status = {
        "I1_NO_DOUBLE_ALLOCATION": "FAIL" if violations["I1_NO_DOUBLE_ALLOCATION"] else "PASS",
        "I2_EXACTLY_ONE_ASSIGNEE": "FAIL" if violations["I2_EXACTLY_ONE_ASSIGNEE"] else "PASS",
        "I3_NOT_IN_MAINTENANCE": "FAIL" if violations["I3_NOT_IN_MAINTENANCE"] else "PASS",
        "I4_RELEASE_EXACTLY_ONCE": ("NOT_OBSERVED" if not cancelled else
                                     "FAIL" if violations["I4_RELEASE_EXACTLY_ONCE"] else "PASS"),
        "I5_VALID_ACCESS_ONLY": ("NOT_OBSERVED" if not access else
                                  "FAIL" if violations["I5_VALID_ACCESS_ONLY"] else "PASS"),
    }
    return {"run_id": manifest.get("run_id"), "invariants": invariant_status,
            "violations": dict(violations), "confirmed": len(confirmed), "rejected": len(rejected),
            "double_allocation_rate": doubles / total_slots,
            "double_allocation_ci95": [low, high], "unnecessary_rejections": len(unnecessary),
            "jain_users": jain(user_counts.values()) if user_counts else None,
            "jain_equipment": jain(equipment_counts.values()) if equipment_counts else None,
            "fixture_source": equipment_fixture.get("source", "unknown")}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("manifest", type=Path)
    parser.add_argument("--equipment", type=Path, default=Path("experimentos/fixtures/equipos-experimentales.json"))
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    result = evaluate(json.loads(args.manifest.read_text(encoding="utf-8")),
                      json.loads(args.equipment.read_text(encoding="utf-8")))
    rendered = json.dumps(result, indent=2)
    if args.output: args.output.write_text(rendered, encoding="utf-8")
    else: print(rendered)


if __name__ == "__main__": main()
