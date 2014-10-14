package org.quickload.config;

import java.util.Set;
import javax.validation.ConstraintViolation;

public class TaskValidationException
        extends RuntimeException
{
    @SuppressWarnings("unchecked")
    private final Set violations;

    public <T> TaskValidationException(Set<ConstraintViolation<T>> violations)
    {
        super(formatMessage(violations));
        this.violations = violations;
    }

    public Set<ConstraintViolation<?>> getViolations()
    {
        return violations;
    }

    private static <T> String formatMessage(Set<ConstraintViolation<T>> violations)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Configuration task validation failed.");
        for(ConstraintViolation<T> violation : violations) {
            sb.append(" ");
            sb.append(violation.getMessage());
        }
        return sb.toString();
    }
}
