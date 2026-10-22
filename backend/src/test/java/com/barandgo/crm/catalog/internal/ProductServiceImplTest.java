package com.barandgo.crm.catalog.internal;

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

import com.barandgo.crm.catalog.ProductDTO;
import com.barandgo.crm.catalog.Product;
import com.barandgo.crm.shared.BadRequestException;
import com.barandgo.crm.catalog.ProductAllergenSource;
import com.barandgo.crm.catalog.ProductSalesCheck;
import com.barandgo.crm.masterdata.VatRateDTO;
import com.barandgo.crm.masterdata.VatRateService;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductAllergenSource allergenSource;

    @Mock
    private VatRateService vatRateService;

    @Mock
    private ProductCategoryRepository categoryRepository;

    @Mock
    private ProductSalesCheck productSalesCheck;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void deletingSoldProductArchivesInsteadOfDeleting() {
        Product product = product(5L, true);
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(productSalesCheck.hasSales(5L)).thenReturn(true);

        productService.deleteProduct(5L);

        assertThat(product.getActive()).isFalse();
        assertThat(product.getAvailable()).isFalse();
        verify(productRepository).save(product);
        verify(productRepository, never()).delete(any());
    }

    @Test
    void deletingUnsoldProductRemovesIt() {
        Product product = product(6L, true);
        when(productRepository.findById(6L)).thenReturn(Optional.of(product));
        when(productSalesCheck.hasSales(6L)).thenReturn(false);

        productService.deleteProduct(6L);

        verify(productRepository).delete(product);
    }

    @Test
    void sellableProductMustBeActiveAndAvailable() {
        Product sellable = product(7L, true);
        Product outOfStockToday = product(8L, true);
        outOfStockToday.setAvailable(false);
        when(productRepository.findById(7L)).thenReturn(Optional.of(sellable));
        when(productRepository.findById(8L)).thenReturn(Optional.of(outOfStockToday));
        when(productRepository.findById(9L)).thenReturn(Optional.of(product(9L, false)));

        assertThat(productService.getSellableProduct(7L)).isSameAs(sellable);
        assertThatThrownBy(() -> productService.getSellableProduct(8L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no está disponible");
        assertThatThrownBy(() -> productService.getSellableProduct(9L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void archivedProductCannotBecomeAvailable() {
        when(productRepository.findById(5L)).thenReturn(Optional.of(product(5L, false)));

        assertThatThrownBy(() -> productService.toggleAvailability(5L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("archivado");
    }

    @Test
    void productCannotUseInactiveCategory() {
        ProductCategory inactive = category(9L, "TEMPORADA");
        inactive.setActive(false);
        when(categoryRepository.findById(9L)).thenReturn(Optional.of(inactive));

        ProductDTO request = new ProductDTO();
        request.setName("Gazpacho");
        request.setPrice(new BigDecimal("5.00"));
        request.setCategoryId(9L);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("desactivada");
        verify(productRepository, never()).save(any());
    }

    @Test
    void responseCarriesCategoryIdAndName() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "POSTRES")));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(vatRateService.getVatRates()).thenReturn(List.of(
                new VatRateDTO(1L, "General", new BigDecimal("21.00"), true),
                new VatRateDTO(2L, "Reducido", new BigDecimal("10.00"), true)));

        ProductDTO request = new ProductDTO();
        request.setName("Flan");
        request.setPrice(new BigDecimal("4.00"));
        request.setCategoryId(1L);

        ProductDTO created = productService.createProduct(request);

        assertThat(created.getCategoryId()).isEqualTo(1L);
        assertThat(created.getCategory()).isEqualTo("POSTRES");
        // Sin tipo de IVA indicado se aplica el del 10 %
        assertThat(created.getVatRateId()).isEqualTo(2L);
        assertThat(created.getVatRate()).isEqualByComparingTo("10.00");
    }

    private static Product product(Long id, boolean active) {
        Product product = new Product();
        product.setId(id);
        product.setName("Producto " + id);
        product.setPrice(BigDecimal.TEN);
        product.setCategory(category(1L, "ENTRANTES"));
        product.setAvailable(active);
        product.setActive(active);
        return product;
    }

    private static ProductCategory category(Long id, String name) {
        ProductCategory category = new ProductCategory();
        category.setId(id);
        category.setName(name);
        return category;
    }
}
