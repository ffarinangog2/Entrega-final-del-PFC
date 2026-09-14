#!/usr/bin/env python3
"""Inyector controlado para Esc-4. Nunca actúa sin confirmación explícita."""
from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import shlex
import subprocess
import time
import urllib.request


def execute_s4_failure(command: str, confirmation: str, health_url: str, timeout_seconds: float = 60) -> dict:
    if os.environ.get("EXPERIMENTAL_ARBITER_ENABLED", "false").lower() != "true":
        raise PermissionError("Modo experimental deshabilitado")
    if os.environ.get("ARBITER", "").lower() != "s4":
        raise PermissionError("La caída de clúster solo corresponde a ARBITER=s4")
    if confirmation != "CONFIRM_EXPERIMENTAL_NODE_FAILURE":
        raise PermissionError("Falta confirmación explícita")
    if any(token in command.lower() for token in ("down -v", "rm -", "delete", "wipe", "truncate")):
        raise ValueError("Comando destructivo rechazado")
    started_wall = time.time_ns(); started = time.perf_counter()
    completed = subprocess.run(shlex.split(command), check=False, capture_output=True, text=True)
    deadline = time.monotonic() + timeout_seconds
    service_recovered = False
    while time.monotonic() < deadline:
        try:
            with urllib.request.urlopen(health_url, timeout=2) as response:
                service_recovered = 200 <= response.status < 300
            if service_recovered: break
        except OSError:
            time.sleep(0.25)
    recovery_ms = (time.perf_counter() - started) * 1000
    return {"type": "RECOVERY", "strategy": "s4", "started_ns": started_wall,
            "recovery_ms": recovery_ms, "recovery_seconds": recovery_ms / 1000,
            "service_recovered": service_recovered,
            "exit_code": completed.returncode,
            "stdout": completed.stdout[-1000:], "stderr": completed.stderr[-1000:]}


def main():
    parser = argparse.ArgumentParser(description="Ejecutar solo en la VM experimental al 50 % de Esc-4")
    parser.add_argument("--strategy", required=True, choices=("s3", "s4"))
    parser.add_argument("--s4-command", help="Comando reversible preparado por el operador de la VM")
    parser.add_argument("--health-url", help="Health interno que confirma la vuelta al servicio")
    parser.add_argument("--confirm", default="")
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.strategy == "s3":
        result = {"status": "DELEGATED",
                  "detail": "S3 se inyecta mediante el endpoint interno del BullyCluster del backend"}
    elif not args.s4_command or not args.health_url:
        result = {"status": "NOT_EXECUTED", "detail": "Faltan comando reversible o health URL de la VM"}
    else:
        result = execute_s4_failure(args.s4_command, args.confirm, args.health_url)
    args.output.write_text(json.dumps(result, indent=2), encoding="utf-8")


if __name__ == "__main__": main()
