#!/usr/bin/env python3
"""Generador de ráfagas ARBITER. No ejecuta cargas al importarse."""
from __future__ import annotations

import argparse
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import random
import subprocess
import threading
import time
import urllib.error
import urllib.request
import uuid


SCENARIOS = {"esc2": 50, "esc3": 200, "esc4": 200}


def git_sha() -> str:
    return subprocess.run(["git", "rev-parse", "HEAD"], check=True, capture_output=True,
                          text=True).stdout.strip()


def run_burst(strategy_name: str, scenario: str, repetition: int, seed: int,
              equipment_id: str, laboratory_id: str, starts_at: str, ends_at: str,
              output: Path, endpoint: str | None = None, internal_api_key: str | None = None,
              on_halfway=None, http_post=None, agents_override: int | None = None,
              environment: dict[str, str] | None = None) -> dict:
    environment = os.environ if environment is None else environment
    if environment.get("EXPERIMENTAL_ARBITER_ENABLED", "false").lower() != "true":
        raise RuntimeError("EXPERIMENTAL_ARBITER_ENABLED=true es obligatorio")
    if environment.get("ARBITER", "").lower() != strategy_name:
        raise ValueError("ARBITER no coincide con --strategy")
    if scenario not in SCENARIOS:
        raise ValueError("El generador solo admite esc2, esc3 y esc4")
    if scenario == "esc4" and strategy_name not in {"s3", "s4"}:
        raise ValueError("Esc-4 solo admite s3 o s4")
    agents = agents_override or SCENARIOS[scenario]
    if agents < 1 or agents > SCENARIOS[scenario]:
        raise ValueError("agents_override debe estar entre 1 y el tamaño oficial del escenario")
    endpoint = endpoint or environment.get("RESERVAS_EXPERIMENTAL_URL")
    internal_api_key = internal_api_key or environment.get("INTERNAL_API_KEY")
    if not endpoint or not internal_api_key:
        raise RuntimeError("RESERVAS_EXPERIMENTAL_URL e INTERNAL_API_KEY son obligatorios")
    run_id = f"{scenario}-{strategy_name}-r{repetition:02d}-{uuid.uuid4().hex[:8]}"
    random.seed(seed)
    post = http_post or _post_json
    barrier = threading.Barrier(agents + 1)
    started = 0
    started_lock = threading.Lock()
    failure_fired = threading.Event()
    records: list[dict] = []

    def worker(index: int):
        nonlocal started
        payload = {"runId": run_id, "requestId": f"req-{index:04d}",
                   "equipmentId": equipment_id, "laboratorioId": laboratory_id,
                   "agenteId": str(uuid.uuid5(uuid.NAMESPACE_URL, f"{run_id}:agent-{index:04d}")),
                   "inicio": starts_at, "fin": ends_at, "equipmentStatus": "OPERATIVO",
                   "equipmentActive": True, "equipmentSource": "fixture"}
        ready = time.time_ns()
        barrier.wait()
        sent = time.time_ns()
        with started_lock:
            started += 1
            if scenario == "esc4" and started >= agents // 2 and not failure_fired.is_set():
                failure_fired.set()
                if strategy_name == "s3":
                    recovery = post(endpoint.rsplit("/", 1)[0] + "/lider/fallo", {}, internal_api_key)
                    recovery.update({"type": "RECOVERY", "strategy": "s3",
                                     "failure_at_request": started,
                                     "timestamp": datetime.now(timezone.utc).isoformat()})
                    records.append(recovery)
                elif on_halfway:
                    records.append(on_halfway())
        result = post(endpoint, payload, internal_api_key)
        received = time.time_ns()
        allocation = {"request": {"run_id": run_id, "request_id": payload["requestId"],
                                   "equipment_id": equipment_id, "laboratory_id": laboratory_id,
                                   "user_id": payload["agenteId"], "starts_at": starts_at,
                                   "ends_at": ends_at},
                      "status": result.get("estado", "HTTP_ERROR"),
                      "version": result.get("version", 0), "strategy": strategy_name,
                      "decided_at": result.get("decididoEn", datetime.now(timezone.utc).isoformat()),
                      "reason": result.get("motivo"), "node_id": result.get("nodeId"),
                      "leader_id": result.get("leaderId"), "lamport": result.get("lamport"),
                      "backend_response": result}
        records.append({"type": "REQUEST", "ready_ns": ready, "sent_ns": sent,
                        "received_ns": received, "latency_ms": (received - sent) / 1_000_000,
                        "allocation": allocation})

    started_at = datetime.now(timezone.utc).isoformat()
    with ThreadPoolExecutor(max_workers=agents) as executor:
        futures = [executor.submit(worker, i) for i in range(agents)]
        barrier.wait()  # libera toda la ráfaga desde un único punto
        for future in futures: future.result()

    manifest = {"run_id": run_id, "strategy": strategy_name, "scenario": scenario,
                "repetition": repetition, "seed": seed, "sha": git_sha(), "agents": agents,
                "equipment_id": equipment_id, "laboratory_id": laboratory_id,
                "starts_at": starts_at, "ends_at": ends_at,
                "started_source": "synchronized_barrier", "decision_source": "reservas-solicitudes-service",
                "endpoint": endpoint, "records": records,
                "started_at": started_at, "finished_at": datetime.now(timezone.utc).isoformat(),
                "excluded_from_analysis": repetition in {1, 10}}
    output.mkdir(parents=True, exist_ok=True)
    (output / f"{run_id}.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")
    return manifest


def _post_json(url: str, payload: dict, internal_api_key: str) -> dict:
    body = json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(url, data=body, method="POST", headers={
        "Content-Type": "application/json", "X-Internal-Api-Key": internal_api_key})
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        return {"estado": "HTTP_ERROR", "httpStatus": error.code, "detalle": detail}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--strategy", required=True, choices=sorted({"s0", "s1", "s2", "s3", "s4"}))
    parser.add_argument("--scenario", required=True, choices=sorted(SCENARIOS))
    parser.add_argument("--rep", required=True, type=int, choices=range(1, 11))
    parser.add_argument("--seed", type=int, default=20260904)
    parser.add_argument("--equipment-id", required=True)
    parser.add_argument("--laboratory-id", required=True)
    parser.add_argument("--starts-at", required=True)
    parser.add_argument("--ends-at", required=True)
    parser.add_argument("--output", type=Path, default=Path("experimentos/resultados/arbiter/raw"))
    parser.add_argument("--endpoint", default=os.getenv("RESERVAS_EXPERIMENTAL_URL"))
    parser.add_argument("--internal-api-key", default=os.getenv("INTERNAL_API_KEY"))
    parser.add_argument("--s4-failure-command")
    parser.add_argument("--s4-health-url")
    parser.add_argument("--confirm-s4-failure", default="")
    args = parser.parse_args()
    halfway = None
    if args.strategy == "s4" and args.scenario == "esc4":
        if not args.s4_failure_command or not args.s4_health_url:
            raise ValueError("Esc-4/S4 requiere comando reversible y health URL")
        from caida_coordinador import execute_s4_failure
        halfway = lambda: execute_s4_failure(args.s4_failure_command, args.confirm_s4_failure,
                                             args.s4_health_url)
    run_burst(args.strategy, args.scenario, args.rep, args.seed, args.equipment_id,
              args.laboratory_id, args.starts_at, args.ends_at, args.output,
              args.endpoint, args.internal_api_key, on_halfway=halfway)


if __name__ == "__main__":
    main()
