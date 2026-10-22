package com.barandgo.crm.service.conversion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.barandgo.crm.exception.BadRequestException;
import com.barandgo.crm.service.conversion.UnitConverter.Rule;

class UnitConverterTest {

    // Mismas reglas genéricas que siembra la migración V6
    private final UnitConverter generic = new UnitConverter(List.of(
            rule("kg", "g", "1000"),
            rule("l", "cl", "100"),
            rule("l", "ml", "1000"),
            rule("cl", "ml", "10"),
            rule("doc", "ud", "12")));

    @Test
    void sameUnitReturnsQuantityUnchanged() {
        assertThat(generic.convert(new BigDecimal("2.5"), "kg", "kg")).isEqualByComparingTo("2.5");
    }

    @Test
    void appliesDirectRule() {
        assertThat(generic.convert(new BigDecimal("1.5"), "kg", "g")).isEqualByComparingTo("1500");
    }

    @Test
    void appliesRuleInReverse() {
        assertThat(generic.convert(new BigDecimal("250"), "g", "kg")).isEqualByComparingTo("0.25");
        assertThat(generic.convert(new BigDecimal("33"), "cl", "l")).isEqualByComparingTo("0.33");
    }

    @Test
    void resultHasNoScientificNotation() {
        assertThat(generic.convert(BigDecimal.ONE, "kg", "g").toString()).isEqualTo("1000");
    }

    @Test
    void chainsRulesThroughIntermediateUnits() {
        UnitConverter withBox = generic.withItemRules(List.of(rule("caja", "ud", "24")));

        // caja -> ud -> doc: 1 caja = 24 ud = 2 docenas
        assertThat(withBox.convert(BigDecimal.ONE, "caja", "doc")).isEqualByComparingTo("2");
        assertThat(withBox.convert(new BigDecimal("3"), "doc", "caja")).isEqualByComparingTo("1.5");
    }

    @Test
    void itemRuleConnectsDifferentDimensions() {
        // Botella de vino: 1 ud = 0,75 l. Solo tiene sentido para ese artículo
        UnitConverter wine = generic.withItemRules(List.of(rule("ud", "l", "0.75")));

        assertThat(wine.convert(new BigDecimal("2"), "ud", "cl")).isEqualByComparingTo("150");
        assertThat(wine.convert(new BigDecimal("15"), "cl", "ud")).isEqualByComparingTo("0.2");
    }

    @Test
    void itemRuleOverridesGenericRuleForSamePair() {
        // Docena de huevos de un proveedor que sirve "docenas" de 15 (caso real en algunas compras)
        UnitConverter special = generic.withItemRules(List.of(rule("doc", "ud", "15")));

        assertThat(special.convert(BigDecimal.ONE, "doc", "ud")).isEqualByComparingTo("15");
        assertThat(special.convert(new BigDecimal("30"), "ud", "doc")).isEqualByComparingTo("2");
        // El conversor genérico no cambia
        assertThat(generic.convert(BigDecimal.ONE, "doc", "ud")).isEqualByComparingTo("12");
    }

    @Test
    void nonTerminatingDivisionKeepsPrecisionAndRoundTrips() {
        UnitConverter withBox = generic.withItemRules(List.of(rule("caja", "ud", "24")));

        BigDecimal oneUnitInBoxes = withBox.convert(BigDecimal.ONE, "ud", "caja");
        assertThat(oneUnitInBoxes.setScale(6, RoundingMode.HALF_UP)).isEqualByComparingTo("0.041667");

        BigDecimal back = withBox.convert(oneUnitInBoxes, "caja", "ud");
        assertThat(back.setScale(9, RoundingMode.HALF_UP)).isEqualByComparingTo("1");
    }

    @Test
    void longChainDividesOnlyOnce() {
        // 1 ml -> cl -> l y vuelta: con fracciones acumuladas el resultado es exacto
        BigDecimal liters = generic.convert(new BigDecimal("1"), "ml", "l");
        assertThat(liters).isEqualByComparingTo("0.001");
        assertThat(generic.convert(liters, "l", "ml")).isEqualByComparingTo("1");
    }

    @Test
    void factorIsConversionOfOneUnit() {
        assertThat(generic.factor("l", "ml")).isEqualByComparingTo("1000");
    }

    @Test
    void missingPathIsRejected() {
        assertThat(generic.canConvert("kg", "l")).isFalse();
        assertThatThrownBy(() -> generic.convert(BigDecimal.ONE, "kg", "l"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("'kg'")
                .hasMessageContaining("'l'");
    }

    @Test
    void unknownUnitIsRejected() {
        assertThatThrownBy(() -> generic.convert(BigDecimal.ONE, "kg", "arroba"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void invalidRulesAreRejected() {
        assertThatThrownBy(() -> rule("kg", "g", "0")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> rule("kg", "g", "-1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> rule("kg", "kg", "1")).isInstanceOf(IllegalArgumentException.class);
    }

    private static Rule rule(String from, String to, String factor) {
        return new Rule(from, to, new BigDecimal(factor));
    }
}
