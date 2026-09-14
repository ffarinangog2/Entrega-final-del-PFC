import csv
import tempfile
import unittest
from pathlib import Path

from experimentos.analizar_iso25010 import read_efficiency_population


FIELDNAMES = [
    "Type",
    "Name",
    "Request Count",
    "Failure Count",
    "95%",
    "99%",
]


class EfficiencyPopulationTest(unittest.TestCase):
    def write_stats(self, root: Path, rows: list[dict[str, str]]) -> None:
        target = root / "eficiencia_nominal_50u_5m" / "rep-02"
        target.mkdir(parents=True)
        with (target / "locust_stats.csv").open("w", encoding="utf-8", newline="") as file:
            writer = csv.DictWriter(file, fieldnames=FIELDNAMES)
            writer.writeheader()
            writer.writerows(rows)

    def test_selects_get_by_identity_and_excludes_login(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.write_stats(
                root,
                [
                    {
                        "Type": "GET",
                        "Name": "GET /api/v1/reservas",
                        "Request Count": "100",
                        "Failure Count": "7",
                        "95%": "24",
                        "99%": "40",
                    },
                    {
                        "Type": "POST",
                        "Name": "POST /api/v1/auth/login",
                        "Request Count": "50",
                        "Failure Count": "0",
                        "95%": "9400",
                        "99%": "9500",
                    },
                    {
                        "Type": "",
                        "Name": "Aggregated",
                        "Request Count": "150",
                        "Failure Count": "7",
                        "95%": "100",
                        "99%": "2000",
                    },
                ],
            )

            result = read_efficiency_population(root, [2])[2]

            self.assertEqual(result["request"], "GET /api/v1/reservas")
            self.assertEqual(result["total_requests"], 100)
            self.assertEqual(result["failures"], 7)
            self.assertEqual(result["p95_ms"], 24.0)
            self.assertEqual(result["p99_ms"], 40.0)

    def test_rejects_unclassified_active_request(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.write_stats(
                root,
                [
                    {
                        "Type": "GET",
                        "Name": "GET /api/v1/otro",
                        "Request Count": "1",
                        "Failure Count": "0",
                        "95%": "10",
                        "99%": "20",
                    }
                ],
            )

            with self.assertRaisesRegex(ValueError, "sin clasificación poblacional"):
                read_efficiency_population(root, [2])

    def test_rejects_two_active_get_rows_without_joint_distribution(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.write_stats(
                root,
                [
                    {
                        "Type": "GET",
                        "Name": "GET /api/v1/reservas",
                        "Request Count": "100",
                        "Failure Count": "0",
                        "95%": "20",
                        "99%": "40",
                    },
                    {
                        "Type": "GET",
                        "Name": "GET /api/v1/reservas/{id}",
                        "Request Count": "20",
                        "Failure Count": "2",
                        "95%": "30",
                        "99%": "50",
                    },
                ],
            )

            with self.assertRaisesRegex(ValueError, "distribución raw combinable"):
                read_efficiency_population(root, [2])


if __name__ == "__main__":
    unittest.main()
