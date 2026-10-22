package com.barandgo.crm.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.barandgo.crm.dto.unit.UnitConversionDTO;
import com.barandgo.crm.entity.UnitConversion;
import com.barandgo.crm.entity.UnitOfMeasure;
import com.barandgo.crm.enums.UnitDimension;
import com.barandgo.crm.exception.BadRequestException;
import com.barandgo.crm.exception.ConflictException;
import com.barandgo.crm.exception.ResourceNotFoundException;
import com.barandgo.crm.repository.UnitConversionRepository;
import com.barandgo.crm.repository.UnitOfMeasureRepository;

@ExtendWith(MockitoExtension.class)
class UnitOfMeasureServiceImplTest {

    private static final UnitOfMeasure KG = unit(1L, "kg", UnitDimension.MASS);
    private static final UnitOfMeasure G = unit(2L, "g", UnitDimension.MASS);
    private static final UnitOfMeasure L = unit(3L, "l", UnitDimension.VOLUME);
    private static final UnitOfMeasure MG = unit(4L, "mg", UnitDimension.MASS);

    @Mock
    private UnitOfMeasureRepository unitRepository;

    @Mock
    private UnitConversionRepository conversionRepository;

    @InjectMocks
    private UnitOfMeasureServiceImpl unitService;

    @Test
    void genericConversionBetweenDimensionsIsRejected() {
        when(unitRepository.findByCode("kg")).thenReturn(Optional.of(KG));
        when(unitRepository.findByCode("l")).thenReturn(Optional.of(L));

        assertThatThrownBy(() -> unitService.createConversion(new UnitConversionDTO(null, "kg", "l", BigDecimal.ONE)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("misma magnitud");
        verify(conversionRepository, never()).save(any());
    }

    @Test
    void conversionAlreadyDerivableIsRejected() {
        // kg -> g existe; g -> kg sería la inversa y contradiría la regla si el factor no cuadra
        when(unitRepository.findByCode("g")).thenReturn(Optional.of(G));
        when(unitRepository.findByCode("kg")).thenReturn(Optional.of(KG));
        when(conversionRepository.findAll()).thenReturn(List.of(conversion(KG, G, "1000")));

        assertThatThrownBy(() -> unitService.createConversion(new UnitConversionDTO(null, "g", "kg", new BigDecimal("0.002"))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("1 g = 0.001 kg");
    }

    @Test
    void newConversionIsSaved() {
        when(unitRepository.findByCode("g")).thenReturn(Optional.of(G));
        when(unitRepository.findByCode("mg")).thenReturn(Optional.of(MG));
        when(conversionRepository.findAll()).thenReturn(List.of(conversion(KG, G, "1000")));
        when(conversionRepository.save(any(UnitConversion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UnitConversionDTO saved = unitService.createConversion(new UnitConversionDTO(null, "g", "mg", new BigDecimal("1000")));

        assertThat(saved.fromUnit()).isEqualTo("g");
        assertThat(saved.toUnit()).isEqualTo("mg");
        assertThat(saved.factor()).isEqualByComparingTo("1000");
    }

    @Test
    void convertUsesStoredRules() {
        when(unitRepository.findByCode("kg")).thenReturn(Optional.of(KG));
        when(unitRepository.findByCode("g")).thenReturn(Optional.of(G));
        when(conversionRepository.findAll()).thenReturn(List.of(conversion(KG, G, "1000")));

        assertThat(unitService.convert(new BigDecimal("0.75"), "kg", "g").result()).isEqualByComparingTo("750");
    }

    @Test
    void convertWithUnknownUnitReturnsNotFound() {
        when(unitRepository.findByCode("kg")).thenReturn(Optional.of(KG));
        when(unitRepository.findByCode("arroba")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> unitService.convert(BigDecimal.ONE, "kg", "arroba"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static UnitOfMeasure unit(Long id, String code, UnitDimension dimension) {
        UnitOfMeasure unit = new UnitOfMeasure();
        unit.setId(id);
        unit.setCode(code);
        unit.setName(code);
        unit.setDimension(dimension);
        return unit;
    }

    private static UnitConversion conversion(UnitOfMeasure from, UnitOfMeasure to, String factor) {
        UnitConversion conversion = new UnitConversion();
        conversion.setFromUnit(from);
        conversion.setToUnit(to);
        conversion.setFactor(new BigDecimal(factor));
        return conversion;
    }
}
