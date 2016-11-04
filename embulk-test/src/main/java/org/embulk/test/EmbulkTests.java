package org.embulk.test;

import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;
import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.embulk.EmbulkEmbed;
import org.embulk.config.ConfigSource;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

public class EmbulkTests
{
    private EmbulkTests()
    { }

    public static ConfigSource config(String envName)
    {
        String path = System.getenv(envName);
        assumeThat(isNullOrEmpty(path), is(false));
        try {
            return EmbulkEmbed.newSystemConfigLoader().fromYamlFile(new File(path));
        } catch (IOException ex) {
            throw Throwables.propagate(ex);
        }
    }

    public static String readResource(String name)
    {
        try (InputStream in = Resources.getResource(name).openStream()) {
            return CharStreams.toString(new InputStreamReader(in, UTF_8));
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void copyResource(String resource, Path dest)
        throws IOException
    {
        Files.createDirectories(dest.getParent());
        try (InputStream input = Resources.getResource(resource).openStream()) {
            Files.copy(input, dest, REPLACE_EXISTING);
        }
    }

    public static String readFile(Path path) throws IOException
    {
        try (InputStream in = Files.newInputStream(path)) {
            return CharStreams.toString(new InputStreamReader(in, UTF_8));
        }
    }

    public static String readSortedFile(Path path) throws IOException
    {
        List<String> lines = Files.readAllLines(path, UTF_8);
        Collections.sort(lines);
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line);
            sb.append("\n");
        }
        return sb.toString();
    }
}
