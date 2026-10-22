package com.barandgo.crm.inventory.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.barandgo.crm.inventory.InventoryDTOs.MovementTotalDTO;
import com.barandgo.crm.inventory.MovementType;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Agregado del ledger en la base de datos: los informes trabajan con sumas por artículo, almacén y tipo,
 * no con cada movimiento.
 */
@Component
class MovementTotalsQuery {

    private final EntityManager entityManager;

    MovementTotalsQuery(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    List<MovementTotalDTO> query(Long warehouseId, Instant from, Instant to) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<StockMovement> movement = query.from(StockMovement.class);

        List<Predicate> predicates = new ArrayList<>();
        if (warehouseId != null) {
            predicates.add(cb.equal(movement.get("warehouseId"), warehouseId));
        }
        if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(movement.get("occurredAt"), from));
        }
        if (to != null) {
            predicates.add(cb.lessThan(movement.get("occurredAt"), to));
        }

        Expression<Long> item = movement.get("stockItemId");
        Expression<Long> warehouse = movement.get("warehouseId");
        Expression<MovementType> type = movement.get("type");
        Expression<BigDecimal> quantity = cb.sum(movement.get("quantity"));
        Expression<BigDecimal> value = cb.sum(movement.get("totalCost"));
        query.select(cb.tuple(item, warehouse, type, quantity, value))
                .where(predicates.toArray(Predicate[]::new))
                .groupBy(item, warehouse, type);

        return entityManager.createQuery(query).getResultList().stream()
                .map(row -> new MovementTotalDTO(row.get(item), row.get(warehouse), row.get(type), row.get(quantity),
                        row.get(value)))
                .toList();
    }
}
