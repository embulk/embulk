package org.embulk.standards;

import com.google.common.base.Preconditions;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.name.Names;
import org.embulk.spi.FormatterPlugin;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.OutputPlugin;
import org.embulk.spi.ParserPlugin;
import org.embulk.spi.FileDecoderPlugin;
import org.embulk.spi.FileEncoderPlugin;

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
        binder.bind(OutputPlugin.class).annotatedWith(Names.named("null")).to(NullOutputPlugin.class);

        // formatter plugins
        binder.bind(FormatterPlugin.class).annotatedWith(Names.named("csv")).to(CsvFormatterPlugin.class);

        // file encoder plugins
        binder.bind(FileEncoderPlugin.class).annotatedWith(Names.named("gzip")).to(GzipFileEncoderPlugin.class);
    }
}
