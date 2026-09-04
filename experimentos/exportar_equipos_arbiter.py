#!/usr/bin/env python3
"""Exporta IDs/estado reales de Académico para una corrida autorizada."""
from __future__ import annotations

import argparse
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import urllib.request


def export(url: str, token: str) -> dict:
    request = urllib.request.Request(url.rstrip("/") + "/api/v1/equipos?page=0&size=1000",
                                     headers={"Authorization": f"Bearer {token}"})
    with urllib.request.urlopen(request, timeout=15) as response:
        payload = json.load(response)
    items = payload.get("content", payload if isinstance(payload, list) else [])
    return {"source": "academico-laboratorios-service", "captured_at": datetime.now(timezone.utc).isoformat(),
            "equipment": [{"equipmentId": str(item["id"]), "laboratoryId": str(item["laboratorioId"]),
                           "status": item["estado"], "active": bool(item["activo"])} for item in items]}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", default=os.environ.get("ACADEMICO_INTERNAL_URL", ""))
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    token = os.environ.get("EXPERIMENTAL_ACADEMICO_TOKEN", "")
    if os.environ.get("EXPERIMENTAL_ARBITER_ENABLED", "false").lower() != "true" or not args.url or not token:
        raise PermissionError("La exportación exige modo experimental, URL y token en variables de entorno")
    args.output.write_text(json.dumps(export(args.url, token), indent=2), encoding="utf-8")


if __name__ == "__main__": main()
