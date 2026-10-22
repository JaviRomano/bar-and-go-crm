package com.barandgo.crm.masterdata.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TaxIdRulesTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "12345678Z",    // DNI: 12345678 % 23 = 14 -> Z
            "00000000T",    // DNI: resto 0 -> T
            "X1234567L",    // NIE: X -> 0, 01234567 % 23 = 19 -> L
            "Y1234567X",    // NIE: Y -> 1, 11234567 % 23 = 10 -> X
            "K1234567L",    // NIF especial K: se calcula sobre los 7 dígitos
            "B12345674",    // Sociedad limitada: control numérico 4
            "A58818501",    // Sociedad anónima: control numérico 1
            "Q2826000H",    // Organismo público: control en letra
            "G12345674",    // Asociación: admite dígito...
            "G1234567D",    // ...o la letra equivalente
            "ESB12345674",  // Con prefijo de país
            "FR40303265045", // NIF-IVA francés: solo formato
            "DE123456789"   // NIF-IVA alemán: solo formato
    })
    void acceptsValidTaxIds(String taxId) {
        assertThat(TaxIdRules.isValid(taxId)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "12345678A",    // letra de DNI incorrecta
            "X1234567T",    // letra de NIE incorrecta
            "B12345670",    // control de sociedad incorrecto
            "B1234567D",    // sociedad limitada con control en letra
            "Q28260008",    // organismo público con control en dígito
            "ES12345678A",  // prefijo ES con DNI incorrecto
            "US123456789",  // país fuera de la UE
            "1234567Z",     // longitud incorrecta
            "",
            "ZZ"
    })
    void rejectsInvalidTaxIds(String taxId) {
        assertThat(TaxIdRules.isValid(taxId)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "' b-1234567 4 ', B12345674",
            "x.1234567.l, X1234567L",
            "12345678z, 12345678Z"
    })
    void normalizesCaseAndSeparators(String raw, String expected) {
        assertThat(TaxIdRules.normalize(raw)).isEqualTo(expected);
    }
}
