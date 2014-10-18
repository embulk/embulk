package org.quickload.cli;

import com.google.common.base.Preconditions;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.name.Names;
import org.quickload.spi.FormatterPlugin;
import org.quickload.spi.InputPlugin;
import org.quickload.spi.OutputPlugin;
import org.quickload.spi.ParserPlugin;
import org.quickload.standards.*;

public class MyPluginModule implements Module
{
    @Override
    public void configure(Binder binder)
    {
        Preconditions.checkNotNull(binder, "binder is null.");
        binder.bind(InputPlugin.class).annotatedWith(Names.named("in_local_file")).to(LocalFileInputPlugin.class);
        //binder.bind(InputPlugin.class).annotatedWith(Names.named("in_s3_file")).to(S3FileInputPlugin.class);
        binder.bind(OutputPlugin.class).annotatedWith(Names.named("my")).to(LocalFileOutputPlugin.class);
        binder.bind(ParserPlugin.class).annotatedWith(Names.named("my")).to(CsvParserPlugin.class);
        //binder.bind(FormatterPlugin.class).annotatedWith(Names.named("my")).to(CsvFormatterPlugin.class);
        binder.bind(FormatterPlugin.class).annotatedWith(Names.named("my")).to(MessagePackFormatterPlugin.class);
    }
}
