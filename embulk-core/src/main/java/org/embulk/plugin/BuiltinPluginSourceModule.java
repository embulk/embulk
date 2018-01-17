package org.embulk.plugin;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.multibindings.Multibinder;
import org.embulk.exec.ForGuess;

public class BuiltinPluginSourceModule implements Module {
    @Override
    public void configure(Binder binder) {
        Multibinder<PluginSource> multibinder = Multibinder.newSetBinder(binder, PluginSource.class);
        // multibinder.addBinding().to(LocalDirectoryPluginSource.class);  // TODO
        multibinder.addBinding().to(InjectedPluginSource.class);

        // This workaround allows no any guess plugin registered. See also:
        // https://github.com/embulk/embulk/issues/876
        // https://groups.google.com/forum/#!topic/google-guice/5Rnm-d7MU34
        // TODO: Remove this workaround.
        Multibinder.newSetBinder(binder, PluginType.class, ForGuess.class);
    }
}
