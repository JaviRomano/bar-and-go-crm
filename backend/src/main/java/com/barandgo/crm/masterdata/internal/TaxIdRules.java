package com.barandgo.crm.masterdata.internal;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validación de identificadores fiscales de proveedor.
 *
 * <p>Españoles con dígito de control: NIF de persona física (DNI), NIE, NIF especiales K/L/M y
 * NIF de persona jurídica (antiguo CIF). También se admite el prefijo ES. Para el resto de la UE
 * se comprueba solo el formato del NIF-IVA (prefijo de país + 2 a 13 caracteres): el dígito
 * de control varía por país y se verifica en VIES, fuera del alcance de esta aplicación.
 */
public final class TaxIdRules {

    private static final String DNI_LETTERS = "TRWAGMYFPDXBNJZSQVHLCKE";
    private static final String ENTITY_CONTROL_LETTERS = "JABCDEFGHI";

    private static final Pattern DNI = Pattern.compile("\\d{8}[A-Z]");
    private static final Pattern NIE = Pattern.compile("[XYZ]\\d{7}[A-Z]");
    private static final Pattern SPECIAL_NIF = Pattern.compile("[KLM]\\d{7}[A-Z]");
    private static final Pattern ENTITY_NIF = Pattern.compile("[ABCDEFGHJNPQRSUVW]\\d{7}[0-9A-J]");
    private static final Pattern EU_VAT = Pattern.compile("[A-Z]{2}[A-Z0-9]{2,13}");

    private static final Set<String> EU_COUNTRY_PREFIXES = Set.of(
            "AT", "BE", "BG", "CY", "CZ", "DE", "DK", "EE", "EL", "FI", "FR", "HR", "HU", "IE",
            "IT", "LT", "LU", "LV", "MT", "NL", "PL", "PT", "RO", "SE", "SI", "SK", "XI");

    private TaxIdRules() {
    }

    /** Mayúsculas y sin espacios, guiones ni puntos. */
    public static String normalize(String raw) {
        return raw == null ? null : raw.toUpperCase().replaceAll("[\\s.\\-]", "");
    }

    /** Espera un valor ya normalizado. */
    public static boolean isValid(String taxId) {
        if (taxId == null || taxId.isEmpty()) {
            return false;
        }
        if (taxId.startsWith("ES") && taxId.length() > 2) {
            return isValidSpanish(taxId.substring(2));
        }
        if (looksSpanish(taxId)) {
            return isValidSpanish(taxId);
        }
        return EU_VAT.matcher(taxId).matches() && EU_COUNTRY_PREFIXES.contains(taxId.substring(0, 2));
    }

    private static boolean looksSpanish(String taxId) {
        return DNI.matcher(taxId).matches()
                || NIE.matcher(taxId).matches()
                || SPECIAL_NIF.matcher(taxId).matches()
                || ENTITY_NIF.matcher(taxId).matches();
    }

    private static boolean isValidSpanish(String taxId) {
        if (DNI.matcher(taxId).matches()) {
            return personalLetterMatches(taxId.substring(0, 8), taxId.charAt(8));
        }
        if (NIE.matcher(taxId).matches()) {
            String prefix = String.valueOf("XYZ".indexOf(taxId.charAt(0)));
            return personalLetterMatches(prefix + taxId.substring(1, 8), taxId.charAt(8));
        }
        if (SPECIAL_NIF.matcher(taxId).matches()) {
            return personalLetterMatches(taxId.substring(1, 8), taxId.charAt(8));
        }
        if (ENTITY_NIF.matcher(taxId).matches()) {
            return entityControlMatches(taxId);
        }
        return false;
    }

    private static boolean personalLetterMatches(String digits, char letter) {
        return DNI_LETTERS.charAt(Integer.parseInt(digits) % 23) == letter;
    }

    /**
     * Control del NIF de persona jurídica: dígitos en posición par se suman tal cual; los de
     * posición impar se duplican y se suman sus cifras. El control es (10 - unidades) % 10,
     * expresado como dígito, como letra (JABCDEFGHI) o de las dos formas según el tipo de entidad.
     */
    private static boolean entityControlMatches(String taxId) {
        char type = taxId.charAt(0);
        String digits = taxId.substring(1, 8);
        char control = taxId.charAt(8);

        int sum = 0;
        for (int i = 0; i < digits.length(); i++) {
            int digit = digits.charAt(i) - '0';
            if (i % 2 == 0) {
                int doubled = digit * 2;
                sum += doubled / 10 + doubled % 10;
            } else {
                sum += digit;
            }
        }
        int expected = (10 - sum % 10) % 10;
        char expectedDigit = (char) ('0' + expected);
        char expectedLetter = ENTITY_CONTROL_LETTERS.charAt(expected);

        // Entidades sin ánimo de lucro, organismos públicos y no residentes: siempre letra
        if ("NPQRSW".indexOf(type) >= 0) {
            return control == expectedLetter;
        }
        // Sociedades anónimas y limitadas, comunidades de bienes: siempre dígito
        if ("ABEH".indexOf(type) >= 0) {
            return control == expectedDigit;
        }
        return control == expectedDigit || control == expectedLetter;
    }
}
