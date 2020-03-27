package org.embulk.standards;

import static org.embulk.exec.GuessExecutor.registerDefaultGuessPluginTo;
import static org.embulk.plugin.InjectedPluginSource.registerPluginTo;

import com.google.common.base.Preconditions;
import com.google.inject.Binder;
import com.google.inject.Module;
import org.embulk.EmbulkSystemProperties;
import org.embulk.plugin.DefaultPluginType;
import org.embulk.spi.DecoderPlugin;
import org.embulk.spi.EncoderPlugin;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.FormatterPlugin;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.OutputPlugin;
import org.embulk.spi.ParserPlugin;

public class StandardPluginModule implements Module {
    public StandardPluginModule(final EmbulkSystemProperties embulkSystemProperties) {
        this.embulkSystemProperties = embulkSystemProperties;
    }

    @Override
    public void configure(Binder binder) {
        Preconditions.checkNotNull(binder, "binder is null.");

        // input plugins
        if (!embulkSystemProperties.getPropertyAsBoolean("standards.input.config.disabled", false)) {
            registerPluginTo(binder, InputPlugin.class, "config", ConfigInputPlugin.class);
        }
        if (!embulkSystemProperties.getPropertyAsBoolean("standards.input.file.disabled", false)) {
            registerPluginTo(binder, InputPlugin.class, "file", LocalFileInputPlugin.class);
        }

        // parser plugins
        if (!embulkSystemProperties.getPropertyAsBoolean("standards.parser.csv.disabled", false)) {
            registerPluginTo(binder, ParserPlugin.class, "csv", CsvParserPlugin.class);
        }
        if (!embulkSystemProperties.getPropertyAsBoolean("standards.parser.json.disabled", false)) {
            registerPluginTo(binder, ParserPlugin.class, "json", JsonParserPlugin.class);
        }

        // file decoder plugins
        if (!embulkSystemProperties.getPropertyAsBoolean("standards.decoder.gzip.disabled", false)) {
            registerPluginTo(binder, DecoderPlugin.class, "gzip", GzipFileDecoderPlugin.class);
        }
        if (!embulkSystemProperties.getPropertyAsBoolean("standards.decoder.bzip2.disabled", false)) {
            registerPluginTo(binder, DecoderPlugin.class, "bzip2", Bzip2FileDecoderPlugin.class);
        }

        // output plugins
        if (!embulkSystemProperties.getPropertyAsBoolean("standards.output.file.disabled", false)) {
            registerPluginTo(binder, OutputPlugin.class, "file", LocalFileOutputPlugin.class);
        }
        if (!embulkSystemProperties.getPropertyAsBoolean("standards.output.null.disabled", false)) {
            registerPluginTo(binder, OutputPlugin.class, "null", NullOutputPlugin.class);
        }
        if (!embulkSystemProperties.getPropertyAsBoolean("standards.output.stdout.disabled", false)) {
            registerPluginTo(binder, OutputPlugin.class, "stdout", StdoutOutputPlugin.class);
        }

        // formatter plugins
        if (!embulkSystemProperties.getPropertyAsBoolean("standards.formatter.csv.disabled", false)) {
            registerPluginTo(binder, FormatterPlugin.class, "csv", CsvFormatterPlugin.class);
        }

        // file encoder plugins
        if (!embulkSystemProperties.getPropertyAsBoolean("standards.encoder.gzip.disabled", false)) {
            registerPluginTo(binder, EncoderPlugin.class, "gzip", GzipFileEncoderPlugin.class);
        }
        if (!embulkSystemProperties.getPropertyAsBoolean("standards.encoder.bzip2.disabled", false)) {
            registerPluginTo(binder, EncoderPlugin.class, "bzip2", Bzip2FileEncoderPlugin.class);
        }

        // filter plugins
        if (!embulkSystemProperties.getPropertyAsBoolean("standards.filter.rename.disabled", false)) {
            registerPluginTo(binder, FilterPlugin.class, "rename", RenameFilterPlugin.class);
        }
        if (!embulkSystemProperties.getPropertyAsBoolean("standards.filter.remove_columns.disabled", false)) {
            registerPluginTo(binder, FilterPlugin.class, "remove_columns", RemoveColumnsFilterPlugin.class);
        }

        // default guess plugins
        if (!embulkSystemProperties.getPropertyAsBoolean("standards.decoder.gzip.disabled", false)) {
            registerDefaultGuessPluginTo(binder, DefaultPluginType.create("gzip"));
        }
        if (!embulkSystemProperties.getPropertyAsBoolean("standards.decoder.bzip2.disabled", false)) {
            registerDefaultGuessPluginTo(binder, DefaultPluginType.create("bzip2"));
        }
        if (!embulkSystemProperties.getPropertyAsBoolean("standards.parser.json.disabled", false)) {
            // should be registered before CsvGuessPlugin
            registerDefaultGuessPluginTo(binder, DefaultPluginType.create("json"));
        }
        if (!embulkSystemProperties.getPropertyAsBoolean("standards.parser.csv.disabled", false)) {
            registerDefaultGuessPluginTo(binder, DefaultPluginType.create("csv"));
        }
        // charset and newline guess plugins are loaded and invoked by CsvGuessPlugin
    }

    private final EmbulkSystemProperties embulkSystemProperties;
}
