package org.embulk.standards;

import static org.embulk.exec.GuessExecutor.registerDefaultGuessPluginTo;
import static org.embulk.plugin.InjectedPluginSource.registerPluginTo;

import com.google.common.base.Preconditions;
import com.google.inject.Binder;
import com.google.inject.Module;
import org.embulk.plugin.DefaultPluginType;
import org.embulk.spi.DecoderPlugin;
import org.embulk.spi.EncoderPlugin;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.FormatterPlugin;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.OutputPlugin;
import org.embulk.spi.ParserPlugin;

public class StandardPluginModule implements Module {
    @Override
    public void configure(Binder binder) {
        Preconditions.checkNotNull(binder, "binder is null.");

        // input plugins
        registerPluginTo(binder, InputPlugin.class, "config", ConfigInputPlugin.class);
        registerPluginTo(binder, InputPlugin.class, "file", LocalFileInputPlugin.class);

        // parser plugins
        registerPluginTo(binder, ParserPlugin.class, "csv", CsvParserPlugin.class);
        registerPluginTo(binder, ParserPlugin.class, "json", JsonParserPlugin.class);

        // file decoder plugins
        registerPluginTo(binder, DecoderPlugin.class, "gzip", GzipFileDecoderPlugin.class);
        registerPluginTo(binder, DecoderPlugin.class, "bzip2", Bzip2FileDecoderPlugin.class);

        // output plugins
        registerPluginTo(binder, OutputPlugin.class, "file", LocalFileOutputPlugin.class);
        registerPluginTo(binder, OutputPlugin.class, "null", NullOutputPlugin.class);
        registerPluginTo(binder, OutputPlugin.class, "stdout", StdoutOutputPlugin.class);

        // formatter plugins
        registerPluginTo(binder, FormatterPlugin.class, "csv", CsvFormatterPlugin.class);

        // file encoder plugins
        registerPluginTo(binder, EncoderPlugin.class, "gzip", GzipFileEncoderPlugin.class);
        registerPluginTo(binder, EncoderPlugin.class, "bzip2", Bzip2FileEncoderPlugin.class);

        // filter plugins
        registerPluginTo(binder, FilterPlugin.class, "rename", RenameFilterPlugin.class);
        registerPluginTo(binder, FilterPlugin.class, "remove_columns", RemoveColumnsFilterPlugin.class);

        // default guess plugins
        registerDefaultGuessPluginTo(binder, DefaultPluginType.create("gzip"));
        registerDefaultGuessPluginTo(binder, DefaultPluginType.create("bzip2"));
        registerDefaultGuessPluginTo(binder, DefaultPluginType.create("json")); // should be registered before CsvGuessPlugin
        registerDefaultGuessPluginTo(binder, DefaultPluginType.create("csv"));
        // charset and newline guess plugins are loaded and invoked by CsvGuessPlugin
    }
}
