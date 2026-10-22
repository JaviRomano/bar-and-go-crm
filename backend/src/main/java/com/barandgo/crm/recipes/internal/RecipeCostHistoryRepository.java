package com.barandgo.crm.recipes.internal;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecipeCostHistoryRepository extends JpaRepository<RecipeCostHistory, Long> {

    List<RecipeCostHistory> findByRecipeIdOrderByRecordedAtDescIdDesc(Long recipeId);

    Optional<RecipeCostHistory> findFirstByRecipeIdOrderByRecordedAtDescIdDesc(Long recipeId);
}
