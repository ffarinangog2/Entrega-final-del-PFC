import json
from pathlib import Path
import sys
import threading
import tempfile
from unittest.mock import patch
import unittest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from arbiter_core import (Allocation, BullyCluster, ExperimentConfig, ExperimentalLifecycleLog, InMemoryExperimentalStore,
                          LamportClock, SlotRequest, a12, descriptive, jain, mann_whitney,
                          resolve_strategy, wilson_interval)
from analizar_arbiter import compare, valid_samples
from oraculo_reservas import evaluate
from generar_evidencia_arbiter import manifests, sha256sums
from planificar_arbiter import build_plan
from caida_coordinador import execute_s4_failure
from generador_rafagas import run_burst


def request(index=1, equipment="eq-1", user=None):
    return SlotRequest("run-1", f"req-{index}", equipment, "lab-1", f"user-{index}" if user is None else user,
                       "2026-09-07T08:00:00+00:00", "2026-09-07T10:00:00+00:00")


def config(strategy):
    return ExperimentConfig.from_env({"EXPERIMENTAL_ARBITER_ENABLED": "true", "ARBITER": strategy,
                                      "INTERNAL_API_KEY": "test-only"})


class GuardsAndStrategiesTest(unittest.TestCase):
    def test_guards_require_explicit_mode_strategy_and_internal_key(self):
        with self.assertRaises(PermissionError): ExperimentConfig.from_env({"ARBITER": "s0"})
        with self.assertRaises(ValueError): ExperimentConfig.from_env({"EXPERIMENTAL_ARBITER_ENABLED": "true", "ARBITER": "x", "INTERNAL_API_KEY": "k"})
        with self.assertRaises(PermissionError): ExperimentConfig.from_env({"EXPERIMENTAL_ARBITER_ENABLED": "true", "ARBITER": "s0"})

    def test_s4_failure_injector_is_inaccessible_outside_experimental_mode(self):
        with patch.dict("os.environ", {}, clear=True):
            with self.assertRaises(PermissionError):
                execute_s4_failure("safe-command", "CONFIRM_EXPERIMENTAL_NODE_FAILURE", "http://internal/health")

    def test_generator_requires_real_reservas_endpoint(self):
        env = {"EXPERIMENTAL_ARBITER_ENABLED": "true", "ARBITER": "s4", "INTERNAL_API_KEY": "test"}
        with patch.dict("os.environ", env, clear=True), patch("generador_rafagas.git_sha", return_value="abc123"), \
                tempfile.TemporaryDirectory() as directory:
            with self.assertRaisesRegex(RuntimeError, "RESERVAS_EXPERIMENTAL_URL"):
                run_burst("s4", "esc2", 1, 7, "eq", "lab", "2026-09-07T08:00:00+00:00",
                          "2026-09-07T10:00:00+00:00", Path(directory))

    def test_generator_sends_barrier_burst_to_reservas_service(self):
        env = {"EXPERIMENTAL_ARBITER_ENABLED": "true", "ARBITER": "s1"}
        calls = []
        def post(url, payload, key):
            calls.append((url, payload, key))
            return {"estado": "CONFIRMED", "requestId": payload["requestId"]}
        with patch.dict("os.environ", env, clear=True), patch("generador_rafagas.git_sha", return_value="abc123"), \
                tempfile.TemporaryDirectory() as directory:
            manifest = run_burst("s1", "esc2", 1, 7, str(__import__("uuid").uuid4()),
                                 str(__import__("uuid").uuid4()), "2026-09-07T08:00:00+00:00",
                                 "2026-09-07T10:00:00+00:00", Path(directory),
                                 "http://reservas:8084/api/v1/internal/experimentos/arbiter/adjudicar",
                                 "internal-test-key", http_post=post)
        self.assertEqual(len(calls), 50)
        self.assertEqual(manifest["decision_source"], "reservas-solicitudes-service")
        self.assertTrue(all(call[2] == "internal-test-key" for call in calls))

    def test_selector_resolves_five_independent_strategies(self):
        names = [resolve_strategy(config(name), InMemoryExperimentalStore()).name for name in sorted({"s0", "s1", "s2", "s3", "s4"})]
        self.assertEqual(names, ["s0", "s1", "s2", "s3", "s4"])

    def test_s0_is_negative_control_and_allows_double_allocation(self):
        store = InMemoryExperimentalStore(); strategy = resolve_strategy(config("s0"), store)
        self.assertEqual(strategy.allocate(request(1)).status, "CONFIRMED")
        self.assertEqual(strategy.allocate(request(2)).status, "CONFIRMED")

    def test_s1_uses_real_compare_and_set_version_conflict(self):
        barrier = threading.Barrier(2); store = InMemoryExperimentalStore(barrier.wait)
        strategy = resolve_strategy(config("s1"), store); results = []
        threads = [threading.Thread(target=lambda r=request(i): results.append(strategy.allocate(r))) for i in (1, 2)]
        for thread in threads: thread.start()
        for thread in threads: thread.join()
        self.assertEqual(sorted(r.status for r in results), ["CONFIRMED", "REJECTED"])
        self.assertIn("OPTIMISTIC_CONFLICT", {r.reason for r in results})

    def test_s2_serializes_equipment_slot(self):
        store = InMemoryExperimentalStore(); strategy = resolve_strategy(config("s2"), store)
        results = [strategy.allocate(request(1)), strategy.allocate(request(2))]
        self.assertEqual([r.status for r in results], ["CONFIRMED", "REJECTED"])

    def test_s3_bully_uses_highest_live_node_and_lamport(self):
        cluster = BullyCluster((1, 2, 3)); strategy = resolve_strategy(config("s3"), InMemoryExperimentalStore(), cluster)
        first = strategy.allocate(request(1)); recovery = cluster.fail_leader(); second = strategy.allocate(request(2, equipment="eq-2"))
        self.assertEqual((first.leader_id, second.leader_id), (3, 2))
        self.assertGreater(second.lamport, first.lamport)
        self.assertGreaterEqual(recovery, 0)
        self.assertEqual([e["type"] for e in cluster.events],
                         ["ELECTION", "REQUEST_ORDERED", "LEADER_FAILED", "ELECTION", "REQUEST_ORDERED"])

    def test_lamport_receive_advances_beyond_remote(self):
        clock = LamportClock(); self.assertEqual(clock.tick(), 1); self.assertEqual(clock.receive(8), 9)

    def test_heartbeat_detects_failure_and_bully_reelects(self):
        cluster = BullyCluster((1, 2, 3)); cluster.heartbeat(1); cluster.heartbeat(2)
        cluster.transport.heartbeats[1] = 100; cluster.transport.heartbeats[2] = 100; cluster.transport.heartbeats[3] = 0
        self.assertEqual(cluster.detect_failures(10, now=105), [3])
        self.assertEqual(cluster.leader_id, 2)

    def test_s4_delegates_serializable_decision_to_store(self):
        store = InMemoryExperimentalStore(); strategy = resolve_strategy(config("s4"), store)
        self.assertEqual(strategy.allocate(request(1)).status, "CONFIRMED")
        self.assertEqual(strategy.allocate(request(2)).reason, "REAL_CONFLICT")

    def test_real_barrier_releases_workers_together(self):
        barrier = threading.Barrier(3); passed = []
        threads = [threading.Thread(target=lambda: (barrier.wait(), passed.append(True))) for _ in range(2)]
        for thread in threads: thread.start()
        self.assertEqual(passed, []); barrier.wait()
        for thread in threads: thread.join()
        self.assertEqual(len(passed), 2)

    def test_lifecycle_releases_once_and_denies_access_without_current_allocation(self):
        log = ExperimentalLifecycleLog(); row = resolve_strategy(config("s2"), InMemoryExperimentalStore()).allocate(request(1))
        log.cancel("req-1"); log.cancel("req-1")
        self.assertEqual(sum(e["type"] == "RELEASED" for e in log.events), 1)
        self.assertTrue(log.access(row, "user-1", "eq-1", "2026-09-07T09:00:00+00:00"))
        self.assertFalse(log.access(None, "intruder", "eq-1", "2026-09-07T09:00:00+00:00"))


class OracleAndStatisticsTest(unittest.TestCase):
    def manifest(self, rows, events=None):
        return {"run_id": "run-1", "records": [{"type": "REQUEST", "allocation": r.json()} for r in rows],
                "events": events or []}

    def fixture(self, status="OPERATIVO"):
        return {"source": "synthetic-versioned-fixture", "equipment": [
            {"equipmentId": "eq-1", "laboratoryId": "lab-1", "status": status, "active": True}]}

    def allocation(self, index, status="CONFIRMED", reason=None, user=None):
        return Allocation(request(index, user=user), status, 0, "s0", "2026-09-04T00:00:00Z", reason)

    def test_oracle_checks_all_five_invariants(self):
        rows = [self.allocation(1), self.allocation(2)]
        events = [{"type": "CANCELLED", "allocation_id": "req-1"},
                  {"type": "ACCESS_GRANTED", "user_id": "intruder", "equipment_id": "eq-1", "at": "2026-09-07T09:00:00+00:00"}]
        result = evaluate(self.manifest(rows, events), self.fixture("MANTENIMIENTO"))
        self.assertEqual(result["invariants"], {
            "I1_NO_DOUBLE_ALLOCATION": "FAIL", "I2_EXACTLY_ONE_ASSIGNEE": "PASS",
            "I3_NOT_IN_MAINTENANCE": "FAIL", "I4_RELEASE_EXACTLY_ONCE": "FAIL",
            "I5_VALID_ACCESS_ONLY": "FAIL"})

    def test_oracle_marks_unobserved_and_computes_unnecessary_rejection(self):
        rows = [self.allocation(1), self.allocation(2, "REJECTED", "TRANSIENT_FAILURE", user="user-2")]
        result = evaluate(self.manifest(rows), self.fixture())
        self.assertEqual(result["invariants"]["I4_RELEASE_EXACTLY_ONCE"], "NOT_OBSERVED")
        self.assertEqual(result["invariants"]["I5_VALID_ACCESS_ONLY"], "NOT_OBSERVED")
        self.assertEqual(result["unnecessary_rejections"], 0)  # sí existía conflicto real final

    def test_oracle_accepts_single_release_and_valid_access(self):
        row = self.allocation(1)
        events = [{"type": "CANCELLED", "allocation_id": "req-1"},
                  {"type": "RELEASED", "allocation_id": "req-1"},
                  {"type": "ACCESS_GRANTED", "user_id": "user-1", "equipment_id": "eq-1", "at": "2026-09-07T09:00:00+00:00"}]
        result = evaluate(self.manifest([row], events), self.fixture())
        self.assertEqual(result["invariants"]["I4_RELEASE_EXACTLY_ONCE"], "PASS")
        self.assertEqual(result["invariants"]["I5_VALID_ACCESS_ONLY"], "PASS")

    def test_oracle_detects_missing_assignee_and_duplicate_release(self):
        row = self.allocation(1, user="")
        events = [{"type": "CANCELLED", "allocation_id": "req-1"},
                  {"type": "RELEASED", "allocation_id": "req-1"},
                  {"type": "RELEASED", "allocation_id": "req-1"}]
        result = evaluate(self.manifest([row], events), self.fixture())
        self.assertEqual(result["invariants"]["I2_EXACTLY_ONE_ASSIGNEE"], "FAIL")
        self.assertEqual(result["invariants"]["I4_RELEASE_EXACTLY_ONCE"], "FAIL")

    def test_statistics_are_reproducible(self):
        self.assertAlmostEqual(jain([1, 1, 1]), 1.0)
        self.assertAlmostEqual(a12([3, 4], [1, 2]), 1.0)
        u, p = mann_whitney([3, 4], [1, 2]); self.assertEqual(u, 4.0); self.assertGreaterEqual(p, 0)
        self.assertEqual(descriptive([1, 2, 3])["median"], 2)
        low, high = wilson_interval(0, 10); self.assertEqual(low, 0.0); self.assertGreater(high, 0)

    def test_analysis_discards_first_and_last_repetition(self):
        rows = [{"repetition": rep, "strategy": strategy, "latency_ms": rep, "valid": True}
                for strategy in ("s1", "s2") for rep in range(1, 11)]
        self.assertEqual(len(valid_samples(rows)), 16)
        result = compare(rows, "latency_ms", "s1", "s2")
        self.assertEqual(result["group_a_summary"]["n"], 8)
        self.assertEqual(result["favorable_direction"], "lower")

    def test_official_matrix_has_exactly_130_planned_runs(self):
        plan = build_plan()
        self.assertEqual(len(plan), 130)
        self.assertEqual(sum(r["scenario"] == "esc1" for r in plan), 10)
        self.assertEqual(sum(r["scenario"] == "esc2" for r in plan), 50)
        self.assertEqual(sum(r["scenario"] == "esc3" for r in plan), 50)
        self.assertEqual(sum(r["scenario"] == "esc4" for r in plan), 20)
        self.assertEqual(sum(r["analysis"] for r in plan), 104)

    def test_raw_parser_requires_metadata_and_hashes_without_touching_historical_manifest(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); raw = root / "raw"; raw.mkdir()
            payload = {"run_id": "r", "strategy": "s2", "scenario": "esc2", "repetition": 2,
                       "seed": 7, "sha": "abc", "records": []}
            (raw / "run.json").write_text(json.dumps(payload), encoding="utf-8")
            self.assertEqual(manifests(raw), [payload])
            sha256sums(root, root / "SHA256SUMS")
            self.assertIn("raw/run.json", (root / "SHA256SUMS").read_text(encoding="utf-8"))


if __name__ == "__main__": unittest.main()
