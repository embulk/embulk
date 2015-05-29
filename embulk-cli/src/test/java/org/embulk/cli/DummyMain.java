package org.embulk.cli;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Arrays;

public class DummyMain {

    public static void main(String[] args) throws Exception {
        System.out.println(Arrays.asList(args));
        File thisFolder = new File(SelfrunTest.class.getResource("/org/embulk/cli/DummyMain.class").toURI()).getParentFile();
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(thisFolder, "args.txt")), Charset.defaultCharset()))) {
            for (String arg : args) {
                writer.write(arg);
                writer.newLine();
            }
        }
    }

}
