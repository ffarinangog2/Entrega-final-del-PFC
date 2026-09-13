import csv
import tempfile
import unittest
from pathlib import Path

from experimentos.generar_boxplots import load_plot_metrics


class BoxplotPopulationTest(unittest.TestCase):
    def test_efficiency_uses_get_raw_instead_of_aggregated_csv_value(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            csv_path = root / "iso25010.csv"
            with csv_path.open("w", encoding="utf-8", newline="") as source:
                writer = csv.DictWriter(
                    source,
                    fieldnames=[
                        "escenario",
                        "repeticion",
                        "p95_ms",
                        "p99_ms",
                        "failure_rate_percent",
                        "valida",
                    ],
                )
                writer.writeheader()
                writer.writerow(
                    {
                        "escenario": "eficiencia_nominal_50u_5m",
                        "repeticion": "2",
                        "p95_ms": "100",
                        "p99_ms": "2000",
                        "failure_rate_percent": "7",
                        "valida": "si",
                    }
                )

            stats_dir = root / "raw" / "eficiencia_nominal_50u_5m" / "rep-02"
            stats_dir.mkdir(parents=True)
            with (stats_dir / "locust_stats.csv").open(
                "w", encoding="utf-8", newline=""
            ) as source:
                writer = csv.DictWriter(
                    source,
                    fieldnames=[
                        "Type",
                        "Name",
                        "Request Count",
                        "Failure Count",
                        "95%",
                        "99%",
                    ],
                )
                writer.writeheader()
                writer.writerows(
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
                    ]
                )

            metrics = load_plot_metrics(csv_path, root / "raw")

            self.assertEqual(metrics["p95 (ms)"], [24.0])
            self.assertEqual(metrics["p99 (ms)"], [40.0])
            self.assertNotIn(2000.0, metrics["p99 (ms)"])


if __name__ == "__main__":
    unittest.main()
