package org.embulk.cli;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;

public class SelfrunTest {
    private static File testSelfrunFile;

    @BeforeClass
    public static void prepare() throws Exception {
        final File originalSelfrunFile = findSelfrun();
        final FileSystem fileSystem = FileSystems.getDefault();

        final File thisFolder = new File(
                SelfrunTest.class.getResource("/org/embulk/cli/SelfrunTest.class").toURI()).getParentFile();
        testSelfrunFile = new File(
                thisFolder, System.getProperty("file.separator").equals("\\") ? "selfrun.bat" : "selfrun.sh");

        final File classpath = thisFolder.getParentFile().getParentFile().getParentFile();
        final String line =
                new String(Files.readAllBytes(fileSystem.getPath(originalSelfrunFile.getAbsolutePath())),
                           Charset.defaultCharset())
                .replaceAll("java ",
                            "java -classpath "
                                    + classpath.getAbsolutePath().replaceAll("\\\\", "\\\\\\\\")
                                    + " org.embulk.cli.DummyMain ");

        // Modify selfrun so that arguments are written in 'args.txt' .
        Files.write(fileSystem.getPath(testSelfrunFile.getAbsolutePath()),
                    line.getBytes(Charset.defaultCharset()),
                    StandardOpenOption.CREATE);

        if (!testSelfrunFile.setExecutable(true)) {
            throw new Exception("Cannot set the copied selfrun file executable..");
        }
    }

    @Test
    public void testNoArgument() throws Exception {
        List<String> args = execute();
        assertEquals(Arrays.asList(
                         "-XX:+AggressiveOpts",
                         "-XX:+TieredCompilation",
                         "-XX:TieredStopAtLevel=1",
                         "-Xverify:none",
                         "-jar",
                         testSelfrunFile.getAbsolutePath()),
                     args);
    }

    @Test
    public void testArguments() throws Exception {
        List<String> args = execute("a1", "a2", "\"a3=v3\"");
        assertEquals(Arrays.asList(
                         "-XX:+AggressiveOpts",
                         "-XX:+TieredCompilation",
                         "-XX:TieredStopAtLevel=1",
                         "-Xverify:none",
                         "-jar",
                         testSelfrunFile.getAbsolutePath(),
                         "a1",
                         "a2",
                         "a3=v3"),
                     args);
    }

    @Test
    public void testRun() throws Exception {
        List<String> args = execute("run", "a1");
        assertEquals(Arrays.asList(
                         "-XX:+AggressiveOpts",
                         "-XX:+UseConcMarkSweepGC",
                         "-jar",
                         testSelfrunFile.getAbsolutePath(),
                         "run",
                         "a1"),
                     args);
    }

    @Test
    public void testJpO() throws Exception {
        List<String> args = execute("-J+O", "a1", "a2");
        assertEquals(Arrays.asList(
                         "-XX:+AggressiveOpts",
                         "-XX:+UseConcMarkSweepGC",
                         "-jar",
                         testSelfrunFile.getAbsolutePath(),
                         "a1",
                         "a2"),
                     args);
    }

    @Test
    public void testJmO() throws Exception {
        List<String> args = execute("-J-O", "a1", "a2");
        assertEquals(Arrays.asList(
                         "-XX:+AggressiveOpts",
                         "-XX:+TieredCompilation",
                         "-XX:TieredStopAtLevel=1",
                         "-Xverify:none",
                         "-jar",
                         testSelfrunFile.getAbsolutePath(),
                         "a1",
                         "a2"),
                     args);
    }

    @Test
    public void testR1() throws Exception {
        List<String> args = execute("-Rr1", "a1", "a2");
        assertEquals(Arrays.asList(
                         "-XX:+AggressiveOpts",
                         "-XX:+TieredCompilation",
                         "-XX:TieredStopAtLevel=1",
                         "-Xverify:none",
                         "-jar",
                         testSelfrunFile.getAbsolutePath(),
                         "-Rr1",
                         "a1",
                         "a2"),
                     args);
    }

    @Test
    public void testR2() throws Exception {
        List<String> args = execute("\"-Rr1=v1\"", "\"-Rr2=v2\"", "a1", "a2");
        assertEquals(Arrays.asList(
                         "-XX:+AggressiveOpts",
                         "-XX:+TieredCompilation",
                         "-XX:TieredStopAtLevel=1",
                         "-Xverify:none",
                         "-jar",
                         testSelfrunFile.getAbsolutePath(),
                         "-Rr1=v1",
                         "-Rr2=v2",
                         "a1",
                         "a2"),
                     args);
    }

    @Test
    public void testRRun() throws Exception {
        List<String> args = execute("-Rr1", "run", "a1");
        assertEquals(Arrays.asList(
                         "-XX:+AggressiveOpts",
                         "-XX:+UseConcMarkSweepGC",
                         "-jar",
                         testSelfrunFile.getAbsolutePath(),
                         "-Rr1",
                         "run",
                         "a1"),
                     args);
    }

    @Test
    public void testJ1() throws Exception {
        List<String> args = execute("-J-Dj1", "a1", "a2");
        assertEquals(Arrays.asList(
                         "-XX:+AggressiveOpts",
                         "-XX:+TieredCompilation",
                         "-XX:TieredStopAtLevel=1",
                         "-Xverify:none",
                         "-Dj1",
                         "-jar",
                         testSelfrunFile.getAbsolutePath(),
                         "a1",
                         "a2"),
                     args);
    }

    @Test
    public void testJ2() throws Exception {
        List<String> args = execute("\"-J-Dj1=v1\"", "\"-J-Dj2=v2\"", "a1", "a2");
        assertEquals(Arrays.asList(
                         "-XX:+AggressiveOpts",
                         "-XX:+TieredCompilation",
                         "-XX:TieredStopAtLevel=1",
                         "-Xverify:none",
                         "-Dj1=v1",
                         "-Dj2=v2",
                         "-jar",
                         testSelfrunFile.getAbsolutePath(),
                         "a1",
                         "a2"),
                     args);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testJR() throws Exception {
        List<String> args = execute("-Jj1", "-Rr1", "a1", "a2");
        assertEquals(Arrays.asList(
                         "-XX:+AggressiveOpts",
                         "-XX:+TieredCompilation",
                         "-XX:TieredStopAtLevel=1",
                         "-Xverify:none",
                         "j1",
                         "-jar",
                         testSelfrunFile.getAbsolutePath(),
                         "-Rr1",
                         "a1",
                         "a2"),
                     args);
    }

    @Test
    public void testJFile() throws Exception {
        File javaArgsFile = new File(testSelfrunFile.getParentFile(), "java_args.txt");
        FileSystem fileSystem = FileSystems.getDefault();
        Files.write(fileSystem.getPath(javaArgsFile.getAbsolutePath()),
                    "j1 j2 j3".getBytes(Charset.defaultCharset()),
                    StandardOpenOption.CREATE);

        List<String> args = execute("-J", javaArgsFile.getAbsolutePath(), "a1", "a2");
        assertEquals(Arrays.asList(
                         "-XX:+AggressiveOpts",
                         "-XX:+TieredCompilation",
                         "-XX:TieredStopAtLevel=1",
                         "-Xverify:none",
                         "j1",
                         "j2",
                         "j3",
                         "-jar",
                         testSelfrunFile.getAbsolutePath(),
                         "a1",
                         "a2"),
                     args);
    }

    private List<String> execute(String... arguments) throws Exception {
        File temp = new File(testSelfrunFile.getParentFile(), "call-" + testSelfrunFile.getName());
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temp),
                                                                               Charset.defaultCharset()))) {
            writer.write(testSelfrunFile.getAbsolutePath());
            for (String argument : arguments) {
                writer.write(" ");
                writer.write(argument);
            }
        }
        if (!temp.setExecutable(true)) {
            throw new Exception("Cannot set the copied selfrun file executable..");
        }

        File argsFile = new File(testSelfrunFile.getParentFile(), "args.txt");
        if (argsFile.exists()) {
            if (!argsFile.delete()) {
                throw new IOException("Cannot delete " + argsFile);
            }
        }

        Process process = Runtime.getRuntime().exec(temp.getAbsolutePath());
        int exitCode = process.waitFor();
        if (exitCode != 0 || !argsFile.exists()) {
            StringBuilder builder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream(),
                                                                                  Charset.defaultCharset()))) {
                builder.append(reader.readLine());
                builder.append(System.getProperty("line.separator"));
            }
            throw new Exception(builder.toString());
        }

        FileSystem fileSystem = FileSystems.getDefault();
        List<String> args = Files.readAllLines(fileSystem.getPath(argsFile.getAbsolutePath()),
                                               Charset.defaultCharset());
        return args;
    }

    private static File findSelfrun() {
        File folder = new File(".");
        if (new File(folder, "embulk-core").exists()) {
            folder = new File(folder, "embulk-core");
        }
        if (System.getProperty("file.separator").equals("\\")) {
            return new File(new File(new File(new File(folder, "src"), "main"), "bat"), "selfrun.bat");
        } else {
            return new File(new File(new File(new File(folder, "src"), "main"), "sh"), "selfrun.sh");
        }
    }
}
