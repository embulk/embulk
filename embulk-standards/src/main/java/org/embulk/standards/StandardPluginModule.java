package org.embulk.standards;

import static org.embulk.exec.GuessExecutor.registerDefaultGuessPluginTo;
import static org.embulk.plugin.InjectedPluginSource.registerPluginTo;

import com.google.common.base.Preconditions;
import com.google.inject.Binder;
import com.google.inject.Module;
import org.embulk.config.ConfigSource;
import org.embulk.plugin.DefaultPluginType;
import org.embulk.spi.DecoderPlugin;
import org.embulk.spi.EncoderPlugin;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.FormatterPlugin;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.OutputPlugin;
import org.embulk.spi.ParserPlugin;

public class StandardPluginModule implements Module {
    public StandardPluginModule(final ConfigSource systemConfig) {
        this.systemConfig = systemConfig;
    }

    @Override
    public void configure(Binder binder) {
        Preconditions.checkNotNull(binder, "binder is null.");

        // input plugins
        if (!systemConfig.get(boolean.class, "standards.input.config.disabled", false)) {
            registerPluginTo(binder, InputPlugin.class, "config", ConfigInputPlugin.class);
        }
        if (!systemConfig.get(boolean.class, "standards.input.file.disabled", false)) {
            registerPluginTo(binder, InputPlugin.class, "file", LocalFileInputPlugin.class);
        }

        // parser plugins
        if (!systemConfig.get(boolean.class, "standards.parser.csv.disabled", false)) {
            registerPluginTo(binder, ParserPlugin.class, "csv", CsvParserPlugin.class);
        }
        if (!systemConfig.get(boolean.class, "standards.parser.json.disabled", false)) {
            registerPluginTo(binder, ParserPlugin.class, "json", JsonParserPlugin.class);
        }

        // file decoder plugins
        if (!systemConfig.get(boolean.class, "standards.decoder.gzip.disabled", false)) {
            registerPluginTo(binder, DecoderPlugin.class, "gzip", GzipFileDecoderPlugin.class);
        }
        if (!systemConfig.get(boolean.class, "standards.decoder.bzip2.disabled", false)) {
            registerPluginTo(binder, DecoderPlugin.class, "bzip2", Bzip2FileDecoderPlugin.class);
        }

        // output plugins
        if (!systemConfig.get(boolean.class, "standards.output.file.disabled", false)) {
            registerPluginTo(binder, OutputPlugin.class, "file", LocalFileOutputPlugin.class);
        }
        if (!systemConfig.get(boolean.class, "standards.output.null.disabled", false)) {
            registerPluginTo(binder, OutputPlugin.class, "null", NullOutputPlugin.class);
        }
        if (!systemConfig.get(boolean.class, "standards.output.stdout.disabled", false)) {
            registerPluginTo(binder, OutputPlugin.class, "stdout", StdoutOutputPlugin.class);
        }

        // formatter plugins
        if (!systemConfig.get(boolean.class, "standards.formatter.csv.disabled", false)) {
            registerPluginTo(binder, FormatterPlugin.class, "csv", CsvFormatterPlugin.class);
        }

        // file encoder plugins
        if (!systemConfig.get(boolean.class, "standards.encoder.gzip.disabled", false)) {
            registerPluginTo(binder, EncoderPlugin.class, "gzip", GzipFileEncoderPlugin.class);
        }
        if (!systemConfig.get(boolean.class, "standards.encoder.bzip2.disabled", false)) {
            registerPluginTo(binder, EncoderPlugin.class, "bzip2", Bzip2FileEncoderPlugin.class);
        }

        // filter plugins
        if (!systemConfig.get(boolean.class, "standards.filter.rename.disabled", false)) {
            registerPluginTo(binder, FilterPlugin.class, "rename", RenameFilterPlugin.class);
        }
        if (!systemConfig.get(boolean.class, "standards.filter.remove_columns.disabled", false)) {
            registerPluginTo(binder, FilterPlugin.class, "remove_columns", RemoveColumnsFilterPlugin.class);
        }

        // default guess plugins
        if (!systemConfig.get(boolean.class, "standards.decoder.gzip.disabled", false)) {
            registerDefaultGuessPluginTo(binder, DefaultPluginType.create("gzip"));
        }
        if (!systemConfig.get(boolean.class, "standards.decoder.bzip2.disabled", false)) {
            registerDefaultGuessPluginTo(binder, DefaultPluginType.create("bzip2"));
        }
        if (!systemConfig.get(boolean.class, "standards.parser.json.disabled", false)) {
            // should be registered before CsvGuessPlugin
            registerDefaultGuessPluginTo(binder, DefaultPluginType.create("json"));
        }
        if (!systemConfig.get(boolean.class, "standards.parser.csv.disabled", false)) {
            registerDefaultGuessPluginTo(binder, DefaultPluginType.create("csv"));
        }
        // charset and newline guess plugins are loaded and invoked by CsvGuessPlugin
    }

    private final ConfigSource systemConfig;
}
