package org.embulk.deps;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Expands a classpath string to a list of {@link java.nio.file.Path} in the same manner with Java Runtime's "--classpath".
 *
 * @see <a href="https://docs.oracle.com/javase/8/docs/technotes/tools/windows/classpath.html#A1100762">Class Path Wild Cards</a>
 */
final class ClasspathExpander {
    private ClasspathExpander() {
        // No instantiation.
    }

    /**
     * Expands a classpath string with wildcards into an array of URLs.
     *
     * @param classpath  a classpath string
     * @return a list of {@link java.nio.file.Path}
     */
    static List<Path> expand(final String classpath) throws IOException {
        if (classpath == null || classpath.indexOf('*') < 0) {
            return Collections.unmodifiableList(new ArrayList<Path>());
        }

        final List<String> filenameList = splitClasspath(classpath);
        final List<Path> expanded = Collections.unmodifiableList(expandWildcards(filenameList));
        logger.debug("Expanded wildcards:");
        logger.debug("    before: \"{}\"", classpath);
        logger.debug("    after : \"{}\"", expanded.toString());

        return expanded;
    }

    private static ArrayList<Path> expandWildcards(final List<String> filenameList) throws IOException {
        // https://hg.openjdk.java.net/jdk8u/jdk8u-dev/jdk/file/jdk8u242-b01/src/share/bin/wildcard.c#l386
        final ArrayList<Path> pathList = new ArrayList<>();
        for (final String file : filenameList) {
            if (isWildcard(file)) {
                final List<Path> expanded = wildcardFileList(file);
                if (expanded != null) {
                    pathList.addAll(expanded);
                }
            } else {
                pathList.add(Paths.get(file).toAbsolutePath());
            }
        }
        return pathList;
    }

    /**
     * Expands a wildcard'ed filename String into a List of Paths.
     *
     * <p>It expects {@code wildcard} finishes with {@code "*"}.
     *
     * @param wildcard  a wildcard'ed filename
     * @return a List of Paths
     */
    private static List<Path> wildcardFileList(final String wildcard) throws IOException {
        // https://hg.openjdk.java.net/jdk8u/jdk8u-dev/jdk/file/jdk8u242-b01/src/share/bin/wildcard.c#l356
        final int length = wildcard.length();
        final Path dir;
        if (length < 2) {
            dir = Paths.get(".");
        } else {
            dir = Paths.get(wildcard.substring(0, length - 1));
        }

        final ArrayList<Path> pathList = new ArrayList<>();

        try (final DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)) {
            for (final Path entry : dirStream) {
                final String basename = entry.getFileName().toString();
                if (Files.isRegularFile(entry) && isJarFileName(basename)) {
                    pathList.add(entry.toAbsolutePath());
                }
            }
        } catch (final DirectoryIteratorException ex) {
            throw ex.getCause();  // IOException
        }
        return Collections.unmodifiableList(pathList);
    }

    private static List<String> splitClasspath(final String classpath) {
        // Paranoia: Following the implementation of FileList_split in wildcard.c.
        // https://hg.openjdk.java.net/jdk8u/jdk8u-dev/jdk/file/jdk8u242-b01/src/share/bin/wildcard.c#l310
        final int length = classpath.length();
        final ArrayList<String> filenameList = new ArrayList<>();
        for (int p = 0; ; ) {
            for (int q = p; q < length; ++q) {
                if (q == length - 1) {
                    filenameList.add(classpath.substring(p));
                    return Collections.unmodifiableList(filenameList);
                } else if (classpath.charAt(q) == File.pathSeparatorChar) {
                    filenameList.add(classpath.substring(p, q - p));
                    p = q + 1;
                }
            }
        }
    }

    private static boolean isWildcard(final String filename) {
        // https://hg.openjdk.java.net/jdk8u/jdk8u-dev/jdk/file/jdk8u242-b01/src/share/bin/wildcard.c#l376
        final int length = filename.length();
        return (length > 0)
                && (filename.charAt(length - 1) == '*')
                && (length == 1 || filename.charAt(length - 2) == File.separatorChar)
                && (!(new File(filename)).exists());
    }

    private static boolean isJarFileName(final String filename) {
        // https://hg.openjdk.java.net/jdk8u/jdk8u-dev/jdk/file/jdk8u242-b01/src/share/bin/wildcard.c#l332
        final int length = filename.length();
        return (length >= 4)
                && (filename.charAt(length - 4) == '.')
                && (filename.substring(length - 3).equals("jar")
                    || filename.substring(length - 3).equals("JAR"))
                /* Paranoia: Maybe filename is "DIR:foo.jar" */
                && (filename.indexOf(File.pathSeparatorChar) < 0);
    }

    private static final Logger logger = LoggerFactory.getLogger(ClasspathExpander.class);
}
