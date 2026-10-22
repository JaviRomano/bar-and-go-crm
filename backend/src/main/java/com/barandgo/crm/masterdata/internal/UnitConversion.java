package com.barandgo.crm.masterdata.internal;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Conversión genérica entre unidades: 1 fromUnit = factor * toUnit.
 * La inversa se deduce, no hace falta guardarla.
 */
@Entity
@Table(name = "unit_conversion")
public class UnitConversion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "from_unit_id", nullable = false)
    private UnitOfMeasure fromUnit;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "to_unit_id", nullable = false)
    private UnitOfMeasure toUnit;

    @Column(nullable = false, precision = 19, scale = 9)
    private BigDecimal factor;

    @Version
    private Long version;

    public UnitConversion() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UnitOfMeasure getFromUnit() {
        return fromUnit;
    }

    public void setFromUnit(UnitOfMeasure fromUnit) {
        this.fromUnit = fromUnit;
    }

    public UnitOfMeasure getToUnit() {
        return toUnit;
    }

    public void setToUnit(UnitOfMeasure toUnit) {
        this.toUnit = toUnit;
    }

    public BigDecimal getFactor() {
        return factor;
    }

    public void setFactor(BigDecimal factor) {
        this.factor = factor;
    }

    public Long getVersion() {
        return version;
    }
}
