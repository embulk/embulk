package org.embulk.standards;

import com.google.inject.Binder;
import com.google.inject.Module;
import org.embulk.plugin.InjectedPluginSource;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.FormatterPlugin;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.OutputPlugin;
import org.embulk.spi.ParserPlugin;

public class RemoveColumnsFilterPluginTestModule implements Module {
    public RemoveColumnsFilterPluginTestModule() {
    }

    @Override
    public void configure(final Binder binder) {
        InjectedPluginSource.registerPluginTo(binder, FormatterPlugin.class, "csv", CsvFormatterPlugin.class);
        InjectedPluginSource.registerPluginTo(binder, InputPlugin.class, "file", LocalFileInputPlugin.class);
        InjectedPluginSource.registerPluginTo(binder, OutputPlugin.class, "file", LocalFileOutputPlugin.class);
        InjectedPluginSource.registerPluginTo(binder, ParserPlugin.class, "csv", CsvParserPlugin.class);
        InjectedPluginSource.registerPluginTo(binder, FilterPlugin.class, "remove_columns", RemoveColumnsFilterPlugin.class);
    }
}
