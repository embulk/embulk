package org.embulk.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestTempFileSpace {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test(expected = IllegalArgumentException.class)
    public void testNullBaseDir() throws IOException {
        TempFileSpace.with(null, "embulk20191030T000000Z");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullPrefix() throws IOException {
        TempFileSpace.with(temporaryFolder.getRoot().toPath(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRelativePath() throws IOException {
        final Path path = temporaryFolder.getRoot().toPath();
        TempFileSpace.with(path.subpath(path.getNameCount() - 1, path.getNameCount()), "embulk20191030T000001Z");
    }

    @Test(expected = IOException.class)
    public void testNonexistent() throws IOException {
        TempFileSpace.with(temporaryFolder.getRoot().toPath().resolve("somenonexistentdir"), "embulk20191030T000002Z");
    }

    @Test
    public void testCreateTempFile() {
        final String prefix = "embulk20191030T000003Z";

        final TempFileSpace space = create(prefix);
        // The temporary directory should not have been created before the first createTempFile().
        assertFalse(space.getTempDirectoryForTesting().isPresent());
        assertEquals(0, entriesPrefixedWith(prefix));

        final File tempFile = space.createTempFile();
        System.out.println(tempFile.getAbsolutePath());
        assertTrue(space.getTempDirectoryForTesting().isPresent());
        assertEquals(1, entriesPrefixedWith(prefix));
        assertTrue(tempFile.exists());
        assertTrue(tempFile.getName().endsWith(".tmp"));

        space.cleanup();
        assertFalse(space.getTempDirectoryForTesting().isPresent());
        assertEquals(0, entriesPrefixedWith(prefix));
        assertFalse(tempFile.exists());
    }

    @Test
    public void testCreateTempFileWithExt() {
        final String prefix = "embulk20191030T000004Z";

        final TempFileSpace space = create(prefix);
        // The temporary directory should not have been created before the first createTempFile().
        assertFalse(space.getTempDirectoryForTesting().isPresent());
        assertEquals(0, entriesPrefixedWith(prefix));

        final File tempFile = space.createTempFile("myext");
        System.out.println(tempFile.getAbsolutePath());
        assertTrue(space.getTempDirectoryForTesting().isPresent());
        assertEquals(1, entriesPrefixedWith(prefix));
        assertTrue(tempFile.exists());
        assertTrue(tempFile.getName().endsWith(".myext"));

        space.cleanup();
        assertFalse(space.getTempDirectoryForTesting().isPresent());
        assertEquals(0, entriesPrefixedWith(prefix));
        assertFalse(tempFile.exists());
    }

    @Test
    public void testCreateTempFileWithPrefixAndExt() {
        final String prefix = "embulk20191030T000005Z";

        final TempFileSpace space = create(prefix);
        // The temporary directory should not have been created before the first createTempFile().
        assertFalse(space.getTempDirectoryForTesting().isPresent());
        assertEquals(0, entriesPrefixedWith(prefix));

        final File tempFile = space.createTempFile("filenameprefix", "myext");
        System.out.println(tempFile.getAbsolutePath());
        assertTrue(space.getTempDirectoryForTesting().isPresent());
        assertEquals(1, entriesPrefixedWith(prefix));
        assertTrue(tempFile.exists());
        assertTrue(tempFile.getName().startsWith("filenameprefix"));
        assertTrue(tempFile.getName().endsWith(".myext"));

        space.cleanup();
        assertFalse(space.getTempDirectoryForTesting().isPresent());
        assertEquals(0, entriesPrefixedWith(prefix));
        assertFalse(tempFile.exists());
    }

    private long entriesPrefixedWith(final String prefix) {
        try (final Stream<Path> stream = Files.list(temporaryFolder.getRoot().toPath())) {
            return stream.filter(entry -> entry.getFileName().toString().startsWith(prefix)).count();
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private TempFileSpace create(final String prefix) {
        try {
            return TempFileSpace.with(temporaryFolder.getRoot().toPath(), prefix);
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
