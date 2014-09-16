package org.quickload.config;

import java.util.Set;
import javax.validation.Validator;
import javax.validation.ConstraintViolation;

public class ModelValidator
{
    private final Validator validator;

    public ModelValidator(Validator validator)
    {
        this.validator = validator;
    }

    public <T extends DynamicModel<?>> void validateModel(T model) throws ConfigValidationException
    {
        Set<ConstraintViolation<T>> violations = validator.validate(model);
        if (!violations.isEmpty()) {
            throw new ConfigValidationException(violations);
        }
    }
}

