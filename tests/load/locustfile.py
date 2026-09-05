"""Carga autenticada de solo lectura para Reservas/Solicitudes."""

import os
from uuid import UUID

from locust import HttpUser, between, task
from locust.exception import StopUser


class ReservasUser(HttpUser):
    """Usuario que consulta el listado y detalles reales de reservas."""

    wait_time = between(1, 3)

    def on_start(self) -> None:
        self.reserva_ids: list[str] = []
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
                access_token = response.json().get("accessToken")
            except ValueError:
                response.failure("Login no devolvió JSON válido")
                raise StopUser() from None
            if not access_token:
                response.failure("Login no devolvió accessToken")
                raise StopUser()

        self.client.headers.update({"Authorization": f"Bearer {access_token}"})

    @task(3)
    def listar_reservas(self) -> None:
        with self.client.get(
            "/api/v1/reservas",
            params={"pagina": 0, "tamanio": 20},
            name="GET /api/v1/reservas",
            catch_response=True,
        ) as response:
            if response.status_code != 200:
                response.failure(f"HTTP esperado 200, recibido {response.status_code}")
                return
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
        with self.client.get(
            f"/api/v1/reservas/{reserva_id}",
            name="GET /api/v1/reservas/{id}",
            catch_response=True,
        ) as response:
            if response.status_code != 200:
                response.failure(f"HTTP esperado 200, recibido {response.status_code}")
                return
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
