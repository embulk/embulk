package org.quickload.standards;

import com.google.common.base.Preconditions;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.name.Names;
import org.quickload.spi.FormatterPlugin;
import org.quickload.spi.InputPlugin;
import org.quickload.spi.OutputPlugin;
import org.quickload.spi.ParserPlugin;

public class StandardPluginModule implements Module
{
    @Override
    public void configure(Binder binder)
    {
        Preconditions.checkNotNull(binder, "binder is null.");

        // input plugin
        binder.bind(InputPlugin.class).annotatedWith(Names.named("local_file")).to(LocalFileInputPlugin.class);
        binder.bind(InputPlugin.class).annotatedWith(Names.named("s3_file")).to(S3FileInputPlugin.class);

        // output plugin
        binder.bind(OutputPlugin.class).annotatedWith(Names.named("local_file")).to(LocalFileOutputPlugin.class);

        // parser plugin
        binder.bind(ParserPlugin.class).annotatedWith(Names.named("csv")).to(CsvParserPlugin.class);

        // formatter plugin
        binder.bind(FormatterPlugin.class).annotatedWith(Names.named("csv")).to(CsvFormatterPlugin.class);
        binder.bind(FormatterPlugin.class).annotatedWith(Names.named("msgpack")).to(MessagePackFormatterPlugin.class);
    }
}
