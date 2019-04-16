package org.embulk.spi;

/**
 * Allocates a new {@code TempFileSpace}.
 *
 * <p>It is a part of {@code org.embulk.spi} so that {@link org.embulk.spi.ExecSession} can refer it,
 * but it should not be used directly. Use {@link org.embulk.spi.Exec.getTempFileSpace} instead.
 */
public interface TempFileSpaceAllocator {
    TempFileSpace newSpace(final String subdirectoryName);
}
