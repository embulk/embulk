package org.embulk.config;

import java.util.Set;
import javax.validation.ConstraintViolation;

public class TaskValidationException extends ConfigException {
    @SuppressWarnings("unchecked")
    private final Set violations;

    public <T> TaskValidationException(Set<ConstraintViolation<T>> violations) {
        super(formatMessage(violations));
        this.violations = violations;
    }

    @SuppressWarnings("unchecked")
    public Set<ConstraintViolation<?>> getViolations() {
        return violations;
    }

    private static <T> String formatMessage(Set<ConstraintViolation<T>> violations) {
        StringBuilder sb = new StringBuilder();
        sb.append("Configuration task validation failed.");
        for (ConstraintViolation<T> violation : violations) {
            sb.append(" ");
            sb.append(violation.getPropertyPath());
            sb.append(" ");
            sb.append(violation.getMessage());
            sb.append(" but got ");
            sb.append(violation.getInvalidValue());
        }
        return sb.toString();
    }
}
