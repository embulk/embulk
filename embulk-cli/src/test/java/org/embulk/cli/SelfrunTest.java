package org.embulk.cli;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;


// 原因不明だが、jarに組み込むと動かない
// 前のラベルに戻るgotoができない
// バイナリ部分が何か影響している模様
public class SelfrunTest {

    private static File testSelfrun;

    @BeforeClass
    public static void prepare() throws Exception {
        File selfrun = findSelfrun();
        FileSystem fs = FileSystems.getDefault();
        String line = new String(Files.readAllBytes(fs.getPath(selfrun.getAbsolutePath())), Charset.defaultCharset());

        File thisFolder = new File(SelfrunTest.class.getResource("/org/embulk/cli/SelfrunTest.class").toURI()).getParentFile();
        testSelfrun = new File(thisFolder, System.getProperty("file.separator").equals("\\") ? "selfrun.bat" : "selfrun.sh");
        testSelfrun.setExecutable(true);

        File classpath = thisFolder.getParentFile().getParentFile().getParentFile();
        line = line.replaceAll("java ", "java -classpath " + classpath.getAbsolutePath().replaceAll("\\\\", "\\\\\\\\") + " org.embulk.cli.DummyMain ");

        // Modify selfrun so that arguments are written in 'args.txt' .
        Files.write(fs.getPath(testSelfrun.getAbsolutePath()), line.getBytes(Charset.defaultCharset()), StandardOpenOption.CREATE);
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
                testSelfrun.getAbsolutePath()),
                args);
    }

    @Test
    public void testArguments() throws Exception {
        List<String> args = execute("a1", "a2");
        assertEquals(Arrays.asList(
                "-XX:+AggressiveOpts",
                "-XX:+TieredCompilation",
                "-XX:TieredStopAtLevel=1",
                "-Xverify:none",
                "-jar",
                testSelfrun.getAbsolutePath(),
                "a1",
                "a2"),
                args);
    }

    @Test
    public void testRun() throws Exception {
        List<String> args = execute("run", "a1");
        assertEquals(Arrays.asList(
                "-XX:+AggressiveOpts",
                "-XX:+UseConcMarkSweepGC",
                "-jar",
                testSelfrun.getAbsolutePath(),
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
                testSelfrun.getAbsolutePath(),
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
                testSelfrun.getAbsolutePath(),
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
                testSelfrun.getAbsolutePath(),
                "r1",
                "a1",
                "a2"),
                args);
    }

    @Test
    public void testR2() throws Exception {
        List<String> args = execute("-Rr1", "-Rr2", "a1", "a2");
        assertEquals(Arrays.asList(
                "-XX:+AggressiveOpts",
                "-XX:+TieredCompilation",
                "-XX:TieredStopAtLevel=1",
                "-Xverify:none",
                "-jar",
                testSelfrun.getAbsolutePath(),
                "r1",
                "r2",
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
                testSelfrun.getAbsolutePath(),
                "r1",
                "run",
                "a1"),
                args);
    }

    @Test
    public void testJ1() throws Exception {
        List<String> args = execute("-Jj1", "a1", "a2");
        assertEquals(Arrays.asList(
                "-XX:+AggressiveOpts",
                "-XX:+TieredCompilation",
                "-XX:TieredStopAtLevel=1",
                "-Xverify:none",
                "j1",
                "-jar",
                testSelfrun.getAbsolutePath(),
                "a1",
                "a2"),
                args);
    }

    @Test
    public void testJ2() throws Exception {
        List<String> args = execute("-Jj1", "-Jj2", "a1", "a2");
        assertEquals(Arrays.asList(
                "-XX:+AggressiveOpts",
                "-XX:+TieredCompilation",
                "-XX:TieredStopAtLevel=1",
                "-Xverify:none",
                "j1",
                "j2",
                "-jar",
                testSelfrun.getAbsolutePath(),
                "a1",
                "a2"),
                args);
    }

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
                testSelfrun.getAbsolutePath(),
                "r1",
                "a1",
                "a2"),
                args);
    }

    @Test
    public void testJFile() throws Exception {
        File javaArgsFile = new File(testSelfrun.getParentFile(), "java_args.txt");
        FileSystem fs = FileSystems.getDefault();
        Files.write(fs.getPath(javaArgsFile.getAbsolutePath()), "j1 j2 j3".getBytes(), StandardOpenOption.CREATE);

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
                testSelfrun.getAbsolutePath(),
                "a1",
                "a2"),
                args);
    }

    private List<String> execute(String... arguments) throws Exception {
        List<String> commands = new ArrayList<String>();
        commands.add(testSelfrun.getAbsolutePath());
        commands.addAll(Arrays.asList(arguments));
        Process process = Runtime.getRuntime().exec(commands.toArray(new String[commands.size()]));
        process.waitFor();

        FileSystem fs = FileSystems.getDefault();
        File argsFile = new File(testSelfrun.getParentFile(), "args.txt");
        List<String> args = Files.readAllLines(fs.getPath(argsFile.getAbsolutePath()), Charset.defaultCharset());
        return args;
    }

    private static File findSelfrun() {
        File folder = new File(".");
        if (new File(folder, "embulk-cli").exists()) {
            folder = new File(folder, "embulk-cli");
        }
        return new File(new File(new File(new File(folder, "src"), "main"), "sh"), "selfrun.sh");
    }

}
