package org.embulk.standards;

import static org.embulk.exec.GuessExecutor.registerDefaultGuessPluginTo;

import com.google.common.base.Preconditions;
import com.google.inject.Binder;
import com.google.inject.Module;
import org.embulk.EmbulkSystemProperties;
import org.embulk.plugin.DefaultPluginType;

public class StandardPluginModule implements Module {
    public StandardPluginModule(final EmbulkSystemProperties embulkSystemProperties) {
        this.embulkSystemProperties = embulkSystemProperties;
    }

    @Override
    public void configure(Binder binder) {
        Preconditions.checkNotNull(binder, "binder is null.");

        // default guess plugins
        if (!embulkSystemProperties.getPropertyAsBoolean("standards.decoder.gzip.disabled", false)) {
            registerDefaultGuessPluginTo(binder, DefaultPluginType.create("gzip"));
        }
        if (!embulkSystemProperties.getPropertyAsBoolean("standards.decoder.bzip2.disabled", false)) {
            registerDefaultGuessPluginTo(binder, DefaultPluginType.create("bzip2"));
        }
        if (!embulkSystemProperties.getPropertyAsBoolean("standards.parser.json.disabled", false)) {
            // should be registered before CsvGuessPlugin
            registerDefaultGuessPluginTo(binder, DefaultPluginType.create("json"));
        }
        if (!embulkSystemProperties.getPropertyAsBoolean("standards.parser.csv.disabled", false)) {
            registerDefaultGuessPluginTo(binder, DefaultPluginType.create("csv"));
        }
        // charset and newline guess plugins are loaded and invoked by CsvGuessPlugin
    }

    private final EmbulkSystemProperties embulkSystemProperties;
}
