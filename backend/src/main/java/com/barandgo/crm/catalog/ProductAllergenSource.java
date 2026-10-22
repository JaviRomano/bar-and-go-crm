package com.barandgo.crm.catalog;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.barandgo.crm.masterdata.Allergen;

/**
 * Alérgenos de cada producto. La carta los muestra pero no los calcula: salen de los ingredientes del
 * escandallo. La implementa el módulo de escandallos, de modo que la dependencia va de escandallos a
 * carta y no al revés.
 */
public interface ProductAllergenSource {

    /** Productos sin escandallo no aparecen en el resultado. */
    Map<Long, Set<Allergen>> allergensByProduct(Collection<Long> productIds);
}
