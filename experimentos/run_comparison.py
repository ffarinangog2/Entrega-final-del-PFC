"""Automatiza la comparación experimental entre pandas y PySpark.

El coordinador ejecuta cada tratamiento en un proceso aislado, muestrea CPU y
memoria del árbol de procesos y guarda un JSON por repetición medida. El modo
worker materializa el resultado únicamente para obtener la cantidad de filas.

Dependencia de instrumentación: ``psutil``.
"""

from __future__ import annotations

import argparse
import csv
import importlib.metadata
import json
import os
import platform
import socket
import statistics
import subprocess
import sys
import tempfile
import time
import uuid
from datetime import UTC, datetime
from pathlib import Path
from typing import Any

import psutil


PROJECT_ROOT = Path(__file__).resolve().parents[1]
SPARK_DIR = PROJECT_ROOT / "spark"
DEFAULT_CONFIG = Path(__file__).with_name("experiment-config.json")
SAMPLE_INTERVAL_SECONDS = 0.2
MIB = 1024 * 1024

COMPARISON_COLUMNS = (
    "run_id",
    "protocol_version",
    "treatment",
    "dataset_id",
    "dataset_snapshot_id",
    "iteration",
    "started_at",
    "finished_at",
    "duration_ms",
    "cpu_percent_mean",
    "cpu_percent_max",
    "memory_mib_mean",
    "memory_mib_max",
    "rows_processed",
    "throughput_rows_per_second",
    "status",
    "error",
    "host",
    "operating_system",
    "cpu_model",
    "logical_cpu_count",
    "system_memory_mib",
    "python_version",
    "pandas_version",
    "spark_version",
    "java_version",
)


def utc_now() -> str:
    """Devuelve una marca ISO-8601 normalizada en UTC."""

    return datetime.now(UTC).isoformat().replace("+00:00", "Z")


def installed_version(distribution: str) -> str | None:
    """Consulta una versión sin importar el paquete medido."""

    try:
        return importlib.metadata.version(distribution)
    except importlib.metadata.PackageNotFoundError:
        return None


def environment_metadata() -> dict[str, Any]:
    """Captura información estable del entorno para cada observación."""

    return {
        "host": socket.gethostname(),
        "operating_system": platform.platform(),
        "cpu_model": platform.processor() or "unknown",
        "logical_cpu_count": psutil.cpu_count(logical=True) or 1,
        "memory_mib": round(psutil.virtual_memory().total / MIB, 3),
        "python_version": platform.python_version(),
        "pandas_version": installed_version("pandas"),
        "spark_version": installed_version("pyspark"),
        "java_version": os.getenv("JAVA_VERSION"),
    }


def load_config(path: Path) -> dict[str, Any]:
    """Lee y valida los campos mínimos de la configuración experimental."""

    with path.open(encoding="utf-8") as config_file:
        config = json.load(config_file)

    treatment_ids = {item["id"] for item in config["treatments"]}
    expected = {"pandas-baseline", "pyspark-pipeline"}
    if treatment_ids != expected:
        raise ValueError(f"Los tratamientos deben ser exactamente: {sorted(expected)}")

    for item in config["treatments"]:
        entrypoint = PROJECT_ROOT / item["entrypoint"]
        if not entrypoint.is_file():
            raise FileNotFoundError(f"No existe el tratamiento: {entrypoint}")

    return config


def execute_pandas_worker() -> int:
    """Materializa la línea base pandas y devuelve sus filas resultantes."""

    sys.path.insert(0, str(SPARK_DIR))
    import baseline

    engine = baseline.crear_motor(baseline.PandasDbConfig.desde_entorno())
    try:
        sources = baseline.cargar_fuentes(engine)
        result = baseline.construir_pipeline(sources)
        return len(result.index)
    finally:
        engine.dispose()


def execute_spark_worker() -> int:
    """Materializa el pipeline Spark mediante count y devuelve sus filas."""

    sys.path.insert(0, str(SPARK_DIR))
    import pipeline

    spark = pipeline.crear_sesion()
    try:
        jdbc = pipeline.JdbcConfig.desde_entorno()
        sources = pipeline.cargar_fuentes(spark, jdbc)
        result = pipeline.construir_pipeline(sources)
        return result.count()
    finally:
        spark.stop()


def worker(treatment: str, result_path: Path) -> int:
    """Ejecuta un tratamiento y comunica su conteo al coordinador."""

    try:
        if treatment == "pandas-baseline":
            rows = execute_pandas_worker()
        elif treatment == "pyspark-pipeline":
            rows = execute_spark_worker()
        else:
            raise ValueError(f"Tratamiento desconocido: {treatment}")

        payload = {"status": "completed", "rows_processed": rows, "error": None}
        exit_code = 0
    except Exception as error:  # El error debe persistirse como observación.
        payload = {
            "status": "failed",
            "rows_processed": 0,
            "error": f"{type(error).__name__}: {error}",
        }
        exit_code = 1

    result_path.write_text(json.dumps(payload, ensure_ascii=False), encoding="utf-8")
    return exit_code


def process_tree(root: psutil.Process) -> list[psutil.Process]:
    """Obtiene procesos vivos del tratamiento, incluidos hijos de Spark."""

    processes = [root]
    try:
        processes.extend(root.children(recursive=True))
    except (psutil.NoSuchProcess, psutil.AccessDenied):
        pass
    return processes


def sample_resources(
    root: psutil.Process,
    known_processes: dict[int, psutil.Process],
) -> tuple[float, float]:
    """Muestrea CPU acumulada y RSS total del árbol de procesos."""

    for process in process_tree(root):
        if process.pid not in known_processes:
            known_processes[process.pid] = process
            try:
                process.cpu_percent(interval=None)
            except (psutil.NoSuchProcess, psutil.AccessDenied):
                pass

    cpu_percent = 0.0
    rss_bytes = 0
    for process in list(known_processes.values()):
        try:
            cpu_percent += process.cpu_percent(interval=None)
            rss_bytes += process.memory_info().rss
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            continue

    return cpu_percent, rss_bytes / MIB


def terminate_process_tree(process: subprocess.Popen[Any]) -> None:
    """Finaliza un tratamiento que excedió el tiempo configurado."""

    try:
        root = psutil.Process(process.pid)
        for child in root.children(recursive=True):
            child.kill()
        root.kill()
    except psutil.NoSuchProcess:
        pass
    process.wait()


def execute_treatment(
    treatment: str,
    iteration: int,
    config: dict[str, Any],
) -> dict[str, Any]:
    """Ejecuta y mide una repetición, sin persistirla todavía."""

    run_id = str(uuid.uuid4())
    started_at = utc_now()
    start = time.perf_counter()
    timeout = float(config["execution"]["timeout_seconds"])
    cpu_samples: list[float] = []
    memory_samples: list[float] = []

    with tempfile.TemporaryDirectory(prefix="scli-experiment-") as temp_dir:
        temp_path = Path(temp_dir)
        worker_result = temp_path / "worker-result.json"
        worker_log = temp_path / "worker.log"

        command = [
            sys.executable,
            str(Path(__file__).resolve()),
            "--worker",
            treatment,
            "--worker-result",
            str(worker_result),
        ]

        with worker_log.open("w+", encoding="utf-8") as log_file:
            process = subprocess.Popen(
                command,
                cwd=PROJECT_ROOT,
                stdout=log_file,
                stderr=subprocess.STDOUT,
            )
            root_process = psutil.Process(process.pid)
            known_processes: dict[int, psutil.Process] = {}
            timed_out = False

            while process.poll() is None:
                cpu, memory = sample_resources(root_process, known_processes)
                cpu_samples.append(cpu)
                memory_samples.append(memory)
                if time.perf_counter() - start >= timeout:
                    timed_out = True
                    terminate_process_tree(process)
                    break
                time.sleep(SAMPLE_INTERVAL_SECONDS)

            log_file.seek(0)
            captured_log = log_file.read().strip()

        if timed_out:
            worker_payload = {
                "status": "timeout",
                "rows_processed": 0,
                "error": f"Tiempo máximo excedido: {timeout} segundos",
            }
        elif worker_result.is_file():
            worker_payload = json.loads(worker_result.read_text(encoding="utf-8"))
        else:
            worker_payload = {
                "status": "failed",
                "rows_processed": 0,
                "error": captured_log or f"Proceso finalizado con código {process.returncode}",
            }

    duration_ms = (time.perf_counter() - start) * 1000
    rows_processed = int(worker_payload["rows_processed"])
    throughput = rows_processed / (duration_ms / 1000) if duration_ms > 0 else 0.0

    return {
        "run_id": run_id,
        "protocol_version": config["protocol_version"],
        "treatment": treatment,
        "dataset_id": config["dataset"]["id"],
        "dataset_snapshot_id": config["dataset"].get("snapshot_id"),
        "iteration": iteration,
        "started_at": started_at,
        "finished_at": utc_now(),
        "duration_ms": round(duration_ms, 3),
        "cpu_percent_mean": round(statistics.fmean(cpu_samples), 3) if cpu_samples else 0.0,
        "cpu_percent_max": round(max(cpu_samples), 3) if cpu_samples else 0.0,
        "memory_mib_mean": (
            round(statistics.fmean(memory_samples), 3) if memory_samples else 0.0
        ),
        "memory_mib_max": round(max(memory_samples), 3) if memory_samples else 0.0,
        "rows_processed": rows_processed,
        "throughput_rows_per_second": round(throughput, 3),
        "status": worker_payload["status"],
        "error": worker_payload.get("error"),
        "environment": environment_metadata(),
    }


def persist_record(record: dict[str, Any], output_directory: Path) -> Path:
    """Guarda atómicamente una observación JSON."""

    output_directory.mkdir(parents=True, exist_ok=True)
    destination = output_directory / (
        f"{record['started_at'].replace(':', '-')}_{record['treatment']}_"
        f"{record['run_id']}.json"
    )
    temporary = destination.with_suffix(".tmp")
    temporary.write_text(
        json.dumps(record, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    temporary.replace(destination)
    return destination


def flatten_record(record: dict[str, Any]) -> dict[str, Any]:
    """Aplana el entorno anidado para representar una ejecución en CSV."""

    environment = record["environment"]
    return {
        **{column: record.get(column) for column in COMPARISON_COLUMNS[:17]},
        "host": environment["host"],
        "operating_system": environment["operating_system"],
        "cpu_model": environment["cpu_model"],
        "logical_cpu_count": environment["logical_cpu_count"],
        "system_memory_mib": environment["memory_mib"],
        "python_version": environment["python_version"],
        "pandas_version": environment.get("pandas_version"),
        "spark_version": environment.get("spark_version"),
        "java_version": environment.get("java_version"),
    }


def generate_comparative_outputs(
    records: list[dict[str, Any]],
    output_directory: Path,
) -> tuple[Path, Path]:
    """Consolida el lote medido en las plantillas comparativas JSON y CSV."""

    output_directory.mkdir(parents=True, exist_ok=True)
    json_destination = output_directory / "comparacion.json"
    csv_destination = output_directory / "comparacion.csv"

    json_payload = {
        "protocol_version": records[0]["protocol_version"] if records else None,
        "generated_at": utc_now() if records else None,
        "results": records,
    }
    json_temporary = json_destination.with_suffix(".json.tmp")
    json_temporary.write_text(
        json.dumps(json_payload, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    json_temporary.replace(json_destination)

    csv_temporary = csv_destination.with_suffix(".csv.tmp")
    with csv_temporary.open("w", encoding="utf-8", newline="") as csv_file:
        writer = csv.DictWriter(csv_file, fieldnames=COMPARISON_COLUMNS)
        writer.writeheader()
        writer.writerows(flatten_record(record) for record in records)
    csv_temporary.replace(csv_destination)

    return json_destination, csv_destination


def ordered_treatments(config: dict[str, Any], iteration: int) -> list[str]:
    """Alterna el orden para reducir sesgo entre tratamientos."""

    treatments = [item["id"] for item in config["treatments"]]
    if config["execution"].get("alternate_treatment_order") and iteration % 2 == 0:
        treatments.reverse()
    return treatments


def run_comparison(config_path: Path) -> None:
    """Ejecuta calentamientos y repeticiones medidas de ambos tratamientos."""

    config = load_config(config_path)
    warmups = int(config["execution"]["warmup_iterations"])
    repetitions = int(config["execution"]["measured_iterations"])
    output_directory = PROJECT_ROOT / config["output"]["directory"]
    measured_records: list[dict[str, Any]] = []

    for warmup in range(1, warmups + 1):
        for treatment in ordered_treatments(config, warmup):
            execute_treatment(treatment, warmup, config)

    for iteration in range(1, repetitions + 1):
        for treatment in ordered_treatments(config, iteration):
            record = execute_treatment(treatment, iteration, config)
            persist_record(record, output_directory)
            measured_records.append(record)

    generate_comparative_outputs(measured_records, output_directory)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--config", type=Path, default=DEFAULT_CONFIG)
    parser.add_argument("--worker", choices=["pandas-baseline", "pyspark-pipeline"])
    parser.add_argument("--worker-result", type=Path)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if args.worker:
        if args.worker_result is None:
            raise ValueError("--worker-result es obligatorio en modo worker")
        return worker(args.worker, args.worker_result)

    run_comparison(args.config)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
