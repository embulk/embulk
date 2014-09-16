package org.quickload.config;

import java.util.Set;
import javax.validation.Validator;
import javax.validation.ConstraintViolation;

public class ConfigValidationException
        extends ConfigException
{
    public <T extends DynamicModel<?>> ConfigValidationException(Set<ConstraintViolation<T>> violations)
    {
        super(formatMessage(violations));
    }

    private static <T extends DynamicModel<?>> String formatMessage(Set<ConstraintViolation<T>> violations)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Configuration validation failed.");
        for(ConstraintViolation<T> violation : violations) {
            sb.append(" ");
            sb.append(violation.getMessage());
        }
        return sb.toString();
    }
}
