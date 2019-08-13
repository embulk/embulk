package org.embulk.spi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TempFileSpaceTest {
    private static final String TMP_SUBDIR = "embulk_test";

    private static File dir = new File(System.getProperty("java.io.tmpdir") + TMP_SUBDIR);
    private static TempFileSpace space;

    @BeforeClass
    public static void setUp() {
        space = new TempFileSpace(dir);
    }

    @AfterClass
    public static void tearDown() {
        assertTrue(dir.exists());
        space.cleanup();
        assertFalse(dir.exists());
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullDirConstructor() {
        TempFileSpace tmpSpace = new TempFileSpace(null);
    }

    @Test
    public void testCreateTempFile() {
        File tmpFile = space.createTempFile();
        System.out.println(tmpFile.getAbsolutePath());
        assertTrue(tmpFile.exists());
        assertTrue(tmpFile.getName().endsWith(".tmp"));
    }

    @Test
    public void testCreateTempFileWithExt() {
        File tmpFile = space.createTempFile("myext");
        System.out.println(tmpFile.getName());
        assertTrue(tmpFile.exists());
        assertTrue(tmpFile.getName().endsWith(".myext"));
        assertFalse(tmpFile.getName().indexOf(':') != -1);
    }

    @Test
    public void testCreateTempFileWithPrefixAndExt() {
        File tmpFile = space.createTempFile("prefix", "myext");
        System.out.println(tmpFile.getName());
        assertTrue(tmpFile.exists());
        assertTrue(tmpFile.getName().startsWith("prefix"));

        space.cleanup();
        tmpFile = space.createTempFile("prefix", "myext");
        System.out.println(tmpFile.getName());
        assertTrue(tmpFile.exists());
        assertTrue(tmpFile.getName().startsWith("prefix"));
    }
}
