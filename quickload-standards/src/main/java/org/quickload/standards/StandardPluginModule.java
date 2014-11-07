package org.quickload.standards;

import com.google.common.base.Preconditions;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.name.Names;
import org.quickload.spi.FormatterPlugin;
import org.quickload.spi.InputPlugin;
import org.quickload.spi.OutputPlugin;
import org.quickload.spi.ParserPlugin;
import org.quickload.spi.FileDecoderPlugin;
import org.quickload.spi.FileEncoderPlugin;

public class StandardPluginModule implements Module
{
    @Override
    public void configure(Binder binder)
    {
        Preconditions.checkNotNull(binder, "binder is null.");

        // input plugins
        binder.bind(InputPlugin.class).annotatedWith(Names.named("file")).to(LocalFileInputPlugin.class);
        binder.bind(InputPlugin.class).annotatedWith(Names.named("s3_file")).to(S3FileInputPlugin.class);

        // parser plugins
        binder.bind(ParserPlugin.class).annotatedWith(Names.named("csv")).to(CsvParserPlugin.class);

        // file decoder plugins
        binder.bind(FileDecoderPlugin.class).annotatedWith(Names.named("gzip")).to(GzipFileDecoderPlugin.class);

        // output plugins
        binder.bind(OutputPlugin.class).annotatedWith(Names.named("file")).to(LocalFileOutputPlugin.class);

        // formatter plugins
        binder.bind(FormatterPlugin.class).annotatedWith(Names.named("csv")).to(CsvFormatterPlugin.class);
        binder.bind(FormatterPlugin.class).annotatedWith(Names.named("msgpack")).to(MessagePackFormatterPlugin.class);

        // file encoder plugins
        binder.bind(FileEncoderPlugin.class).annotatedWith(Names.named("gzip")).to(GzipFileEncoderPlugin.class);
    }
}
