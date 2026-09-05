package ec.edu.scli.reservas.experimental.application;

import ec.edu.scli.reservas.experimental.domain.ArbitrajeStrategy;
import java.util.*;

public final class ArbitrajeStrategyResolver {
    private final Map<String, ArbitrajeStrategy> strategies;
    private final String selected;
    public ArbitrajeStrategyResolver(Collection<ArbitrajeStrategy> strategies, String selected) {
        Map<String, ArbitrajeStrategy> indexed = new HashMap<>();
        strategies.forEach(strategy -> indexed.put(strategy.nombre(), strategy));
        this.strategies = Map.copyOf(indexed); this.selected = selected == null ? "" : selected.trim().toLowerCase(Locale.ROOT);
        if (!this.strategies.containsKey(this.selected)) throw new IllegalArgumentException("ARBITER debe ser s0,s1,s2,s3 o s4");
    }
    public ArbitrajeStrategy resolve() { return strategies.get(selected); }
}
