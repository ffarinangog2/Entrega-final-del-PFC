"""Núcleo aislado del experimento ARBITER E4; no participa del flujo productivo."""
from __future__ import annotations

from contextlib import contextmanager
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
import math
import os
import statistics
import threading
import time
from typing import Callable, Iterable, Protocol


STRATEGIES = {"s0", "s1", "s2", "s3", "s4"}


@dataclass(frozen=True)
class ExperimentConfig:
    enabled: bool
    arbiter: str
    internal_key: str

    @classmethod
    def from_env(cls, env: dict[str, str] | None = None) -> "ExperimentConfig":
        values = os.environ if env is None else env
        enabled = values.get("EXPERIMENTAL_ARBITER_ENABLED", "false").lower() == "true"
        arbiter = values.get("ARBITER", "").lower().strip()
        key = values.get("INTERNAL_API_KEY", "").strip()
        if not enabled:
            raise PermissionError("El subsistema ARBITER está deshabilitado")
        if arbiter not in STRATEGIES:
            raise ValueError("ARBITER debe ser uno de s0,s1,s2,s3,s4")
        if not key:
            raise PermissionError("El modo experimental requiere INTERNAL_API_KEY")
        return cls(enabled, arbiter, key)


@dataclass(frozen=True)
class SlotRequest:
    run_id: str
    request_id: str
    equipment_id: str
    laboratory_id: str
    user_id: str
    starts_at: str
    ends_at: str


@dataclass
class Allocation:
    request: SlotRequest
    status: str
    version: int
    strategy: str
    created_at: str
    reason: str | None = None
    node_id: int | None = None
    leader_id: int | None = None
    lamport: int | None = None

    def json(self) -> dict:
        result = asdict(self)
        result["request"] = asdict(self.request)
        return result


class ExperimentalLifecycleLog:
    """Eventos observables para cancelación/liberación y regla de acceso."""
    def __init__(self): self.events: list[dict] = []; self._released: set[str] = set()
    def cancel(self, allocation_id: str) -> None:
        self.events.append({"type": "CANCELLED", "allocation_id": allocation_id, "timestamp": now_utc()})
        if allocation_id not in self._released:
            self._released.add(allocation_id)
            self.events.append({"type": "RELEASED", "allocation_id": allocation_id, "timestamp": now_utc()})
    def access(self, allocation: Allocation | None, user_id: str, equipment_id: str, at: str) -> bool:
        valid = allocation is not None and allocation.status == "CONFIRMED" \
            and allocation.request.user_id == user_id and allocation.request.equipment_id == equipment_id \
            and allocation.request.starts_at <= at < allocation.request.ends_at
        self.events.append({"type": "ACCESS_GRANTED" if valid else "ACCESS_DENIED", "user_id": user_id,
                            "equipment_id": equipment_id, "at": at, "timestamp": now_utc()})
        return valid


def overlaps(a: SlotRequest, b: SlotRequest) -> bool:
    return a.equipment_id == b.equipment_id and a.starts_at < b.ends_at and a.ends_at > b.starts_at


def now_utc() -> str:
    return datetime.now(timezone.utc).isoformat()


class ExperimentalStore(Protocol):
    def direct_allocate(self, request: SlotRequest, strategy: str) -> Allocation: ...
    def snapshot(self, request: SlotRequest) -> tuple[int, list[Allocation]]: ...
    def optimistic_allocate(self, request: SlotRequest, expected_version: int, strategy: str) -> Allocation: ...
    def pessimistic_allocate(self, request: SlotRequest, strategy: str) -> Allocation: ...
    def serializable_allocate(self, request: SlotRequest, strategy: str) -> Allocation: ...
    def allocations(self) -> list[Allocation]: ...


class InMemoryExperimentalStore:
    """Doble ligero. El store Cockroach se usa únicamente en la VM experimental."""
    def __init__(self, before_optimistic_commit: Callable[[], None] | None = None):
        self._rows: list[Allocation] = []
        self._versions: dict[tuple[str, str, str], int] = {}
        self._global = threading.RLock()
        self._locks: dict[tuple[str, str, str], threading.Lock] = {}
        self.before_optimistic_commit = before_optimistic_commit

    @staticmethod
    def key(r: SlotRequest) -> tuple[str, str, str]:
        return r.equipment_id, r.starts_at, r.ends_at

    def _conflict(self, request: SlotRequest) -> bool:
        return any(a.status == "CONFIRMED" and overlaps(a.request, request) for a in self._rows)

    def _result(self, request: SlotRequest, strategy: str, status: str, version: int, reason: str | None = None) -> Allocation:
        row = Allocation(request, status, version, strategy, now_utc(), reason)
        self._rows.append(row)
        return row

    def direct_allocate(self, request: SlotRequest, strategy: str) -> Allocation:
        # Control negativo deliberado: ninguna lectura/decisión atómica.
        return self._result(request, strategy, "CONFIRMED", 0)

    def snapshot(self, request: SlotRequest) -> tuple[int, list[Allocation]]:
        with self._global:
            return self._versions.get(self.key(request), 0), list(self._rows)

    def optimistic_allocate(self, request: SlotRequest, expected_version: int, strategy: str) -> Allocation:
        if self.before_optimistic_commit:
            self.before_optimistic_commit()
        with self._global:
            current = self._versions.get(self.key(request), 0)
            if current != expected_version:
                return self._result(request, strategy, "REJECTED", current, "OPTIMISTIC_CONFLICT")
            if self._conflict(request):
                return self._result(request, strategy, "REJECTED", current, "REAL_CONFLICT")
            current += 1
            self._versions[self.key(request)] = current
            return self._result(request, strategy, "CONFIRMED", current)

    @contextmanager
    def _slot_lock(self, request: SlotRequest):
        with self._global:
            # Una agenda por equipo serializa también franjas parcialmente solapadas.
            lock = self._locks.setdefault((request.equipment_id, "AGENDA", "AGENDA"), threading.Lock())
        with lock:
            yield

    def pessimistic_allocate(self, request: SlotRequest, strategy: str) -> Allocation:
        with self._slot_lock(request):
            if self._conflict(request):
                return self._result(request, strategy, "REJECTED", 0, "REAL_CONFLICT")
            return self._result(request, strategy, "CONFIRMED", 0)

    def serializable_allocate(self, request: SlotRequest, strategy: str) -> Allocation:
        with self._global:
            if self._conflict(request):
                return self._result(request, strategy, "REJECTED", 0, "REAL_CONFLICT")
            return self._result(request, strategy, "CONFIRMED", 0)

    def allocations(self) -> list[Allocation]:
        with self._global:
            return list(self._rows)


class ArbitrajeStrategy(Protocol):
    name: str
    def allocate(self, request: SlotRequest) -> Allocation: ...


class S0DirectStrategy:
    name = "s0"
    def __init__(self, store: ExperimentalStore): self.store = store
    def allocate(self, request: SlotRequest) -> Allocation: return self.store.direct_allocate(request, self.name)


class S1OptimisticStrategy:
    name = "s1"
    def __init__(self, store: ExperimentalStore): self.store = store
    def allocate(self, request: SlotRequest) -> Allocation:
        version, rows = self.store.snapshot(request)
        if any(a.status == "CONFIRMED" and overlaps(a.request, request) for a in rows):
            return Allocation(request, "REJECTED", version, self.name, now_utc(), "REAL_CONFLICT")
        return self.store.optimistic_allocate(request, version, self.name)


class S2PessimisticStrategy:
    name = "s2"
    def __init__(self, store: ExperimentalStore): self.store = store
    def allocate(self, request: SlotRequest) -> Allocation: return self.store.pessimistic_allocate(request, self.name)


class LamportClock:
    def __init__(self): self._value = 0; self._lock = threading.Lock()
    def tick(self) -> int:
        with self._lock: self._value += 1; return self._value
    def receive(self, remote: int) -> int:
        with self._lock: self._value = max(self._value, remote) + 1; return self._value
    @property
    def value(self) -> int: return self._value


@dataclass
class BullyNode:
    node_id: int
    alive: bool = True


class BullyTransport(Protocol):
    """Puerto mínimo; un adaptador TCP puede sustituir al doble lógico en VM."""
    nodes: dict[int, BullyNode]
    heartbeats: dict[int, float]
    def heartbeat(self, node_id: int) -> None: ...
    def fail(self, node_id: int) -> None: ...


class InProcessBullyTransport:
    def __init__(self, node_ids: Iterable[int]):
        self.nodes = {i: BullyNode(i) for i in node_ids}
        self.heartbeats = {i: time.monotonic() for i in self.nodes}
    def heartbeat(self, node_id: int) -> None:
        if node_id not in self.nodes or not self.nodes[node_id].alive:
            raise RuntimeError("Heartbeat de nodo desconocido o inactivo")
        self.heartbeats[node_id] = time.monotonic()
    def fail(self, node_id: int) -> None: self.nodes[node_id].alive = False


class BullyCluster:
    """Tres nodos lógicos reconstruidos para E4; el mayor ID vivo lidera."""
    def __init__(self, node_ids: Iterable[int] = (1, 2, 3), transport: BullyTransport | None = None):
        self.transport = transport or InProcessBullyTransport(node_ids)
        self.nodes = self.transport.nodes
        self.clock = LamportClock()
        self.events: list[dict] = []
        self._serial = threading.RLock()
        self.leader_id = self.elect("startup")

    def elect(self, reason: str) -> int:
        candidates = [n.node_id for n in self.nodes.values() if n.alive]
        if not candidates: raise RuntimeError("No quedan nodos de arbitraje vivos")
        self.leader_id = max(candidates)
        self.events.append({"type": "ELECTION", "leader_id": self.leader_id,
                            "lamport": self.clock.tick(), "reason": reason, "timestamp": now_utc()})
        return self.leader_id

    def heartbeat(self, node_id: int) -> int:
        self.transport.heartbeat(node_id)
        return self.clock.tick()

    def detect_failures(self, timeout_seconds: float, now: float | None = None) -> list[int]:
        instant = time.monotonic() if now is None else now
        failed = []
        for node_id, last_seen in self.transport.heartbeats.items():
            if self.nodes[node_id].alive and instant - last_seen > timeout_seconds:
                self.transport.fail(node_id); failed.append(node_id)
        if failed and self.leader_id in failed: self.elect("heartbeat_timeout")
        return failed

    def fail_leader(self) -> float:
        started = time.perf_counter()
        failed = self.leader_id
        self.transport.fail(failed)
        self.events.append({"type": "LEADER_FAILED", "node_id": failed,
                            "lamport": self.clock.tick(), "timestamp": now_utc()})
        self.elect("leader_failure")
        return (time.perf_counter() - started) * 1000

    @contextmanager
    def ordered(self):
        with self._serial:
            stamp = self.clock.tick()
            yield self.leader_id, stamp


class S3BullyLamportStrategy:
    name = "s3"
    def __init__(self, store: ExperimentalStore, cluster: BullyCluster): self.store, self.cluster = store, cluster
    def allocate(self, request: SlotRequest) -> Allocation:
        with self.cluster.ordered() as (leader, stamp):
            self.cluster.events.append({"type": "REQUEST_ORDERED", "request_id": request.request_id,
                                        "leader_id": leader, "lamport": stamp, "timestamp": now_utc()})
            row = self.store.pessimistic_allocate(request, self.name)
            row.node_id = leader; row.leader_id = leader; row.lamport = stamp
            return row


class S4SerializableQuorumStrategy:
    name = "s4"
    def __init__(self, store: ExperimentalStore): self.store = store
    def allocate(self, request: SlotRequest) -> Allocation: return self.store.serializable_allocate(request, self.name)


def resolve_strategy(config: ExperimentConfig, store: ExperimentalStore,
                     cluster: BullyCluster | None = None) -> ArbitrajeStrategy:
    factories = {
        "s0": lambda: S0DirectStrategy(store),
        "s1": lambda: S1OptimisticStrategy(store),
        "s2": lambda: S2PessimisticStrategy(store),
        "s3": lambda: S3BullyLamportStrategy(store, cluster or BullyCluster()),
        "s4": lambda: S4SerializableQuorumStrategy(store),
    }
    return factories[config.arbiter]()


def jain(values: Iterable[float]) -> float:
    xs = list(values)
    if not xs: raise ValueError("Jain requiere al menos una adjudicación observable")
    denominator = len(xs) * sum(x * x for x in xs)
    return sum(xs) ** 2 / denominator if denominator else 0.0


def a12(group_a: Iterable[float], group_b: Iterable[float]) -> float:
    a, b = list(group_a), list(group_b)
    if not a or not b: raise ValueError("A12 requiere dos grupos no vacíos")
    wins = sum(x > y for x in a for y in b)
    ties = sum(x == y for x in a for y in b)
    return (wins + 0.5 * ties) / (len(a) * len(b))


def mann_whitney(group_a: Iterable[float], group_b: Iterable[float]) -> tuple[float, float]:
    a, b = list(group_a), list(group_b)
    if not a or not b: raise ValueError("Mann-Whitney requiere dos grupos no vacíos")
    combined = sorted((v, 0) for v in a) + sorted((v, 1) for v in b)
    combined.sort(key=lambda item: item[0])
    ranks = [0.0] * len(combined); i = 0
    while i < len(combined):
        j = i
        while j + 1 < len(combined) and combined[j + 1][0] == combined[i][0]: j += 1
        rank = (i + j + 2) / 2
        for k in range(i, j + 1): ranks[k] = rank
        i = j + 1
    rank_a = sum(r for r, item in zip(ranks, combined) if item[1] == 0)
    u_a = rank_a - len(a) * (len(a) + 1) / 2
    mean = len(a) * len(b) / 2
    sd = math.sqrt(len(a) * len(b) * (len(a) + len(b) + 1) / 12)
    z = 0.0 if sd == 0 else (u_a - mean) / sd
    p_two_sided = math.erfc(abs(z) / math.sqrt(2))
    return u_a, p_two_sided


def wilson_interval(successes: int, total: int, z: float = 1.959963984540054) -> tuple[float, float]:
    if total <= 0: raise ValueError("El intervalo binomial requiere observaciones")
    p = successes / total; denominator = 1 + z * z / total
    centre = (p + z * z / (2 * total)) / denominator
    radius = z * math.sqrt(p * (1 - p) / total + z * z / (4 * total * total)) / denominator
    return max(0.0, centre - radius), min(1.0, centre + radius)


def descriptive(values: Iterable[float]) -> dict[str, float]:
    xs = list(values)
    if not xs: raise ValueError("No existen observaciones")
    sd = statistics.stdev(xs) if len(xs) > 1 else 0.0
    margin = 1.96 * sd / math.sqrt(len(xs))
    mean = statistics.mean(xs)
    return {"n": len(xs), "mean": mean, "sd": sd, "median": statistics.median(xs),
            "ci95_low": mean - margin, "ci95_high": mean + margin}
