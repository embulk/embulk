package org.embulk.standards;

import com.google.common.base.Preconditions;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.name.Names;
import org.embulk.spi.FormatterPlugin;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.OutputPlugin;
import org.embulk.spi.ParserPlugin;
import org.embulk.spi.DecoderPlugin;
import org.embulk.spi.EncoderPlugin;
import static org.embulk.plugin.InjectedPluginSource.registerPlugin;

public class StandardPluginModule implements Module
{
    @Override
    public void configure(Binder binder)
    {
        Preconditions.checkNotNull(binder, "binder is null.");

        // input plugins
        registerPlugin(binder, InputPlugin.class, "file", LocalFileInputPlugin.class);
        registerPlugin(binder, InputPlugin.class, "s3_file", S3FileInputPlugin.class);

        // parser plugins
        registerPlugin(binder, ParserPlugin.class, "csv", CsvParserPlugin.class);

        // file decoder plugins
        registerPlugin(binder, DecoderPlugin.class, "gzip", GzipFileDecoderPlugin.class);

        // output plugins
        registerPlugin(binder, OutputPlugin.class, "file", LocalFileOutputPlugin.class);
        registerPlugin(binder, OutputPlugin.class, "null", NullOutputPlugin.class);

        // formatter plugins
        registerPlugin(binder, FormatterPlugin.class, "csv", CsvFormatterPlugin.class);

        // file encoder plugins
        registerPlugin(binder, EncoderPlugin.class, "gzip", GzipFileEncoderPlugin.class);
    }
}
