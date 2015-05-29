package org.embulk.cli;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;

public class DummyMain {

    public static void main(String[] args) throws Exception {
        System.out.println(Arrays.asList(args));
        File thisFolder = new File(SelfrunTest.class.getResource("/org/embulk/cli/DummyMain.class").toURI()).getParentFile();
        try (FileWriter writer = new FileWriter(new File(thisFolder, "args.txt"))) {
            for (String arg : args) {
                writer.write(arg);
                writer.write(System.getProperty("line.separator"));
            }
        }
    }

}
