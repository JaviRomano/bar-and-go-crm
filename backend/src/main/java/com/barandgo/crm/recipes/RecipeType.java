package com.barandgo.crm.recipes;

/**
 * DISH: escandallo de un producto de la carta (plato o bebida), por raciones.
 * PREPARATION: elaboración intermedia (fondo, salsa, masa) que otros escandallos usan como
 * sub-escandallo; si tiene artículo de stock asociado, se produce por adelantado y se guarda.
 */
public enum RecipeType {
    DISH,
    PREPARATION
}
