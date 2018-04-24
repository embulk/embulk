package org.embulk.deps;

public class UnacceptableDuplicatedResourceException extends ReflectiveOperationException {
    public UnacceptableDuplicatedResourceException(final String message) {
        super(message);
    }
}
