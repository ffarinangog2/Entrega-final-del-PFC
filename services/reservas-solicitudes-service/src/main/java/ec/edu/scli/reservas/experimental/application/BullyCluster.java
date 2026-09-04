package ec.edu.scli.reservas.experimental.application;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public final class BullyCluster {
    public record Event(String type, int nodeId, int leaderId, long lamport, Instant at) { }
    private final NavigableMap<Integer, Boolean> nodes = new TreeMap<>();
    private final Map<Integer, Instant> heartbeats = new HashMap<>();
    private final List<Event> events = new ArrayList<>();
    private final ReentrantLock orderLock = new ReentrantLock(true);
    private final LamportClock lamport;
    private final Clock clock;
    private volatile int leaderId;

    public BullyCluster(LamportClock lamport, Clock clock, int... nodeIds) {
        this.lamport = lamport; this.clock = clock;
        for (int id : nodeIds) { nodes.put(id, true); heartbeats.put(id, clock.instant()); }
        elect("STARTUP");
    }
    public synchronized int elect(String reason) {
        leaderId = nodes.descendingMap().entrySet().stream().filter(Map.Entry::getValue)
                .map(Map.Entry::getKey).findFirst().orElseThrow(() -> new IllegalStateException("No hay nodos vivos"));
        events.add(new Event("ELECTION_" + reason, leaderId, leaderId, lamport.tick(), clock.instant()));
        return leaderId;
    }
    public synchronized void heartbeat(int nodeId) {
        if (!Boolean.TRUE.equals(nodes.get(nodeId))) throw new IllegalStateException("Nodo inactivo");
        heartbeats.put(nodeId, clock.instant()); lamport.tick();
    }
    public synchronized long failLeader() {
        Instant start = clock.instant(); int failed = leaderId; nodes.put(failed, false);
        events.add(new Event("LEADER_FAILED", failed, failed, lamport.tick(), start)); elect("FAILURE");
        return Math.max(0, java.time.Duration.between(start, clock.instant()).toMillis());
    }
    public synchronized List<Integer> detectFailures(java.time.Duration timeout) {
        List<Integer> failed = new ArrayList<>(); Instant now = clock.instant();
        heartbeats.forEach((id, last) -> { if (Boolean.TRUE.equals(nodes.get(id)) && last.plus(timeout).isBefore(now)) {
            nodes.put(id, false); failed.add(id); } });
        if (failed.contains(leaderId)) elect("HEARTBEAT_TIMEOUT");
        return List.copyOf(failed);
    }
    public OrderedRequest acquireOrder() { orderLock.lock(); return new OrderedRequest(leaderId, lamport.tick(), orderLock); }
    public int leaderId() { return leaderId; }
    public synchronized List<Event> events() { return List.copyOf(events); }

    public record OrderedRequest(int leaderId, long lamport, ReentrantLock lock) implements AutoCloseable {
        public void close() { lock.unlock(); }
    }
}
