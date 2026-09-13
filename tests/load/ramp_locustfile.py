"""Escenario ramp de 0 a 200 usuarios en 10 minutos."""

import math

from locust import LoadTestShape

from locustfile import ReservasUser  # noqa: F401


class RampTo200Users(LoadTestShape):
    """Incrementa linealmente la carga y termina al completar 10 minutos."""

    duration_seconds = 10 * 60
    max_users = 200

    def tick(self) -> tuple[int, float] | None:
        elapsed = self.get_run_time()
        if elapsed >= self.duration_seconds:
            return None

        progress = elapsed / self.duration_seconds
        users = min(self.max_users, math.ceil(self.max_users * progress))
        return users, 1.0
