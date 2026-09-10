package com.example.wms.config;

import com.example.wms.repository.ProductRepository;
import com.example.wms.repository.SlotReader;
import com.example.wms.service.validation.ProductExistsHandler;
import com.example.wms.service.validation.QuantityPositiveHandler;
import com.example.wms.service.validation.ReceiveValidationHandler;
import com.example.wms.service.validation.WarehouseCapacityHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {

    /**
     * Assembles the Chain of Responsibility once at startup:
     * ProductExists -> QuantityPositive -> WarehouseCapacity.
     *
     * The three handler classes are plain objects (not @Component beans) —
     * they're constructed here, given their real dependencies as normal
     * constructor arguments, and chained together. This produces exactly
     * ONE Spring bean of type ReceiveValidationHandler. (An earlier version
     * of this file also had @Component on each handler class individually,
     * which created FOUR beans of that type and made every class depending
     * on ReceiveValidationHandler ambiguous to wire — that's the "expected
     * single matching bean but found 4" startup error.)
     */
    @Bean
    public ReceiveValidationHandler receiveValidationChain(ProductRepository productRepository,
                                                             SlotReader slotReader) {
        ReceiveValidationHandler productExists = new ProductExistsHandler(productRepository);
        ReceiveValidationHandler quantityPositive = new QuantityPositiveHandler();
        ReceiveValidationHandler warehouseCapacity = new WarehouseCapacityHandler(slotReader);

        productExists.setNext(quantityPositive).setNext(warehouseCapacity);
        return productExists;
    }
}
