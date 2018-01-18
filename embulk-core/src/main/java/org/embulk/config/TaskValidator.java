package org.embulk.config;

import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

public class TaskValidator {
    private final Validator validator;

    public TaskValidator(Validator validator) {
        this.validator = validator;
    }

    public <T> void validateModel(T model) throws TaskValidationException {
        Set<ConstraintViolation<T>> violations = validator.validate(model);
        if (!violations.isEmpty()) {
            throw new TaskValidationException(violations);
        }
    }
}
