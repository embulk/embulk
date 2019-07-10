package org.embulk.exec;

/**
 * Allocates a {@link org.embulk.spi.TempFileSpace}.
 *
 * <p>It has been deprecated. {@link SimpleTempFileSpaceAllocator} is the alternative.
 */
@Deprecated
public class TempFileAllocator extends SimpleTempFileSpaceAllocator {
    public TempFileAllocator() {
        super();
    }
}
