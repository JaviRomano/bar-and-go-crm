package com.barandgo.crm.service.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barandgo.crm.dto.supplier.SupplierDTO;
import com.barandgo.crm.entity.Supplier;
import com.barandgo.crm.exception.ConflictException;
import com.barandgo.crm.exception.ResourceNotFoundException;
import com.barandgo.crm.repository.SupplierRepository;
import com.barandgo.crm.service.SupplierService;
import com.barandgo.crm.validator.TaxIdRules;

/**
 * Proveedores. No se borran: un proveedor con histórico se desactiva (active = false).
 */
@Service
@Transactional
public class SupplierServiceImpl implements SupplierService {

    private static final Logger logger = LoggerFactory.getLogger(SupplierServiceImpl.class);

    private final SupplierRepository supplierRepository;

    public SupplierServiceImpl(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierDTO> getSuppliers(Boolean active) {
        List<Supplier> suppliers = active == null
                ? supplierRepository.findAllByOrderByLegalNameAsc()
                : supplierRepository.findByActiveOrderByLegalNameAsc(active);
        return suppliers.stream().map(SupplierDTO::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierDTO getSupplierById(Long id) {
        return SupplierDTO.from(findSupplier(id));
    }

    @Override
    public SupplierDTO createSupplier(SupplierDTO supplierDTO) {
        String taxId = TaxIdRules.normalize(supplierDTO.taxId());
        if (supplierRepository.existsByTaxId(taxId)) {
            throw new ConflictException("Ya existe un proveedor con el CIF/NIF " + taxId);
        }

        Supplier supplier = new Supplier();
        supplier.setTaxId(taxId);
        apply(supplier, supplierDTO);
        supplier.setActive(supplierDTO.active() != null ? supplierDTO.active() : true);

        Supplier saved = supplierRepository.save(supplier);
        logger.info("Proveedor {} creado", saved.getId());
        return SupplierDTO.from(saved);
    }

    @Override
    public SupplierDTO updateSupplier(Long id, SupplierDTO supplierDTO) {
        Supplier supplier = findSupplier(id);
        String taxId = TaxIdRules.normalize(supplierDTO.taxId());
        if (supplierRepository.existsByTaxIdAndIdNot(taxId, id)) {
            throw new ConflictException("Ya existe otro proveedor con el CIF/NIF " + taxId);
        }

        supplier.setTaxId(taxId);
        apply(supplier, supplierDTO);
        if (supplierDTO.active() != null) {
            supplier.setActive(supplierDTO.active());
        }

        logger.info("Proveedor {} actualizado", id);
        return SupplierDTO.from(supplierRepository.save(supplier));
    }

    // PUT sustituye los datos descriptivos: un campo opcional ausente queda vacío
    private static void apply(Supplier supplier, SupplierDTO dto) {
        supplier.setLegalName(dto.legalName().trim());
        supplier.setTradeName(dto.tradeName());
        supplier.setContactName(dto.contactName());
        supplier.setEmail(dto.email());
        supplier.setPhone(dto.phone());
        supplier.setPaymentTerms(dto.paymentTerms());
        supplier.setLeadTimeDays(dto.leadTimeDays());
        supplier.setNotes(dto.notes());
    }

    private Supplier findSupplier(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con ID: " + id));
    }
}
