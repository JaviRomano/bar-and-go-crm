package com.barandgo.crm.purchasing.internal;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.barandgo.crm.masterdata.UnitConverter;
import com.barandgo.crm.masterdata.UnitOfMeasureDTO;
import com.barandgo.crm.masterdata.UnitOfMeasureService;
import com.barandgo.crm.shared.BadRequestException;

/**
 * Unidades de medida vistas desde compras: traduce entre código (API) e id (base de datos) y
 * monta el conversor de un artículo con sus reglas propias sobre las genéricas.
 */
@Component
class UnitCatalog {

    private final UnitOfMeasureService unitService;

    UnitCatalog(UnitOfMeasureService unitService) {
        this.unitService = unitService;
    }

    /** Instantánea de unidades y reglas genéricas, para usar durante una operación. */
    Units load() {
        List<UnitOfMeasureDTO> units = unitService.getUnits();
        return new Units(
                units.stream().collect(Collectors.toMap(UnitOfMeasureDTO::code, UnitOfMeasureDTO::id)),
                units.stream().collect(Collectors.toMap(UnitOfMeasureDTO::id, UnitOfMeasureDTO::code)),
                unitService.genericConverter());
    }

    record Units(Map<String, Long> idByCode, Map<Long, String> codeById, UnitConverter generic) {

        Long idOf(String code) {
            Long id = idByCode.get(code);
            if (id == null) {
                throw new BadRequestException("Unidad de medida desconocida: '" + code + "'");
            }
            return id;
        }

        String codeOf(Long id) {
            return codeById.get(id);
        }

        UnitConverter converterFor(Collection<ItemUnitConversion> itemConversions) {
            List<UnitConverter.Rule> rules = itemConversions.stream()
                    .map(c -> new UnitConverter.Rule(codeOf(c.getFromUnitId()), codeOf(c.getToUnitId()), c.getFactor()))
                    .toList();
            return generic.withItemRules(rules);
        }
    }
}
