package ec.edu.scli.reservas.experimental.application;

import java.util.concurrent.atomic.AtomicLong;

public final class LamportClock {
    private final AtomicLong value = new AtomicLong();
    public long tick() { return value.incrementAndGet(); }
    public long receive(long remote) { return value.updateAndGet(local -> Math.max(local, remote) + 1); }
    public long value() { return value.get(); }
}
