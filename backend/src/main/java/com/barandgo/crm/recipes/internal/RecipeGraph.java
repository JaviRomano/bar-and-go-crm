package com.barandgo.crm.recipes.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Grafo de sub-escandallos. Un ciclo (A usa B, B usa A) haría infinitos el coste y la explosión de
 * ingredientes, así que se rechaza al guardar.
 */
final class RecipeGraph {

    private RecipeGraph() {
    }

    /**
     * Busca un ciclo que pase por {@code start}.
     *
     * @param subRecipesOf sub-escandallos directos de cada escandallo
     * @return el ciclo como lista de ids, empezando y terminando en {@code start}
     */
    static Optional<List<Long>> findCycle(Map<Long, Set<Long>> subRecipesOf, Long start) {
        List<Long> path = new ArrayList<>();
        path.add(start);
        return search(subRecipesOf, start, start, path, new HashSet<>());
    }

    private static Optional<List<Long>> search(Map<Long, Set<Long>> subRecipesOf, Long start, Long current,
                                               List<Long> path, Set<Long> explored) {
        for (Long next : subRecipesOf.getOrDefault(current, Set.of())) {
            if (next.equals(start)) {
                List<Long> cycle = new ArrayList<>(path);
                cycle.add(start);
                return Optional.of(cycle);
            }
            // Un nodo ya explorado sin encontrar el inicio no lleva a él por ningún otro camino
            if (explored.add(next)) {
                path.add(next);
                Optional<List<Long>> found = search(subRecipesOf, start, next, path, explored);
                if (found.isPresent()) {
                    return found;
                }
                path.remove(path.size() - 1);
            }
        }
        return Optional.empty();
    }
}
