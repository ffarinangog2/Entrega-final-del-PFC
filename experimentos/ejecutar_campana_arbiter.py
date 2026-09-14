#!/usr/bin/env python3
"""Orquesta la campaña oficial ARBITER en una VM autorizada.

No adjudica: todas las decisiones S0-S4 se envían a reservas-solicitudes-service.
La configuración runtime se restaura mediante ``finally`` incluso ante abortos.
"""
from __future__ import annotations

import argparse
from collections import Counter
import csv
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import urllib.request

from generar_evidencia_arbiter import sha256sums
from arbiter_core import a12, descriptive, mann_whitney
from generador_rafagas import run_burst
from oraculo_reservas import evaluate
from planificar_arbiter import build_plan


EXPECTED = {"esc1": 10, "esc2": 50, "esc3": 50, "esc4": 20}
FINAL_STATES = {"COMPLETED", "FAILED", "INVALID"}


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat()


def validate_plan(plan: list[dict]) -> None:
    counts = Counter(row["scenario"] for row in plan)
    if len(plan) != 130 or dict(counts) != EXPECTED:
        raise RuntimeError(f"Matriz inválida: total={len(plan)}, escenarios={dict(counts)}")
    if sum(bool(row["analysis"]) for row in plan) != 104:
        raise RuntimeError("La matriz no contiene exactamente 104 muestras analíticas")


def extract_internal_api_key(effective_environment: list[str]) -> str:
    """Extrae el valor ya interpretado por Compose, nunca el texto dotenv crudo."""
    prefix = "INTERNAL_API_KEY="
    matches = [entry[len(prefix):] for entry in effective_environment if entry.startswith(prefix)]
    if len(matches) != 1 or not matches[0]:
        raise RuntimeError("El contenedor de Reservas no expone una INTERNAL_API_KEY efectiva única")
    return matches[0]


def write_runtime_mode(path: Path, original: str, enabled: bool, strategy: str) -> None:
    values = {"EXPERIMENTAL_ARBITER_ENABLED": "true" if enabled else "false", "ARBITER": strategy}
    lines = original.splitlines()
    for key, value in values.items():
        pattern = re.compile(rf"^{re.escape(key)}=")
        positions = [i for i, line in enumerate(lines) if pattern.match(line)]
        if positions:
            lines[positions[0]] = f"{key}={value}"
            for i in reversed(positions[1:]):
                del lines[i]
        else:
            lines.append(f"{key}={value}")
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


class Campaign:
    def __init__(self, args, command_runner=subprocess.run, sleeper=time.sleep):
        self.args = args
        self.run_command = command_runner
        self.sleep = sleeper
        self.root = args.output_dir.resolve()
        self.raw = self.root / "raw"
        self.oracle = self.root / "analysis" / "oracle"
        self.logs = self.root / "logs"
        self.checkpoint_path = self.root / "manifest" / "checkpoint.json"
        self.fixture = json.loads(args.equipment_fixture.read_text(encoding="utf-8"))
        self.original_env = args.runtime_env.read_text(encoding="utf-8")
        self.original_process_env = os.environ.copy()
        self.current_mode = None
        for directory in (self.raw, self.oracle, self.logs, self.checkpoint_path.parent,
                          self.root / "summary", self.root / "analysis"):
            directory.mkdir(parents=True, exist_ok=True)
        self.internal_api_key = self.effective_internal_api_key()

    def command(self, *parts: str, capture=False, env=None) -> subprocess.CompletedProcess:
        result = self.run_command(list(parts), cwd=self.args.repo, check=False,
                                  capture_output=capture, text=True,
                                  env=self.original_process_env.copy() if env is None else env)
        if result.returncode:
            detail = (result.stderr or result.stdout or "")[-2000:] if capture else ""
            raise RuntimeError(f"Comando falló ({result.returncode}): {' '.join(parts)} {detail}")
        return result

    def effective_internal_api_key(self) -> str:
        service_id = self.command("docker", "compose", "-p", self.args.compose_project,
                                  "-f", str(self.args.compose_file), "ps", "-q",
                                  "reservas-solicitudes-service", capture=True).stdout.strip()
        if not service_id:
            raise RuntimeError("No se encontró el contenedor de Reservas")
        effective = json.loads(self.command("docker", "inspect", "--format",
                               "{{json .Config.Env}}", service_id, capture=True).stdout)
        return extract_internal_api_key(effective)

    def compose_environment(self, enabled: bool, strategy: str) -> dict[str, str]:
        environment = self.original_process_env.copy()
        environment["EXPERIMENTAL_ARBITER_ENABLED"] = "true" if enabled else "false"
        environment["ARBITER"] = strategy
        return environment

    def set_mode(self, enabled: bool, strategy: str = "") -> None:
        target = (enabled, strategy)
        if target == self.current_mode:
            return
        if enabled and strategy not in {"s0", "s1", "s2", "s3", "s4"}:
            raise ValueError("Estrategia experimental inválida")
        write_runtime_mode(self.args.runtime_env, self.original_env, enabled, strategy)
        environment = self.compose_environment(enabled, strategy)
        self.command("docker", "compose", "-p", self.args.compose_project, "-f",
                     str(self.args.compose_file), "up", "-d", "--no-deps", "--force-recreate",
                     "reservas-solicitudes-service", env=environment)
        self.wait_health(self.runtime_url(self.args.reservas_health_url))
        self.current_mode = target

    def runtime_url(self, value: str) -> str:
        if "{reservas_ip}" not in value:
            return value
        service_id = self.command("docker", "compose", "-p", self.args.compose_project,
                                  "-f", str(self.args.compose_file), "ps", "-q",
                                  "reservas-solicitudes-service", capture=True).stdout.strip()
        if not service_id:
            raise RuntimeError("No se encontró el contenedor de Reservas")
        networks = json.loads(self.command("docker", "inspect", "--format",
                              "{{json .NetworkSettings.Networks}}", service_id,
                              capture=True).stdout)
        addresses = [network.get("IPAddress") for network in networks.values() if network.get("IPAddress")]
        if not addresses:
            raise RuntimeError("El contenedor de Reservas no tiene IP interna")
        return value.replace("{reservas_ip}", addresses[0])

    def wait_health(self, url: str, timeout=240) -> None:
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            try:
                with urllib.request.urlopen(url, timeout=3) as response:
                    if response.status == 200:
                        return
            except OSError:
                pass
            self.sleep(2)
        raise RuntimeError(f"Servicio no quedó healthy: {url}")

    def checkpoint(self) -> dict:
        if self.args.resume and self.checkpoint_path.exists():
            return json.loads(self.checkpoint_path.read_text(encoding="utf-8"))
        plan = build_plan(self.args.seed)
        validate_plan(plan)
        rows = []
        for index, row in enumerate(plan, 1):
            rows.append({"id": index, **row, "state": "PLANNED",
                         "excluded_from_analysis": not row["analysis"]})
        value = {"sha": self.args.sha, "created_at": utc_now(), "runs": rows}
        self.save_checkpoint(value)
        return value

    def save_checkpoint(self, value: dict) -> None:
        temporary = self.checkpoint_path.with_suffix(".tmp")
        temporary.write_text(json.dumps(value, indent=2), encoding="utf-8")
        temporary.replace(self.checkpoint_path)

    def burst(self, strategy: str, scenario: str, rep: int, seed: int,
              output: Path, agents=None, halfway=None) -> dict:
        environment = self.compose_environment(True, strategy)
        return run_burst(strategy, scenario, rep, seed, self.args.equipment_id,
                         self.args.laboratory_id, self.args.starts_at, self.args.ends_at,
                         output, self.runtime_url(self.args.endpoint),
                         self.internal_api_key, on_halfway=halfway,
                         agents_override=agents, environment=environment)

    def evaluate_manifest(self, manifest: dict) -> dict:
        result = evaluate(manifest, self.fixture)
        output = self.oracle / f"{manifest['run_id']}.json"
        output.write_text(json.dumps(result, indent=2), encoding="utf-8")
        return result

    def smoke(self) -> None:
        smoke_dir = self.root / "smoke"
        for strategy in ("s0", "s1", "s2", "s3", "s4"):
            self.set_mode(True, strategy)
            manifest = self.burst(strategy, "esc2", 1, self.args.seed, smoke_dir, agents=3)
            if any(r.get("allocation", {}).get("strategy") != strategy
                   for r in manifest["records"] if r.get("type") == "REQUEST"):
                raise RuntimeError(f"Smoke seleccionó Strategy incorrecta: {strategy}")
            if not any(r.get("allocation", {}).get("status") == "CONFIRMED"
                       for r in manifest["records"] if r.get("type") == "REQUEST"):
                raise RuntimeError(f"Smoke sin adjudicación confirmada: {strategy}")
            self.evaluate_manifest(manifest)

    def negative_control(self) -> None:
        self.set_mode(True, "s0")
        manifest = self.burst("s0", "esc3", 0, self.args.seed,
                              self.root / "precheck-negative-control")
        result = self.evaluate_manifest(manifest)
        if result["double_allocation_rate"] <= 0:
            raise RuntimeError("S0/Esc-3 no exhibió doble adjudicación; campaña abortada")

    def nominal(self, row: dict) -> dict:
        self.set_mode(False, "")
        run_id = f"esc1-production-r{row['repetition']:02d}"
        destination = self.raw / run_id
        destination.mkdir(parents=True, exist_ok=True)
        prefix = destination / "locust"
        started = utc_now()
        result = self.run_command([
            self.args.locust_python, "-m", "locust", "-f", str(self.args.locustfile), "--headless",
            "--host", self.args.gateway_url, "-u", "50", "-r", "10",
            "--run-time", self.args.nominal_duration, "--csv", str(prefix),
            "--html", str(destination / "report.html")], cwd=self.args.repo,
            check=False, capture_output=True, text=True)
        (destination / "locust.log").write_text((result.stdout or "") + (result.stderr or ""),
                                                 encoding="utf-8")
        manifest = {"run_id": run_id, "strategy": "production", "scenario": "esc1",
                    "repetition": row["repetition"], "seed": row["seed"], "sha": self.args.sha,
                    "started_at": started, "finished_at": utc_now(), "users": 50,
                    "duration": self.args.nominal_duration, "exit_code": result.returncode,
                    "excluded_from_analysis": row["excluded_from_analysis"], "records": []}
        (self.raw / f"{run_id}.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")
        if result.returncode:
            raise RuntimeError(f"Locust terminó con código {result.returncode}")
        return manifest

    def s4_halfway(self):
        command = self.args.s4_failure_command.strip().split()
        if len(command) != 3 or command[:2] != ["docker", "restart"] \
                or not re.fullmatch(r"reservas-db-node-[123]", command[2]):
            raise RuntimeError("Esc-4/S4 falla cerrada: solo se admite docker restart reservas-db-node-[123]")
        started = time.perf_counter()
        result = self.run_command(command, cwd=self.args.repo, check=False,
                                  capture_output=True, text=True)
        if result.returncode:
            raise RuntimeError("No se pudo reiniciar reversiblemente un nodo Cockroach")
        self.wait_health(self.runtime_url(self.args.reservas_health_url))
        recovery = (time.perf_counter() - started) * 1000
        return {"type": "RECOVERY", "strategy": "s4", "recovery_ms": recovery,
                "recovery_seconds": recovery / 1000, "timestamp": utc_now(),
                "service_recovered": True}

    def full(self) -> dict:
        checkpoint = self.checkpoint()
        for row in checkpoint["runs"]:
            if row["state"] == "COMPLETED" or (self.args.resume and row["state"] in {"INVALID"}):
                continue
            row.update(state="STARTED", started_at=utc_now(), error=None)
            self.save_checkpoint(checkpoint)
            try:
                if row["scenario"] == "esc1":
                    manifest = self.nominal(row)
                    row["oracle_status"] = "N/A"
                else:
                    strategy = row["strategy"]
                    self.set_mode(True, strategy)
                    halfway = self.s4_halfway if row["scenario"] == "esc4" and strategy == "s4" else None
                    manifest = self.burst(strategy, row["scenario"], row["repetition"],
                                          row["seed"], self.raw, halfway=halfway)
                    oracle = self.evaluate_manifest(manifest)
                    row["oracle_status"] = oracle["invariants"]
                row.update(state="COMPLETED", run_id=manifest["run_id"], finished_at=utc_now())
            except Exception as exc:  # conserva fallo real; nunca lo convierte en cero
                row.update(state="FAILED", error=f"{type(exc).__name__}: {exc}", finished_at=utc_now())
            self.save_checkpoint(checkpoint)
        return checkpoint

    def validate_completion(self, checkpoint: dict) -> None:
        completed = [r for r in checkpoint["runs"] if r["state"] == "COMPLETED"]
        if len(completed) != 130:
            raise RuntimeError(f"Campaña incompleta: {len(completed)}/130 COMPLETED")
        counts = Counter(r["scenario"] for r in completed)
        if dict(counts) != EXPECTED:
            raise RuntimeError(f"Conteos finales inválidos: {dict(counts)}")
        if sum(not r["excluded_from_analysis"] for r in completed) != 104:
            raise RuntimeError("Conteo analítico final distinto de 104")

    def finalize(self) -> None:
        summaries = []
        for path in sorted(self.raw.glob("*.json")):
            manifest = json.loads(path.read_text(encoding="utf-8"))
            requests = [record for record in manifest.get("records", [])
                        if record.get("type") == "REQUEST"]
            oracle_path = self.oracle / path.name
            oracle = (json.loads(oracle_path.read_text(encoding="utf-8"))
                      if oracle_path.exists() else {})
            latencies = [record["latency_ms"] for record in requests
                         if isinstance(record.get("latency_ms"), (int, float))]
            recoveries = [record.get("recovery_ms") for record in manifest.get("records", [])
                          if record.get("type") == "RECOVERY" and record.get("recovery_ms") is not None]
            summaries.append({
                "run_id": manifest["run_id"], "scenario": manifest["scenario"],
                "strategy": manifest["strategy"], "repetition": manifest["repetition"],
                "excluded_from_analysis": manifest.get("excluded_from_analysis", False),
                "valid": True, "requests": len(requests),
                "latency_ms": (sum(latencies) / len(latencies)) if latencies else None,
                "double_allocation_rate": oracle.get("double_allocation_rate"),
                "unnecessary_rejections": oracle.get("unnecessary_rejections"),
                "jain_users": oracle.get("jain_users"), "jain_equipment": oracle.get("jain_equipment"),
                "recovery_ms": recoveries[0] if recoveries else None,
                "invariants": oracle.get("invariants", "N/A")})
        summary_dir = self.root / "summary"
        (summary_dir / "runs.json").write_text(json.dumps(summaries, indent=2), encoding="utf-8")
        if summaries:
            with (summary_dir / "runs.csv").open("w", newline="", encoding="utf-8") as output:
                writer = csv.DictWriter(output, fieldnames=[key for key in summaries[0] if key != "invariants"])
                writer.writeheader()
                writer.writerows({key: value for key, value in row.items() if key != "invariants"}
                                 for row in summaries)
        comparisons = []
        analytical = [row for row in summaries if not row["excluded_from_analysis"] and row["valid"]]
        for scenario in ("esc2", "esc3"):
            for strategy in ("s1", "s2", "s3", "s4"):
                for metric in ("double_allocation_rate", "latency_ms", "unnecessary_rejections"):
                    left = [float(row[metric]) for row in analytical
                            if row["scenario"] == scenario and row["strategy"] == "s0"
                            and row[metric] is not None]
                    right = [float(row[metric]) for row in analytical
                             if row["scenario"] == scenario and row["strategy"] == strategy
                             and row[metric] is not None]
                    if left and right:
                        u, p = mann_whitney(left, right)
                        comparisons.append({"scenario": scenario, "metric": metric,
                                            "group_a": "s0", "group_b": strategy,
                                            "group_a_summary": descriptive(left),
                                            "group_b_summary": descriptive(right),
                                            "mann_whitney_u": u, "p_value": p,
                                            "a12_a_greater_than_b": a12(left, right)})
        (self.root / "analysis" / "comparisons.json").write_text(
            json.dumps(comparisons, indent=2), encoding="utf-8")
        sha256sums(self.root, self.root / "SHA256SUMS")

    def restore(self) -> None:
        self.args.runtime_env.write_text(self.original_env, encoding="utf-8")
        self.current_mode = None
        environment = self.compose_environment(False, "")
        self.command("docker", "compose", "-p", self.args.compose_project, "-f",
                     str(self.args.compose_file), "up", "-d", "--no-deps", "--force-recreate",
                     "reservas-solicitudes-service", env=environment)
        self.wait_health(self.runtime_url(self.args.reservas_health_url))


def parser() -> argparse.ArgumentParser:
    value = argparse.ArgumentParser()
    mode = value.add_mutually_exclusive_group(required=True)
    mode.add_argument("--smoke", action="store_true")
    mode.add_argument("--precheck-negative-control", action="store_true")
    mode.add_argument("--full", action="store_true")
    value.add_argument("--resume", action="store_true")
    value.add_argument("--output-dir", type=Path, required=True)
    value.add_argument("--repo", type=Path, default=Path.cwd())
    value.add_argument("--runtime-env", type=Path, default=Path(".env"))
    value.add_argument("--compose-file", type=Path, default=Path("docker-compose.prod.yml"))
    value.add_argument("--compose-project", default="scli-prod")
    value.add_argument("--endpoint", required=True)
    value.add_argument("--reservas-health-url", required=True)
    value.add_argument("--gateway-url", default="http://127.0.0.1:8080")
    value.add_argument("--locust-python", default=sys.executable)
    value.add_argument("--locustfile", type=Path, default=Path("tests/load/locustfile.py"))
    value.add_argument("--nominal-duration", default="5m")
    value.add_argument("--equipment-fixture", type=Path,
                       default=Path("experimentos/fixtures/equipos-experimentales.json"))
    value.add_argument("--equipment-id", required=True)
    value.add_argument("--laboratory-id", required=True)
    value.add_argument("--starts-at", required=True)
    value.add_argument("--ends-at", required=True)
    value.add_argument("--s4-failure-command", default="")
    value.add_argument("--seed", type=int, default=20260904)
    value.add_argument("--sha", required=True)
    return value


def execute_campaign(campaign: Campaign, args) -> None:
    """Ejecuta un modo y garantiza restauración aun cuando el modo aborte."""
    try:
        if args.smoke:
            campaign.smoke()
        elif args.precheck_negative_control:
            campaign.negative_control()
        else:
            checkpoint = campaign.full()
            campaign.validate_completion(checkpoint)
        campaign.finalize()
    finally:
        campaign.restore()


def main() -> None:
    args = parser().parse_args()
    args.repo = args.repo.resolve()
    args.runtime_env = (args.repo / args.runtime_env).resolve() if not args.runtime_env.is_absolute() else args.runtime_env
    args.compose_file = (args.repo / args.compose_file).resolve() if not args.compose_file.is_absolute() else args.compose_file
    args.locustfile = (args.repo / args.locustfile).resolve() if not args.locustfile.is_absolute() else args.locustfile
    args.equipment_fixture = ((args.repo / args.equipment_fixture).resolve()
                              if not args.equipment_fixture.is_absolute() else args.equipment_fixture)
    campaign = Campaign(args)
    validate_plan(build_plan(args.seed))
    execute_campaign(campaign, args)


if __name__ == "__main__":
    main()
