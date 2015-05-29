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

public class SelfrunTest {

    private static File testSelfrun;

    @BeforeClass
    public static void prepare() throws Exception {
        File selfrun = findSelfrun();
        FileSystem fs = FileSystems.getDefault();
        String line = new String(Files.readAllBytes(fs.getPath(selfrun.getAbsolutePath())), Charset.defaultCharset());

        File thisFolder = new File(SelfrunTest.class.getResource("/org/embulk/cli/SelfrunTest.class").toURI()).getParentFile();
        testSelfrun = new File(thisFolder, System.getProperty("file.separator").equals("\\") ? "selfrun.bat" : "selfrun.sh");

        File classpath = thisFolder.getParentFile().getParentFile().getParentFile();
        line = line.replaceAll("java ", "java -classpath " + classpath.getAbsolutePath().replaceAll("\\\\", "\\\\\\\\") + " org.embulk.cli.DummyMain ");

        // Modify selfrun so that arguments are written in 'args.txt' .
        Files.write(fs.getPath(testSelfrun.getAbsolutePath()), line.getBytes(Charset.defaultCharset()), StandardOpenOption.CREATE);
    }


    @Test
    public void test() throws Exception {
        System.out.println(execute());
    }


    private List<String> execute(String... arguments) throws Exception {
        List<String> commands = new ArrayList<String>();
        commands.add(testSelfrun.getAbsolutePath());
        commands.addAll(Arrays.asList(arguments));
        Process process = Runtime.getRuntime().exec(commands.toArray(new String[commands.size()]));
        process.waitFor();

        FileSystem fs = FileSystems.getDefault();
        File args = new File(testSelfrun.getParentFile(), "args.txt");
        return Files.readAllLines(fs.getPath(args.getAbsolutePath()), Charset.defaultCharset());
    }

    private static File findSelfrun() {
        File folder = new File(".");
        if (new File(folder, "embulk-cli").exists()) {
            folder = new File(folder, "embulk-cli");
        }
        return new File(new File(new File(new File(folder, "src"), "main"), "sh"), "selfrun.sh");
    }

}
