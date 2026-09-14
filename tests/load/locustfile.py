"""Carga autenticada de solo lectura para Reservas/Solicitudes."""

import os
import time
from collections.abc import Callable
from typing import Any
from uuid import UUID

from locust import HttpUser, between, task
from locust.exception import StopUser


class ReservasUser(HttpUser):
    """Usuario que consulta el listado y detalles reales de reservas."""

    wait_time = between(1, 3)
    refresh_skew_seconds = 30.0

    def on_start(self) -> None:
        self.reserva_ids: list[str] = []
        self.access_token = ""
        self.refresh_token = ""
        self.access_expires_at = 0.0
        username = os.environ.get("LOCUST_USERNAME")
        password = os.environ.get("LOCUST_PASSWORD")
        if not username or not password:
            raise RuntimeError(
                "LOCUST_USERNAME and LOCUST_PASSWORD are required for authenticated load tests"
            )

        with self.client.post(
            "/api/v1/auth/login",
            json={"username": username, "password": password},
            name="POST /api/v1/auth/login",
            catch_response=True,
        ) as response:
            if response.status_code != 200:
                response.failure(f"Login falló con HTTP {response.status_code}")
                raise StopUser()
            try:
                payload = response.json()
            except ValueError:
                response.failure("Login no devolvió JSON válido")
                raise StopUser() from None
            if not self._store_session(payload):
                response.failure(
                    "Login no devolvió accessToken, refreshToken y expiresIn válidos"
                )
                raise StopUser()

    def _store_session(self, payload: object) -> bool:
        if not isinstance(payload, dict):
            return False
        access_token = payload.get("accessToken")
        refresh_token = payload.get("refreshToken")
        expires_in = payload.get("expiresIn")
        if (
            not isinstance(access_token, str)
            or not access_token
            or not isinstance(refresh_token, str)
            or not refresh_token
            or isinstance(expires_in, bool)
            or not isinstance(expires_in, (int, float))
            or expires_in <= 0
        ):
            return False
        self.access_token = access_token
        self.refresh_token = refresh_token
        self.access_expires_at = time.monotonic() + float(expires_in)
        self.client.headers.update({"Authorization": f"Bearer {access_token}"})
        return True

    def _refresh_session(self) -> bool:
        with self.client.post(
            "/api/v1/auth/refresh",
            json={"refreshToken": self.refresh_token},
            name="POST /api/v1/auth/refresh",
            catch_response=True,
        ) as response:
            if response.status_code != 200:
                response.failure(f"Refresh falló con HTTP {response.status_code}")
                return False
            try:
                payload = response.json()
            except ValueError:
                response.failure("Refresh no devolvió JSON válido")
                return False
            if not self._store_session(payload):
                response.failure(
                    "Refresh no devolvió accessToken, refreshToken y expiresIn válidos"
                )
                return False
        return True

    def _ensure_fresh_access_token(self) -> None:
        if time.monotonic() >= self.access_expires_at - self.refresh_skew_seconds:
            if not self._refresh_session():
                raise StopUser()

    def _business_get(
        self,
        path: str,
        name: str,
        validator: Callable[[Any], None],
        *,
        params: dict[str, object] | None = None,
        allow_refresh: bool = True,
    ) -> None:
        if allow_refresh:
            self._ensure_fresh_access_token()
        with self.client.get(
            path,
            params=params,
            name=name,
            catch_response=True,
        ) as response:
            if response.status_code == 401:
                response.failure("HTTP esperado 200, recibido 401")
                should_retry = allow_refresh and self._refresh_session()
            elif response.status_code != 200:
                response.failure(f"HTTP esperado 200, recibido {response.status_code}")
                return
            else:
                validator(response)
                return
        if should_retry:
            self._business_get(
                path,
                name,
                validator,
                params=params,
                allow_refresh=False,
            )

    @task(3)
    def listar_reservas(self) -> None:
        self._business_get(
            "/api/v1/reservas",
            "GET /api/v1/reservas",
            self._validate_reservas,
            params={"pagina": 0, "tamanio": 20},
        )

    def _validate_reservas(self, response: Any) -> None:
        if not self._es_json(response.headers.get("Content-Type", "")):
            response.failure("Content-Type no es application/json")
            return

        try:
            payload = response.json()
        except ValueError:
            response.failure("La respuesta no contiene JSON válido")
            return

        contenido = payload.get("contenido") if isinstance(payload, dict) else None
        if not isinstance(contenido, list):
            response.failure("La respuesta no contiene una lista 'contenido'")
            return

        ids_validos = [
            item["id"]
            for item in contenido
            if isinstance(item, dict) and self._es_uuid(item.get("id"))
        ]
        if ids_validos:
            self.reserva_ids = ids_validos

    @task(1)
    def obtener_reserva_por_id(self) -> None:
        if not self.reserva_ids:
            self.listar_reservas()
            return

        reserva_id = self.reserva_ids.pop(0)
        self.reserva_ids.append(reserva_id)
        self._business_get(
            f"/api/v1/reservas/{reserva_id}",
            "GET /api/v1/reservas/{id}",
            lambda response: self._validate_reserva(response, reserva_id),
        )

    def _validate_reserva(self, response: Any, reserva_id: str) -> None:
        if not self._es_json(response.headers.get("Content-Type", "")):
            response.failure("Content-Type no es application/json")
            return

        try:
            payload = response.json()
        except ValueError:
            response.failure("La respuesta no contiene JSON válido")
            return

        campos = {
            "id",
            "solicitudId",
            "laboratorioId",
            "responsableId",
            "fechaReserva",
            "horaInicio",
            "horaFin",
            "estado",
            "codigoReserva",
            "creadaEn",
            "actualizadaEn",
            "version",
        }
        if not isinstance(payload, dict) or not campos.issubset(payload):
            response.failure("La respuesta no tiene la estructura de ReservaResponse")
        elif payload["id"] != reserva_id:
            response.failure("El id de la respuesta no coincide con el solicitado")

    @staticmethod
    def _es_json(content_type: str) -> bool:
        return content_type.lower().split(";", maxsplit=1)[0].strip() == "application/json"

    @staticmethod
    def _es_uuid(value: object) -> bool:
        if not isinstance(value, str):
            return False
        try:
            UUID(value)
        except ValueError:
            return False
        return True
