package com.example.wms.service.validation;

import com.example.wms.dto.ReceiveCommand;
import com.example.wms.exception.ValidationException;

// Not a @Component — see the note in ProductExistsHandler.java.
public class QuantityPositiveHandler extends ReceiveValidationHandler {

    @Override
    protected void doValidate(ReceiveCommand command) {
        if (command.quantity() <= 0) {
            throw new ValidationException("Quantity must be greater than 0");
        }
    }
}
