import json
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).parents[2]
LAUNCHER = ROOT / "experimentos" / "ejecutar_iso25010.ps1"
BASH_LAUNCHER = ROOT / "experimentos" / "ejecutar_iso25010.sh"
SCENARIO = "fiabilidad_nominal_50u_1h_refresh"


@unittest.skipUnless(shutil.which("powershell"), "PowerShell no disponible")
class CorrectiveLauncherTest(unittest.TestCase):
    def test_dry_run_uses_corrective_path_and_rejects_overwrite(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            evidence_root = Path(temporary) / "raw"
            command = [
                "powershell",
                "-NoProfile",
                "-ExecutionPolicy",
                "Bypass",
                "-File",
                str(LAUNCHER),
                "-Escenario",
                SCENARIO,
                "-Repeticion",
                "1",
                "-HostObjetivo",
                "http://localhost:8080",
                "-EvidenceRoot",
                str(evidence_root),
                "-DryRun",
            ]
            first = subprocess.run(command, cwd=ROOT, capture_output=True, text=True)
            self.assertEqual(0, first.returncode, first.stdout + first.stderr)
            evidence = evidence_root / "_dry-run" / SCENARIO / "rep-01"
            metadata = json.loads(
                (evidence / "metadata.json").read_text(encoding="utf-8-sig")
            )
            self.assertEqual(SCENARIO, metadata["scenario"])
            self.assertEqual(50, metadata["users"])
            self.assertEqual(10, metadata["spawn_rate"])
            self.assertEqual("1h", metadata["planned_duration"])

            second = subprocess.run(command, cwd=ROOT, capture_output=True, text=True)
            self.assertNotEqual(0, second.returncode)
            self.assertIn("ya contiene archivos", second.stdout + second.stderr)

    def test_completed_experiment_exits_zero_without_erasing_locust_code(self) -> None:
        source = LAUNCHER.read_text(encoding="utf-8-sig")
        self.assertIn("if (-not $metadata.execution_completed) { exit 2 }", source)
        self.assertIn("exit 0", source)
        self.assertIn("$metadata.locust_exit_code = $locustExitCode", source)
        self.assertNotIn("exit $locustExitCode", source)

    def test_official_repetition_directory_cannot_be_overwritten(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            evidence_root = Path(temporary) / "raw"
            repetition = evidence_root / SCENARIO / "rep-01"
            repetition.mkdir(parents=True)
            (repetition / "evidence.marker").write_text("preservar\n", encoding="utf-8")
            command = [
                "powershell",
                "-NoProfile",
                "-ExecutionPolicy",
                "Bypass",
                "-File",
                str(LAUNCHER),
                "-Escenario",
                SCENARIO,
                "-Repeticion",
                "1",
                "-HostObjetivo",
                "http://localhost:8080",
                "-EvidenceRoot",
                str(evidence_root),
            ]
            result = subprocess.run(command, cwd=ROOT, capture_output=True, text=True)
            self.assertNotEqual(0, result.returncode)
            self.assertIn("ya contiene archivos", result.stdout + result.stderr)
            self.assertEqual("preservar\n", (repetition / "evidence.marker").read_text())


class UbuntuLauncherTest(unittest.TestCase):
    def test_bash_launcher_is_native_and_does_not_require_pwsh(self) -> None:
        source = BASH_LAUNCHER.read_text(encoding="utf-8")
        self.assertTrue(source.startswith("#!/usr/bin/env bash"))
        self.assertNotIn("pwsh", source)
        self.assertIn("fiabilidad_nominal_50u_1h_refresh", source)
        self.assertIn("--run-time", source)

    def test_preflight_precedes_official_repetition_creation(self) -> None:
        source = BASH_LAUNCHER.read_text(encoding="utf-8")
        self.assertLess(
            source.index('locust_version="$($python_command -m locust --version'),
            source.index('mkdir -p "$evidence_dir"'),
        )
        self.assertLess(
            source.index('curl --fail --silent --show-error "$prometheus_url/-/healthy"'),
            source.index('mkdir -p "$evidence_dir"'),
        )

    def test_powershell_preflight_precedes_official_repetition_creation(self) -> None:
        source = LAUNCHER.read_text(encoding="utf-8-sig")
        self.assertLess(
            source.index("$preflightLocustVersion = (& $pythonCommand -m locust --version"),
            source.index("New-Item -ItemType Directory -Force -Path $evidenceDirectory"),
        )


if __name__ == "__main__":
    unittest.main()
