package org.embulk.deps.config;

import java.util.Set;
import javax.validation.ConstraintViolation;
import org.embulk.config.ConstraintViolations;

public class ConstraintViolationsImpl<T> extends ConstraintViolations {
    @SuppressWarnings("unchecked")
    private final Set<ConstraintViolation<T>> violations;

    public ConstraintViolationsImpl(final Set<ConstraintViolation<T>> violations) {
        this.violations = violations;
    }

    @Override
    public String formatMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("Configuration task validation failed.");
        for (ConstraintViolation<T> violation : this.violations) {
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
