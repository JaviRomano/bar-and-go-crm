package com.barandgo.crm.masterdata.internal;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barandgo.crm.masterdata.VatRateDTO;
import com.barandgo.crm.masterdata.VatRateService;
import com.barandgo.crm.shared.ConflictException;
import com.barandgo.crm.shared.ResourceNotFoundException;

@Service
@Transactional
public class VatRateServiceImpl implements VatRateService {

    private static final Logger logger = LoggerFactory.getLogger(VatRateServiceImpl.class);

    private final VatRateRepository vatRateRepository;

    public VatRateServiceImpl(VatRateRepository vatRateRepository) {
        this.vatRateRepository = vatRateRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VatRateDTO> getVatRates() {
        return vatRateRepository.findAllByOrderByRateDesc().stream().map(VatRateDTO::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VatRateDTO getVatRate(Long id) {
        return VatRateDTO.from(findVatRate(id));
    }

    @Override
    public VatRateDTO createVatRate(VatRateDTO vatRateDTO) {
        String name = vatRateDTO.name().trim();
        if (vatRateRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Ya existe un tipo de IVA con el nombre '" + name + "'");
        }

        VatRate vatRate = new VatRate();
        vatRate.setName(name);
        vatRate.setRate(vatRateDTO.rate());
        vatRate.setActive(vatRateDTO.active() != null ? vatRateDTO.active() : true);

        VatRate saved = vatRateRepository.save(vatRate);
        logger.info("Tipo de IVA '{}' creado: {} %", saved.getName(), saved.getRate());
        return VatRateDTO.from(saved);
    }

    // Cambiar el porcentaje no altera documentos ya creados: cada línea guarda el suyo
    @Override
    public VatRateDTO updateVatRate(Long id, VatRateDTO vatRateDTO) {
        VatRate vatRate = findVatRate(id);
        String name = vatRateDTO.name().trim();
        if (vatRateRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Ya existe un tipo de IVA con el nombre '" + name + "'");
        }

        vatRate.setName(name);
        vatRate.setRate(vatRateDTO.rate());
        if (vatRateDTO.active() != null) {
            vatRate.setActive(vatRateDTO.active());
        }

        logger.info("Tipo de IVA {} actualizado", id);
        return VatRateDTO.from(vatRateRepository.save(vatRate));
    }

    private VatRate findVatRate(Long id) {
        return vatRateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de IVA no encontrado con ID: " + id));
    }
}
