package org.embulk.jruby;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import com.google.inject.Module;
import com.google.inject.Binder;
import com.google.inject.Scopes;
import com.google.inject.Provider;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.jruby.CompatVersion;
import org.jruby.embed.ScriptingContainer;
import org.embulk.plugin.PluginSource;

public class JRubyScriptingModule
        implements Module
{
    public JRubyScriptingModule()
    {
        // TODO get jruby-home from systemConfig to call jruby.container.setHomeDirectory
        // TODO get jruby-load-paths from systemConfig to call jruby.container.setLoadPaths
    }

    @Override
    public void configure(Binder binder)
    {
        binder.bind(ScriptingContainer.class).toProvider(ScriptingContainerProvider.class).in(Scopes.SINGLETON);
        //binder.bind(JRubyModule.class).in(Scopes.SINGLETON);

        Multibinder<PluginSource> multibinder = Multibinder.newSetBinder(binder, PluginSource.class);
        multibinder.addBinding().to(JRubyPluginSource.class);
    }

    private static class ScriptingContainerProvider
            implements Provider<ScriptingContainer>
    {
        private final Injector injector;

        @Inject
        public ScriptingContainerProvider(Injector injector)
        {
            this.injector = injector;
        }

        public ScriptingContainer get()
        {
            ScriptingContainer jruby = new ScriptingContainer();
            jruby.setCompatVersion(CompatVersion.RUBY1_9);

            // search embulk.rb path
            List<String> loadPaths = new ArrayList<String>();

            String coreJarPath = JRubyScriptingModule.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            loadPaths.add(coreJarPath);

            String home = System.getenv("EMBULK_HOME");
            if (home != null && !home.isEmpty()) {
                String homeLibPath = home + File.separator + "lib";
                loadPaths.add(homeLibPath);
            }

            jruby.setLoadPaths(loadPaths);

            // load embulk/java/bootstrap.rb from $EMBULK_HOME/lib
            jruby.runScriptlet("require 'embulk/java/bootstrap'");

            // define Embulk::Java::Injector
            jruby.put("Injector", injector);  // TODO use Embulk::Java::Injector
            jruby.runScriptlet("require 'embulk/java/injected'");

            // load embulk.rb from $EMBULK_HOME/lib
            jruby.runScriptlet("require 'embulk'");

            // load embulk/java/bridge.rb from $EMBULK_HOME/lib
            jruby.runScriptlet("require 'embulk/java/bridge'");

            return jruby;
        }
    }
}
