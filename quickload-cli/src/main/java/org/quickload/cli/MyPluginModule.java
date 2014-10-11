package org.quickload.cli;

import com.google.common.base.Preconditions;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.name.Names;
import org.quickload.spi.FormatterPlugin;
import org.quickload.spi.InputPlugin;
import org.quickload.spi.OutputPlugin;
import org.quickload.spi.ParserPlugin;
import org.quickload.standards.CsvFormatterPlugin;
import org.quickload.standards.CsvParserPlugin;
import org.quickload.standards.LocalFileCsvInputPlugin;
import org.quickload.standards.LocalFileCsvOutputPlugin;

public class MyPluginModule implements Module
{
    @Override
    public void configure(Binder binder)
    {
        Preconditions.checkNotNull(binder, "binder is null.");
        binder.bind(InputPlugin.class).annotatedWith(Names.named("my")).to(LocalFileCsvInputPlugin.class);
        binder.bind(OutputPlugin.class).annotatedWith(Names.named("my")).to(LocalFileCsvOutputPlugin.class);
        binder.bind(ParserPlugin.class).annotatedWith(Names.named("my")).to(CsvParserPlugin.class);
        binder.bind(FormatterPlugin.class).annotatedWith(Names.named("my")).to(CsvFormatterPlugin.class);
    }
}
