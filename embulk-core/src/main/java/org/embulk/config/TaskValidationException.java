package org.embulk.config;

import org.embulk.deps.config.ConstraintViolations;

public class TaskValidationException extends ConfigException {
    public <T> TaskValidationException(final ConstraintViolations violations) {
        super(violations.formatMessage());
        this.violations = violations;
    }

    private final ConstraintViolations violations;
}
