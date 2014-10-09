package org.quickload.cli;

import com.google.common.base.Preconditions;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.name.Names;
import org.quickload.spi.CSVParserPlugin;
import org.quickload.spi.ParserPlugin;

public class MyPluginSettingsModule implements Module
{
    @Override
    public void configure(Binder binder)
    {
        Preconditions.checkNotNull(binder, "binder is null.");
        binder.bind(ParserPlugin.class).annotatedWith(Names.named("my")).to(CSVParserPlugin.class);
    }
}
