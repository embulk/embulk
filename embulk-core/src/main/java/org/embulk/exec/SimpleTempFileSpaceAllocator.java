package org.embulk.exec;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.embulk.spi.TempFileSpace;
import org.embulk.spi.TempFileSpaceAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SimpleTempFileSpaceAllocator implements TempFileSpaceAllocator {
    SimpleTempFileSpaceAllocator() {
        // It is originally intended to use `temp_dirs` in system config, but the reasons are missing.
        // https://github.com/embulk/embulk/commit/a7643573ecb39e6dd71a08edce77c8e64dc70a77

        final String systemPropertyTmpDir = System.getProperty("java.io.tmpdir");
        if (systemPropertyTmpDir == null || systemPropertyTmpDir.isEmpty()) {
            // TODO: Error when java.io.tmpdir is not set?
            logger.warn("Property java.io.tmpdir should be set properly.");
            this.tempDirectoryBase = Paths.get(DEFAULT_TEMP_DIR);
        } else {
            this.tempDirectoryBase = Paths.get(systemPropertyTmpDir);
        }
    }

    @Override
    public TempFileSpace newSpace(final String subdirectoryPrefix) {
        // It is originally intended to support multiple files/directories, but the reasons are missing.
        // https://github.com/embulk/embulk/commit/a7643573ecb39e6dd71a08edce77c8e64dc70a77
        // https://github.com/embulk/embulk/commit/5a78270a4fc20e3c113c68e4c0f6c66c1bd45886

        // UNIX/Linux cannot include '/' as file name.
        // Windows cannot include ':' as file name.
        try {
            return TempFileSpace.with(
                    this.tempDirectoryBase, "embulk" + subdirectoryPrefix.replace('/', '-').replace(':', '-'));
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static final String DEFAULT_TEMP_DIR = "/tmp";

    private static final Logger logger = LoggerFactory.getLogger(SimpleTempFileSpaceAllocator.class);

    private final Path tempDirectoryBase;
}
