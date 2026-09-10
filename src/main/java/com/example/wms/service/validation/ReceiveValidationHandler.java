package com.example.wms.service.validation;

import com.example.wms.dto.ReceiveCommand;

/**
 * CHAIN OF RESPONSIBILITY
 * ------------------------
 * Each concrete handler does exactly one independent check and then passes
 * the command along. Adding a new rule later (e.g. a weight limit) means
 * adding a new handler class and wiring it into the chain in BeanConfig —
 * never editing an existing handler.
 */
public abstract class ReceiveValidationHandler {

    private ReceiveValidationHandler next;

    public ReceiveValidationHandler setNext(ReceiveValidationHandler next) {
        this.next = next;
        return next;
    }

    public void validate(ReceiveCommand command) {
        doValidate(command);
        if (next != null) {
            next.validate(command);
        }
    }

    protected abstract void doValidate(ReceiveCommand command);
}
