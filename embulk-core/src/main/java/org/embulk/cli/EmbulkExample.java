package org.embulk.cli;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPOutputStream;

public class EmbulkExample {
    public void createExample(final String basePathInString) throws IOException {
        createExample(Paths.get(basePathInString));
    }

    public void createExample(final Path basePath) throws IOException {
        // TODO(dmikurube): Log with a kind of loggers instead of |System.out|.
        System.out.printf("Creating %s directory...\n", basePath.toString());

        System.out.printf("  Creating %s/\n", basePath.toString());
        final Path csvPath = basePath.resolve("csv");
        Files.createDirectories(csvPath);
        System.out.printf("  Creating %s/\n", csvPath.toString());

        final Path csvSamplePath = csvPath.resolve("sample_01.csv.gz");
        System.out.printf("  Creating %s\n", csvSamplePath.toString());
        outputSampleCsv(csvSamplePath);

        final Path ymlSamplePath = basePath.resolve("seed.yml");
        System.out.printf("  Creating %s\n", ymlSamplePath.toString());
        outputSampleYml(csvPath, ymlSamplePath);

        System.out.println("");
        System.out.println("Run following subcommands to try embulk:");
        System.out.println("");
        System.out.printf("   1. embulk guess %s -o config.yml\n", ymlSamplePath.toString());
        System.out.println("   2. embulk preview config.yml");
        System.out.println("   3. embulk run config.yml");
        System.out.println("");
    }

    private void outputSampleCsv(final Path csvSamplePath) throws IOException {
        // TODO(dmikurube): Move the data into Java resources.
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("id,account,time,purchase,comment\n");
        csvBuilder.append("1,32864,2015-01-27 19:23:49,20150127,embulk\n");
        csvBuilder.append("2,14824,2015-01-27 19:01:23,20150127,embulk jruby\n");
        csvBuilder.append("3,27559,2015-01-28 02:20:02,20150128,\"Embulk \"\"csv\"\" parser plugin\"\n");
        csvBuilder.append("4,11270,2015-01-29 11:54:36,20150129,NULL\n");
        csvBuilder.append("\n");
        byte[] csvSample = csvBuilder.toString().getBytes(StandardCharsets.UTF_8);

        try (GZIPOutputStream output = new GZIPOutputStream(Files.newOutputStream(csvSamplePath))) {
            output.write(csvSample);
        }
    }

    private void outputSampleYml(final Path csvPath, final Path ymlSamplePath) throws IOException {
        // TODO(dmikurube): Move the data into Java resources.
        StringBuilder ymlBuilder = new StringBuilder();
        ymlBuilder.append("in:\n");
        ymlBuilder.append("  type: file\n");

        // Use single-quotes to quote path strings in YAML for Windows.
        // Ref YAML spec: Single-Quoted Style
        // http://yaml.org/spec/1.2/spec.html#id2788097
        ymlBuilder.append(String.format("  path_prefix: \'%s\'\n",
                                        csvPath.toAbsolutePath().resolve("sample_").toString()));
        ymlBuilder.append("out:\n");
        ymlBuilder.append("  type: stdout\n");
        byte[] ymlSample = ymlBuilder.toString().getBytes(StandardCharsets.UTF_8);

        try (OutputStream output = Files.newOutputStream(ymlSamplePath)) {
            output.write(ymlSample);
        }
    }
}
