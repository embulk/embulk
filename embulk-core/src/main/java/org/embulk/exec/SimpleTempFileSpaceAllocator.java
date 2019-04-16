package org.embulk.exec;

import java.io.File;
import org.embulk.spi.TempFileSpace;
import org.embulk.spi.TempFileSpaceAllocator;

class SimpleTempFileSpaceAllocator implements TempFileSpaceAllocator {
    SimpleTempFileSpaceAllocator() {
        // It is originally intended to use `temp_dirs` in system config, but the reasons are missing.
        // https://github.com/embulk/embulk/commit/a7643573ecb39e6dd71a08edce77c8e64dc70a77
        final String systemPropertyTmpDir = System.getProperty("java.io.tmpdir", DEFAULT_TEMP_DIR);
        this.dir = new File(systemPropertyTmpDir.isEmpty() ? DEFAULT_TEMP_DIR : systemPropertyTmpDir, "embulk");
    }

    @Override
    public TempFileSpace newSpace(final String subdirectoryName) {
        // It is originally intended to support multiple files/directories, but the reasons are missing.
        // https://github.com/embulk/embulk/commit/a7643573ecb39e6dd71a08edce77c8e64dc70a77
        // https://github.com/embulk/embulk/commit/5a78270a4fc20e3c113c68e4c0f6c66c1bd45886

        // UNIX/Linux cannot include '/' as file name.
        // Windows cannot include ':' as file name.
        return new TempFileSpace(new File(this.dir, subdirectoryName.replace('/', '-').replace(':', '-')));
    }

    private static final String DEFAULT_TEMP_DIR = "/tmp";

    private final File dir;
}
