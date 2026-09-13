import importlib.util
import os
import sys
import types
import unittest
from pathlib import Path
from unittest.mock import patch


class StopUser(Exception):
    pass


fake_locust = types.ModuleType("locust")
fake_locust.HttpUser = object
fake_locust.between = lambda *_: None
fake_locust.task = lambda *_: (lambda function: function)
fake_exception = types.ModuleType("locust.exception")
fake_exception.StopUser = StopUser

MODULE_PATH = Path(__file__).parents[2] / "tests" / "load" / "locustfile.py"
SPEC = importlib.util.spec_from_file_location("scli_locustfile_under_test", MODULE_PATH)
assert SPEC and SPEC.loader
locustfile = importlib.util.module_from_spec(SPEC)
with patch.dict(
    sys.modules,
    {"locust": fake_locust, "locust.exception": fake_exception},
):
    SPEC.loader.exec_module(locustfile)


class FakeResponse:
    def __init__(self, status_code: int, payload: object = None) -> None:
        self.status_code = status_code
        self.payload = payload
        self.headers = {"Content-Type": "application/json"}
        self.failures: list[str] = []

    def __enter__(self):
        return self

    def __exit__(self, *_):
        return False

    def json(self):
        return self.payload

    def failure(self, message: str) -> None:
        self.failures.append(message)


class FakeClient:
    def __init__(self, *, posts=None, gets=None) -> None:
        self.headers: dict[str, str] = {}
        self.posts = list(posts or [])
        self.gets = list(gets or [])
        self.post_calls: list[dict[str, object]] = []
        self.get_calls: list[dict[str, object]] = []

    def post(self, path, **kwargs):
        self.post_calls.append({"path": path, **kwargs})
        return self.posts.pop(0)

    def get(self, path, **kwargs):
        self.get_calls.append({"path": path, **kwargs})
        return self.gets.pop(0)


def session(access: str, refresh: str, expires_in: int = 900) -> dict[str, object]:
    return {
        "accessToken": access,
        "refreshToken": refresh,
        "expiresIn": expires_in,
    }


class LocustRefreshTest(unittest.TestCase):
    def user(self, client: FakeClient):
        user = locustfile.ReservasUser()
        user.client = client
        user.reserva_ids = []
        user.access_token = "access-old"
        user.refresh_token = "refresh-old"
        user.access_expires_at = 1000.0
        return user

    def test_login_stores_both_tokens_expiration_and_authorization(self) -> None:
        login = FakeResponse(200, session("access-1", "refresh-1"))
        user = self.user(FakeClient(posts=[login]))

        with patch.dict(
            os.environ,
            {"LOCUST_USERNAME": "docente", "LOCUST_PASSWORD": "secreto"},
            clear=False,
        ), patch.object(locustfile.time, "monotonic", return_value=100.0):
            user.on_start()

        self.assertEqual(user.access_token, "access-1")
        self.assertEqual(user.refresh_token, "refresh-1")
        self.assertEqual(user.access_expires_at, 1000.0)
        self.assertEqual(user.client.headers["Authorization"], "Bearer access-1")
        self.assertEqual(user.client.post_calls[0]["name"], "POST /api/v1/auth/login")

    def test_refreshes_before_expiration_and_rotates_both_tokens(self) -> None:
        refresh = FakeResponse(200, session("access-2", "refresh-2", 600))
        user = self.user(FakeClient(posts=[refresh]))
        user.access_expires_at = 100.0

        with patch.object(locustfile.time, "monotonic", return_value=75.0):
            user._ensure_fresh_access_token()

        self.assertEqual(user.client.post_calls[0]["path"], "/api/v1/auth/refresh")
        self.assertEqual(
            user.client.post_calls[0]["json"], {"refreshToken": "refresh-old"}
        )
        self.assertEqual(user.client.post_calls[0]["name"], "POST /api/v1/auth/refresh")
        self.assertEqual(user.access_token, "access-2")
        self.assertEqual(user.refresh_token, "refresh-2")
        self.assertEqual(user.access_expires_at, 675.0)
        self.assertEqual(user.client.headers["Authorization"], "Bearer access-2")

    def test_401_refreshes_and_retries_get_once(self) -> None:
        first_get = FakeResponse(401)
        retry = FakeResponse(200, {"contenido": []})
        refresh = FakeResponse(200, session("access-2", "refresh-2"))
        user = self.user(FakeClient(posts=[refresh], gets=[first_get, retry]))

        with patch.object(locustfile.time, "monotonic", return_value=0.0):
            user.listar_reservas()

        self.assertEqual(len(user.client.get_calls), 2)
        self.assertEqual(len(user.client.post_calls), 1)
        self.assertEqual(first_get.failures, ["HTTP esperado 200, recibido 401"])
        self.assertEqual(user.client.headers["Authorization"], "Bearer access-2")
        self.assertTrue(
            all(call["name"] == "GET /api/v1/reservas" for call in user.client.get_calls)
        )
        self.assertEqual(user.client.post_calls[0]["name"], "POST /api/v1/auth/refresh")

    def test_second_401_does_not_create_refresh_or_retry_loop(self) -> None:
        first_get = FakeResponse(401)
        second_get = FakeResponse(401)
        refresh = FakeResponse(200, session("access-2", "refresh-2"))
        user = self.user(FakeClient(posts=[refresh], gets=[first_get, second_get]))

        with patch.object(locustfile.time, "monotonic", return_value=0.0):
            user.listar_reservas()

        self.assertEqual(len(user.client.get_calls), 2)
        self.assertEqual(len(user.client.post_calls), 1)
        self.assertEqual(second_get.failures, ["HTTP esperado 200, recibido 401"])

    def test_failed_refresh_is_recorded_and_get_is_not_retried(self) -> None:
        first_get = FakeResponse(401)
        refresh = FakeResponse(503)
        user = self.user(FakeClient(posts=[refresh], gets=[first_get]))

        with patch.object(locustfile.time, "monotonic", return_value=0.0):
            user.listar_reservas()

        self.assertEqual(len(user.client.get_calls), 1)
        self.assertEqual(len(user.client.post_calls), 1)
        self.assertEqual(first_get.failures, ["HTTP esperado 200, recibido 401"])
        self.assertEqual(refresh.failures, ["Refresh falló con HTTP 503"])
        self.assertEqual(user.client.headers, {})


if __name__ == "__main__":
    unittest.main()
