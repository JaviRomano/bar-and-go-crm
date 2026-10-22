package com.barandgo.crm.masterdata;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.barandgo.crm.shared.BadRequestException;

/**
 * Conversor de cantidades entre unidades a partir de reglas "1 from = factor * to".
 *
 * <p>Cada regla se usa en los dos sentidos y las reglas se encadenan (caja -> ud -> doc).
 * Las reglas de un artículo concreto (1 caja = 24 ud) sustituyen a las genéricas
 * para el mismo par de unidades.
 *
 * <p>Para no acumular error de redondeo, el camino se recorre como fracción
 * (producto de numeradores / producto de denominadores) y se divide una sola vez al final.
 * Es inmutable y no depende de la persistencia.
 */
public final class UnitConverter {

    /** Precisión de la división final (34 dígitos significativos). */
    public static final MathContext PRECISION = MathContext.DECIMAL128;

    /** 1 fromCode = factor * toCode. */
    public record Rule(String fromCode, String toCode, BigDecimal factor) {

        public Rule {
            Objects.requireNonNull(fromCode, "fromCode");
            Objects.requireNonNull(toCode, "toCode");
            Objects.requireNonNull(factor, "factor");
            if (fromCode.equals(toCode)) {
                throw new IllegalArgumentException("Una conversión necesita dos unidades distintas: " + fromCode);
            }
            if (factor.signum() <= 0) {
                throw new IllegalArgumentException("El factor de conversión debe ser positivo: " + factor);
            }
        }
    }

    // Arista dirigida: multiplicar por numerator y dividir por denominator
    private record Edge(BigDecimal numerator, BigDecimal denominator) {
    }

    private final Map<String, Map<String, Edge>> graph;

    public UnitConverter(Collection<Rule> rules) {
        this(new HashMap<>(), rules);
    }

    private UnitConverter(Map<String, Map<String, Edge>> base, Collection<Rule> rules) {
        this.graph = base;
        rules.forEach(this::put);
    }

    /**
     * Devuelve un conversor nuevo con las reglas de un artículo añadidas; para un mismo par de
     * unidades prevalecen sobre las genéricas. Este conversor no se modifica.
     */
    public UnitConverter withItemRules(Collection<Rule> itemRules) {
        Map<String, Map<String, Edge>> copy = new HashMap<>();
        graph.forEach((unit, edges) -> copy.put(unit, new HashMap<>(edges)));
        return new UnitConverter(copy, itemRules);
    }

    public BigDecimal convert(BigDecimal quantity, String fromCode, String toCode) {
        Objects.requireNonNull(quantity, "quantity");
        if (fromCode.equals(toCode)) {
            return quantity;
        }
        Edge path = findPath(fromCode, toCode);
        BigDecimal result = quantity.multiply(path.numerator()).divide(path.denominator(), PRECISION);
        return normalize(result);
    }

    /** Factor tal que 1 fromCode = factor * toCode. */
    public BigDecimal factor(String fromCode, String toCode) {
        return convert(BigDecimal.ONE, fromCode, toCode);
    }

    public boolean canConvert(String fromCode, String toCode) {
        return fromCode.equals(toCode) || search(fromCode, toCode) != null;
    }

    private void put(Rule rule) {
        graph.computeIfAbsent(rule.fromCode(), unit -> new HashMap<>())
                .put(rule.toCode(), new Edge(rule.factor(), BigDecimal.ONE));
        graph.computeIfAbsent(rule.toCode(), unit -> new HashMap<>())
                .put(rule.fromCode(), new Edge(BigDecimal.ONE, rule.factor()));
    }

    private Edge findPath(String fromCode, String toCode) {
        Edge path = search(fromCode, toCode);
        if (path == null) {
            throw new BadRequestException(
                    "No hay conversión definida entre '" + fromCode + "' y '" + toCode + "'");
        }
        return path;
    }

    // Búsqueda en anchura: el camino más corto usa menos reglas y por tanto menos aproximaciones
    private Edge search(String fromCode, String toCode) {
        if (!graph.containsKey(fromCode) || !graph.containsKey(toCode)) {
            return null;
        }
        Map<String, Edge> reached = new HashMap<>();
        reached.put(fromCode, new Edge(BigDecimal.ONE, BigDecimal.ONE));
        Deque<String> pending = new ArrayDeque<>();
        pending.add(fromCode);

        while (!pending.isEmpty()) {
            String current = pending.poll();
            Edge accumulated = reached.get(current);
            for (Map.Entry<String, Edge> next : graph.getOrDefault(current, Map.of()).entrySet()) {
                if (reached.containsKey(next.getKey())) {
                    continue;
                }
                Edge step = next.getValue();
                Edge combined = new Edge(
                        accumulated.numerator().multiply(step.numerator()),
                        accumulated.denominator().multiply(step.denominator()));
                if (next.getKey().equals(toCode)) {
                    return combined;
                }
                reached.put(next.getKey(), combined);
                pending.add(next.getKey());
            }
        }
        return null;
    }

    // Quita ceros a la derecha sin pasar a notación científica (1000 y no 1E+3)
    private static BigDecimal normalize(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }
}
