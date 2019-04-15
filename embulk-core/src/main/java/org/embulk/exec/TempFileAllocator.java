package org.embulk.exec;

import com.google.inject.Inject;
import org.embulk.config.ConfigSource;

/**
 * Allocates a {@link org.embulk.spi.TempFileSpace}.
 *
 * <p>It has been deprecated. {@link SimpleTempFileSpaceAllocator} is the alternative.
 */
@Deprecated
public class TempFileAllocator extends SimpleTempFileSpaceAllocator {
    @Inject
    public TempFileAllocator(@ForSystemConfig ConfigSource systemConfig) {
        super();
    }
}
