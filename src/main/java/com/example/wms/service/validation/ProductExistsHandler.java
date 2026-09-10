package com.example.wms.service.validation;

import com.example.wms.dto.ReceiveCommand;
import com.example.wms.exception.ValidationException;
import com.example.wms.repository.ProductRepository;

// Not a @Component: this class is only ever meant to be assembled into the
// single validation chain built by BeanConfig. Registering it as its own
// bean too (as it was before) creates a second bean of type
// ReceiveValidationHandler and makes injecting the chain ambiguous.
public class ProductExistsHandler extends ReceiveValidationHandler {

    private final ProductRepository productRepository;

    public ProductExistsHandler(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    protected void doValidate(ReceiveCommand command) {
        if (command.productSku() == null || command.productSku().isBlank()) {
            throw new ValidationException("Product SKU is required");
        }
        productRepository.findBySku(command.productSku())
                .orElseThrow(() -> new ValidationException("Unknown product SKU: " + command.productSku()));
    }
}
