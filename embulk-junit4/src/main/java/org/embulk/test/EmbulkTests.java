package org.embulk.test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import org.embulk.config.ConfigLoader;
import org.embulk.config.ConfigSource;
import org.embulk.config.ModelManager;

public class EmbulkTests {
    private EmbulkTests() {}

    public static ConfigSource config(String envName) {
        String path = System.getenv(envName);
        assumeThat(path == null || path.isEmpty(), is(false));
        try {
            return new ConfigLoader(new ModelManager()).fromYamlFile(new File(path));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String readResource(String name) {
        try (final InputStream in = EmbulkTests.class.getResourceAsStream(name)) {
            return inputStreamToString(in);
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void copyResource(String resource, Path dest) throws IOException {
        Files.createDirectories(dest.getParent());
        try (final InputStream input = EmbulkTests.class.getResourceAsStream(resource)) {
            Files.copy(input, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static String readFile(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            return inputStreamToString(in);
        }
    }

    public static String readSortedFile(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        Collections.sort(lines);
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line);
            sb.append("\n");
        }
        return sb.toString();
    }

    private static String inputStreamToString(final InputStream stream) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final byte[] buffer = new byte[1024];
        for (int length; (length = stream.read(buffer)) != -1;) {
            output.write(buffer, 0, length);
        }
        return output.toString(StandardCharsets.UTF_8.toString());
    }
}
