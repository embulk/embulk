package org.embulk.exec;

import org.embulk.spi.TempFileSpaceAllocator;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestSimpleTempFileSpaceAllocator {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setJavaTmpDir() {
        this.systemPropertyTmpDir = System.getProperty("java.io.tmpdir");
        System.setProperty("java.io.tmpdir", temporaryFolder.getRoot().getAbsolutePath());
    }

    @After
    public void resetJavaTmpDir() {
        if (this.systemPropertyTmpDir == null) {
            System.clearProperty("java.io.tmpdir");
        } else {
            System.setProperty("java.io.tmpdir", this.systemPropertyTmpDir);
        }
    }

    @Test
    public void testNewSpaceWithIso8601Basic() {
        final TempFileSpaceAllocator allocator = new SimpleTempFileSpaceAllocator();
        allocator.newSpace("20191031T123456Z");
    }

    @Test
    public void testNewSpaceWithNonIso8601Basic() {
        // TODO: Make it fail from the v0.10 series.
        final TempFileSpaceAllocator allocator = new SimpleTempFileSpaceAllocator();
        allocator.newSpace("2019-10-31 12:34:56 UTC");
    }

    private String systemPropertyTmpDir;
}
