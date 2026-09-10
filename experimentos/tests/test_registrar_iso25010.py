import argparse
import csv
import json
import tempfile
import unittest
from pathlib import Path

from experimentos import registrar_iso25010


class RegistrarFiabilidadTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.evidence = self.root / "fiabilidad_nominal_50u_1h" / "rep-02"
        self.evidence.mkdir(parents=True)
        for name in registrar_iso25010.RELIABILITY_EVIDENCE:
            (self.evidence / name).write_text("evidencia\n", encoding="utf-8")
        self.csv_path = self.root / "iso25010.csv"
        self.csv_path.write_text(
            "escenario,repeticion,usuarios,duracion,total_requests,failures,"
            "failure_rate_percent,p95_ms,p99_ms,valida,observacion\n"
            "fiabilidad_nominal_50u_1h,2,50,1h,,,,,,,\n",
            encoding="utf-8",
        )

    def tearDown(self):
        self.temp.cleanup()

    def metadata(self, *, completed=True, exit_code=0):
        data = {
            "status": "completed" if completed else "aborted",
            "scenario": "fiabilidad_nominal_50u_1h",
            "repetition": 2,
            "users": 50,
            "spawn_rate": 10,
            "planned_duration": "1h",
            "planned_duration_seconds": 3600,
            "started_at_utc": "2026-09-10T10:00:00Z",
            "finished_at_utc": "2026-09-10T11:00:00Z",
            "git_branch": "feature/entrega-4",
            "git_sha": "a" * 40,
            "git_worktree_clean_before": True,
            "python_version": "Python 3.12.0",
            "locust_version": "locust 2.31.6",
            "deployment_fingerprint_before": "container image digest",
            "deployment_fingerprint_after": "container image digest",
            "environment_consistent": completed,
            "duration_completed": completed,
            "execution_completed": completed,
            "evidence_complete": completed,
            "locust_exit_code": exit_code,
        }
        (self.evidence / "metadata.json").write_text(json.dumps(data), encoding="utf-8")

    def args(self, *, http_5xx=0):
        return argparse.Namespace(
            scenario="fiabilidad_nominal_50u_1h",
            repetition=2,
            total_requests=1000,
            http_5xx=http_5xx,
            p95_ms=100.0,
            p99_ms=200.0,
            evidence_dir=self.evidence,
            observation="",
            csv_path=self.csv_path,
        )

    def read_row(self):
        with self.csv_path.open(encoding="utf-8", newline="") as stream:
            return next(csv.DictReader(stream))

    def test_completed_without_errors_is_valid(self):
        self.metadata(exit_code=0)
        registrar_iso25010.update_csv(self.args())
        self.assertEqual("si", self.read_row()["valida"])

    def test_completed_with_real_http_500_is_valid_and_preserved(self):
        self.metadata(exit_code=1)
        registrar_iso25010.update_csv(self.args(http_5xx=7))
        row = self.read_row()
        self.assertEqual("7", row["failures"])
        self.assertEqual("0.700000", row["failure_rate_percent"])
        self.assertEqual("si", row["valida"])

    def test_aborted_execution_is_rejected(self):
        self.metadata(completed=False, exit_code=2)
        with self.assertRaisesRegex(ValueError, "no consta como completada"):
            registrar_iso25010.update_csv(self.args())

    def test_incomplete_evidence_is_rejected(self):
        self.metadata()
        (self.evidence / "prometheus-p95-result.txt").unlink()
        with self.assertRaisesRegex(ValueError, "Falta evidencia real"):
            registrar_iso25010.update_csv(self.args())


if __name__ == "__main__":
    unittest.main()
