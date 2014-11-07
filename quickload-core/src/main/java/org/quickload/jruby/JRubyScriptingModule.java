package org.quickload.jruby;

import com.google.inject.Module;
import com.google.inject.Binder;
import com.google.inject.Scopes;
import com.google.inject.Provider;
import org.jruby.embed.ScriptingContainer;
import org.jruby.CompatVersion;
import org.quickload.config.Task;
import org.quickload.config.ConfigSource;

public class JRubyScriptingModule
        implements Module
{
    public JRubyScriptingModule()
    {
        // TODO get jruby-home from systemConfig to call jruby.container.setHomeDirectory
        // TODO get jruby-load-paths from systemConfig to call jruby.container.setLoadPaths
        // TODO get jruby-home from systemConfig to call jruby.container.setHomeDirectory
    }

    @Override
    public void configure(Binder binder)
    {
        binder.bind(ScriptingContainer.class).toProvider(new ScriptingContainerProvider()).in(Scopes.SINGLETON);
    }

    private static class ScriptingContainerProvider
            implements Provider<ScriptingContainer>
    {
        public ScriptingContainer get()
        {
            ScriptingContainer jruby = new ScriptingContainer();
            jruby.setCompatVersion(CompatVersion.RUBY1_9);
            return jruby;
        }
    }
}
