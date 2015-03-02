package org.embulk.standards;

import com.google.common.base.Preconditions;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.google.inject.multibindings.Multibinder;
import org.embulk.spi.FormatterPlugin;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.OutputPlugin;
import org.embulk.spi.ParserPlugin;
import org.embulk.spi.DecoderPlugin;
import org.embulk.spi.EncoderPlugin;
import org.embulk.exec.GuessExecutor;
import org.embulk.plugin.PluginType;
import static org.embulk.plugin.InjectedPluginSource.registerPluginTo;
import static org.embulk.exec.GuessExecutor.registerDefaultGuessPluginTo;

public class StandardPluginModule
        implements Module
{
    @Override
    public void configure(Binder binder)
    {
        Preconditions.checkNotNull(binder, "binder is null.");

        // input plugins
        registerPluginTo(binder, InputPlugin.class, "file", LocalFileInputPlugin.class);

        // parser plugins
        registerPluginTo(binder, ParserPlugin.class, "csv", CsvParserPlugin.class);

        // file decoder plugins
        registerPluginTo(binder, DecoderPlugin.class, "gzip", GzipFileDecoderPlugin.class);

        // output plugins
        registerPluginTo(binder, OutputPlugin.class, "file", LocalFileOutputPlugin.class);
        registerPluginTo(binder, OutputPlugin.class, "null", NullOutputPlugin.class);
        registerPluginTo(binder, OutputPlugin.class, "stdout", StdoutOutputPlugin.class);

        // formatter plugins
        registerPluginTo(binder, FormatterPlugin.class, "csv", CsvFormatterPlugin.class);

        // file encoder plugins
        registerPluginTo(binder, EncoderPlugin.class, "gzip", GzipFileEncoderPlugin.class);

        // default guess plugins
        registerDefaultGuessPluginTo(binder, new PluginType("gzip"));
        registerDefaultGuessPluginTo(binder, new PluginType("charset"));
        registerDefaultGuessPluginTo(binder, new PluginType("newline"));
        registerDefaultGuessPluginTo(binder, new PluginType("csv"));
    }
}
