package com.barandgo.crm.masterdata;

import java.util.List;

import com.barandgo.crm.shared.ResourceNotFoundException;

/**
 * Tipos de IVA. Sin borrado: un tipo en desuso se desactiva.
 */
public interface VatRateService {

    List<VatRateDTO> getVatRates();

    /**
     * @throws ResourceNotFoundException si no existe
     */
    VatRateDTO getVatRate(Long id);

    VatRateDTO createVatRate(VatRateDTO vatRateDTO);

    VatRateDTO updateVatRate(Long id, VatRateDTO vatRateDTO);
}
