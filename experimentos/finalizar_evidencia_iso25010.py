"""Finaliza y verifica evidencia de una repetición ISO 25010 correctiva."""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import math
import re
import sys
from pathlib import Path

from experimentos.registrar_iso25010 import (
    BUSINESS_REQUESTS,
    CORRECTIVE_EVIDENCE,
    CORRECTIVE_RELIABILITY,
    business_event_metrics,
    attempt_directory_name,
    request_population,
    verify_sha256_manifest,
)


SECRET_PATTERNS = (
    re.compile(r"(?i)authorization\s*[:=]\s*bearer\s+[A-Za-z0-9._~-]{20,}"),
    re.compile(r'(?i)"(?:accessToken|refreshToken)"\s*:\s*"[^"\r\n]{20,}"'),
    re.compile(r"(?i)(?:JWT_SECRET|LOCUST_PASSWORD)\s*=\s*\S+"),
)
BUSINESS_URIS = {name.removeprefix("GET ") for _, name in BUSINESS_REQUESTS}
RESERVAS_JOB = "reservas-solicitudes-service"
PROMETHEUS_STEP_SECONDS = 15.0
# Prometheus representa sus timestamps internamente con resolución de milisegundos.
# Esta cota sólo se aplica a sus muestras, nunca a los eventos Locust.
PROMETHEUS_TIME_RESOLUTION_SECONDS = 0.001
EMPTY_ALLOWED_EVIDENCE = {"gateway-service.log", "reservas-service.log"}


def _write_metadata(path: Path, metadata: dict[str, object]) -> None:
    path.write_text(
        json.dumps(metadata, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
    )


def _secret_scan(evidence_dir: Path) -> bool:
    for path in evidence_dir.rglob("*"):
        if not path.is_file() or path.name == "SHA256SUMS":
            continue
        try:
            content = path.read_text(encoding="utf-8-sig")
        except (UnicodeDecodeError, OSError):
            continue
        if any(pattern.search(content) for pattern in SECRET_PATTERNS):
            return False
    return True


def _create_manifest(evidence_dir: Path) -> int:
    manifest = evidence_dir / "SHA256SUMS"
    files = sorted(
        (
            path
            for path in evidence_dir.rglob("*")
            if path.is_file() and path.name != "SHA256SUMS"
        ),
        key=lambda path: path.relative_to(evidence_dir).as_posix(),
    )
    lines = [
        f"{hashlib.sha256(path.read_bytes()).hexdigest()}  "
        f"{path.relative_to(evidence_dir).as_posix()}"
        for path in files
    ]
    manifest.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return len(files)


def _read_boundaries(evidence_dir: Path) -> dict[str, float]:
    raw = json.loads(
        (evidence_dir / "phase-boundaries.json").read_text(encoding="utf-8-sig")
    )
    boundaries = {
        key: float(raw[key]) for key in ("start_epoch", "split_epoch", "finish_epoch")
    }
    if not all(math.isfinite(value) for value in boundaries.values()):
        raise ValueError("Los límites temporales no son finitos")
    if boundaries["split_epoch"] != boundaries["start_epoch"] + 900:
        raise ValueError("split_epoch no equivale exactamente a start_epoch + 900 s")
    if boundaries["finish_epoch"] <= boundaries["split_epoch"]:
        raise ValueError("La repetición no contiene una fase t >= 900 s")
    return boundaries


def validate_prometheus_arrival(
    path: Path,
    *,
    start_epoch: float,
    split_epoch: float,
    finish_epoch: float,
) -> dict[str, object]:
    """Valida una matriz Prometheus que prueba GET en Reservas tras t=900."""
    try:
        payload = json.loads(path.read_text(encoding="utf-8-sig"))
    except (OSError, json.JSONDecodeError) as error:
        raise ValueError("Respuesta Prometheus de Reservas ausente o corrupta") from error
    if not isinstance(payload, dict) or payload.get("status") != "success":
        raise ValueError("Prometheus no respondió status=success para Reservas")
    data = payload.get("data")
    if not isinstance(data, dict) or data.get("resultType") != "matrix":
        raise ValueError("Prometheus no devolvió una matriz temporal de Reservas")
    results = data.get("result")
    if not isinstance(results, list) or not results:
        raise ValueError("Prometheus no devolvió series de Reservas")

    valid_series = 0
    total_activity = 0.0
    post_900_activity = 0.0
    covers_exact_interval = False
    covers_split = False
    for series in results:
        if not isinstance(series, dict):
            continue
        labels = series.get("metric")
        values = series.get("values")
        if not isinstance(labels, dict) or not isinstance(values, list):
            continue
        method = labels.get("method")
        uri = labels.get("uri")
        status = str(labels.get("status", ""))
        if (
            labels.get("job") != RESERVAS_JOB
            or method != "GET"
            or uri not in BUSINESS_URIS
            or not re.fullmatch(r"[1-5]\d\d", status)
        ):
            continue

        points: list[tuple[float, float]] = []
        for value in values:
            if not isinstance(value, list) or len(value) != 2:
                raise ValueError("Serie Prometheus de Reservas mal formada")
            try:
                timestamp = float(value[0])
                counter = float(value[1])
            except (TypeError, ValueError) as error:
                raise ValueError("Muestra Prometheus de Reservas inválida") from error
            if not math.isfinite(timestamp) or not math.isfinite(counter) or counter < 0:
                raise ValueError("Muestra Prometheus de Reservas inválida")
            if (
                timestamp < start_epoch - PROMETHEUS_TIME_RESOLUTION_SECONDS
                or timestamp > finish_epoch + PROMETHEUS_TIME_RESOLUTION_SECONDS
            ):
                raise ValueError("Prometheus contiene muestras fuera de la repetición")
            points.append((timestamp, counter))
        points.sort()
        if len(points) < 2:
            continue

        valid_series += 1
        if (
            points[0][0] <= start_epoch + PROMETHEUS_STEP_SECONDS
            and points[-1][0] >= finish_epoch - PROMETHEUS_STEP_SECONDS
        ):
            covers_exact_interval = True
        if any(
            abs(timestamp - split_epoch) <= PROMETHEUS_TIME_RESOLUTION_SECONDS
            for timestamp, _ in points
        ):
            covers_split = True
        previous_timestamp, previous_counter = points[0]
        for timestamp, counter in points[1:]:
            delta = counter - previous_counter
            if delta > 0:
                total_activity += delta
                if (
                    timestamp > split_epoch
                    and previous_timestamp
                    >= split_epoch - PROMETHEUS_TIME_RESOLUTION_SECONDS
                ):
                    post_900_activity += delta
            previous_timestamp, previous_counter = timestamp, counter

    if valid_series == 0:
        raise ValueError("Prometheus no contiene labels reconocidos de GET de Reservas")
    if not covers_exact_interval:
        raise ValueError("Prometheus no cubre el intervalo exacto de la repetición")
    if not covers_split:
        raise ValueError("Prometheus no contiene la muestra de frontera t=900 s")
    if total_activity <= 0:
        raise ValueError("Prometheus no demuestra actividad GET de Reservas")
    if post_900_activity <= 0:
        raise ValueError("Prometheus no demuestra GET en Reservas después de t=900 s")
    return {
        "valid_series": valid_series,
        "total_get_activity": total_activity,
        "post_900_get_activity": post_900_activity,
        "interval_start_epoch": start_epoch,
        "interval_split_epoch": split_epoch,
        "interval_finish_epoch": finish_epoch,
        "interval_covered": covers_exact_interval,
        "arrival_after_900": True,
    }


def _write_phase_summary(evidence_dir: Path) -> dict[str, object]:
    boundaries = _read_boundaries(evidence_dir)
    start = boundaries["start_epoch"]
    split = boundaries["split_epoch"]
    finish = boundaries["finish_epoch"]
    with (evidence_dir / "locust_requests.csv").open(
        encoding="utf-8-sig", newline=""
    ) as stream:
        events = list(csv.DictReader(stream))

    parsed_events: list[tuple[float, dict[str, str]]] = []
    for event in events:
        try:
            timestamp = float(event["timestamp_epoch"])
        except (KeyError, TypeError, ValueError) as error:
            raise ValueError("Evento Locust con timestamp inválido") from error
        if timestamp < start or timestamp > finish:
            raise ValueError("Evento Locust fuera del intervalo experimental")
        parsed_events.append((timestamp, event))

    summary: dict[str, object] = {
        "boundaries": {
            "start_epoch": start,
            "split_epoch": split,
            "finish_epoch": finish,
            "rules": {
                "t_lt_900": "start <= timestamp < start + 900",
                "t_gte_900": "start + 900 <= timestamp <= finish",
            },
        }
    }
    for label, lower, upper in (
        ("t_lt_900", start, split),
        ("t_gte_900", split, finish),
    ):
        selected = [
            event
            for timestamp, event in parsed_events
            if lower <= timestamp
            and (timestamp < upper if label == "t_lt_900" else timestamp <= upper)
        ]
        business = [
            event
            for event in selected
            if (event.get("request_type"), event.get("name")) in BUSINESS_REQUESTS
        ]
        duration = upper - lower
        summary[label] = {
            "get_total": len(business),
            "get_success": sum(event.get("outcome") == "success" for event in business),
            "http_401": sum(event.get("status_code") == "401" for event in business),
            "http_5xx": sum(
                event.get("status_code", "").isdigit()
                and 500 <= int(event["status_code"]) <= 599
                for event in business
            ),
            "requests_per_second": len(business) / duration,
            "refresh": sum(
                event.get("request_type") == "POST"
                and event.get("name") == "POST /api/v1/auth/refresh"
                for event in selected
            ),
        }
    if summary["t_gte_900"]["get_total"] <= 0:  # type: ignore[index]
        raise ValueError("No hay GET de negocio Locust después de t=900 s")

    prometheus = validate_prometheus_arrival(
        evidence_dir / "prometheus-reservas-status-by-uri-result.json",
        start_epoch=start,
        split_epoch=split,
        finish_epoch=finish,
    )
    summary["prometheus_reservas"] = prometheus
    (evidence_dir / "phase-summary.json").write_text(
        json.dumps(summary, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
    )
    return prometheus


def finalize(
    evidence_dir: Path, scenario: str, repetition: int, attempt: int = 1
) -> dict[str, object]:
    if scenario != CORRECTIVE_RELIABILITY:
        raise ValueError("El finalizador sólo admite la campaña correctiva E2")
    expected = Path(scenario) / attempt_directory_name(repetition, attempt)
    if not evidence_dir.resolve().as_posix().endswith(expected.as_posix()):
        raise ValueError("La ruta no corresponde a la campaña correctiva/repetición")

    metadata_path = evidence_dir / "metadata.json"
    metadata = json.loads(metadata_path.read_text(encoding="utf-8-sig"))
    if (
        metadata.get("scenario") != scenario
        or metadata.get("repetition") != repetition
        or metadata.get("attempt", 1) != attempt
    ):
        raise ValueError("Metadata no corresponde a la campaña/repetición/intento")

    validation_errors: list[str] = []
    try:
        metadata.update(request_population(evidence_dir))
    except (OSError, ValueError) as error:
        validation_errors.append(str(error))
        metadata.update(
            business_get_count=0,
            login_request_count=0,
            refresh_request_count=0,
            business_population_valid=False,
            request_names_separated=False,
        )
    try:
        prometheus = _write_phase_summary(evidence_dir)
        metadata["prometheus_reservas_arrival_valid"] = True
        metadata["prometheus_reservas_post_900_activity"] = prometheus[
            "post_900_get_activity"
        ]
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        validation_errors.append(str(error))
        metadata["prometheus_reservas_arrival_valid"] = False
        metadata["prometheus_reservas_post_900_activity"] = 0
    try:
        metadata.update(business_event_metrics(evidence_dir))
    except (OSError, ValueError) as error:
        validation_errors.append(str(error))
        metadata.update(business_event_count=0, business_http_5xx=0)
    metadata["business_events_consistent"] = (
        metadata["business_event_count"] == metadata["business_get_count"]
    )

    missing = sorted(
        name
        for name in CORRECTIVE_EVIDENCE - {"SHA256SUMS"}
        if not (evidence_dir / name).is_file()
        or (
            (evidence_dir / name).stat().st_size == 0
            and name not in EMPTY_ALLOWED_EVIDENCE
        )
    )
    metadata["evidence_complete"] = (
        not missing
        and metadata.get("reservas_log_capture_succeeded") is True
        and metadata.get("gateway_log_capture_succeeded") is True
    )
    if missing:
        validation_errors.append(f"Evidencia ausente o vacía: {', '.join(missing)}")

    metadata["secret_scan_passed"] = _secret_scan(evidence_dir)
    if not metadata["secret_scan_passed"]:
        validation_errors.append("La evidencia contiene un posible secreto")

    elapsed = metadata.get("elapsed_seconds")
    duration_evidence_valid = bool(
        not isinstance(elapsed, bool)
        and isinstance(elapsed, (int, float))
        and math.isfinite(elapsed)
        and elapsed >= 3595
    )
    metadata["duration_evidence_valid"] = duration_evidence_valid
    if not duration_evidence_valid:
        validation_errors.append("La duración real no alcanza la hora con tolerancia de 5 s")

    base_completed = all(
        (
            metadata.get("duration_completed") is True,
            duration_evidence_valid,
            metadata.get("evidence_complete") is True,
            metadata.get("environment_consistent") is True,
            metadata.get("git_worktree_clean_before") is True,
            metadata.get("git_sha") == metadata.get("git_sha_after"),
            metadata.get("business_population_valid") is True,
            metadata.get("request_names_separated") is True,
            metadata.get("business_events_consistent") is True,
            metadata.get("prometheus_reservas_arrival_valid") is True,
            metadata.get("secret_scan_passed") is True,
            isinstance(metadata.get("locust_exit_code"), int)
            and not isinstance(metadata.get("locust_exit_code"), bool),
            not validation_errors,
        )
    )

    metadata["manifest_entries"] = sum(
        1
        for path in evidence_dir.rglob("*")
        if path.is_file() and path.name != "SHA256SUMS"
    )
    metadata["manifest_valid"] = True
    metadata["execution_completed"] = base_completed
    metadata["status"] = "completed" if base_completed else "aborted"
    metadata["launcher_error"] = "; ".join(validation_errors) or None

    # Última escritura de metadata: desde aquí todos los archivos son inmutables.
    _write_metadata(metadata_path, metadata)
    entries = _create_manifest(evidence_dir)
    if entries != metadata["manifest_entries"]:
        raise ValueError("El número de entradas cambió durante la creación del manifiesto")
    if verify_sha256_manifest(evidence_dir) != entries:
        raise ValueError("El manifiesto SHA-256 no verificó")
    return metadata


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--evidence-dir", required=True, type=Path)
    parser.add_argument("--scenario", required=True)
    parser.add_argument("--repetition", required=True, type=int, choices=range(1, 11))
    parser.add_argument("--attempt", type=int, default=1)
    args = parser.parse_args()
    if args.attempt < 1:
        parser.error("--attempt debe ser mayor o igual que 1")
    try:
        metadata = finalize(
            args.evidence_dir, args.scenario, args.repetition, args.attempt
        )
    except (OSError, ValueError, json.JSONDecodeError) as error:
        # No se modifica evidencia potencialmente hasheada. El fallo se comunica
        # exclusivamente por stderr/exit code y el registrador vuelve a verificar.
        print(f"ERROR: {error}", file=sys.stderr)
        return 2
    print(
        json.dumps(
            {
                "execution_completed": metadata["execution_completed"],
                "locust_exit_code": metadata.get("locust_exit_code"),
                "business_get_count": metadata["business_get_count"],
                "manifest_entries": metadata["manifest_entries"],
                "manifest_valid": metadata["manifest_valid"],
            },
            ensure_ascii=False,
        )
    )
    return 0 if metadata["execution_completed"] else 2


if __name__ == "__main__":
    raise SystemExit(main())
