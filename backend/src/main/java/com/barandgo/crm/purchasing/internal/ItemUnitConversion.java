package com.barandgo.crm.purchasing.internal;

import java.math.BigDecimal;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Conversión propia de un artículo: 1 unidad de origen = factor * unidad de destino.
 * Se identifica por el par de unidades; un artículo no tiene dos reglas para el mismo par.
 */
@Embeddable
public class ItemUnitConversion {

    @Column(name = "from_unit_id", nullable = false)
    private Long fromUnitId;

    @Column(name = "to_unit_id", nullable = false)
    private Long toUnitId;

    @Column(nullable = false, precision = 19, scale = 9)
    private BigDecimal factor;

    protected ItemUnitConversion() {
    }

    public ItemUnitConversion(Long fromUnitId, Long toUnitId, BigDecimal factor) {
        this.fromUnitId = fromUnitId;
        this.toUnitId = toUnitId;
        this.factor = factor;
    }

    public Long getFromUnitId() {
        return fromUnitId;
    }

    public Long getToUnitId() {
        return toUnitId;
    }

    public BigDecimal getFactor() {
        return factor;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ItemUnitConversion that)) {
            return false;
        }
        return Objects.equals(fromUnitId, that.fromUnitId) && Objects.equals(toUnitId, that.toUnitId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromUnitId, toUnitId);
    }
}
