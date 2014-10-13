package org.quickload.model;

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

    public <T> void validateModel(T model) throws ModelValidationException
    {
        Set<ConstraintViolation<T>> violations = validator.validate(model);
        if (!violations.isEmpty()) {
            throw new ModelValidationException(violations);
        }
    }
}

