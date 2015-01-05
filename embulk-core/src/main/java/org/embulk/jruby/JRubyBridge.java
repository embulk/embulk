package org.embulk.jruby;

import com.google.inject.Inject;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.embed.ScriptingContainer;

public class JRubyBridge
{
    private final ScriptingContainer jruby;

    @Inject
    public JRubyBridge(ScriptingContainer jruby)
    {
        this.jruby = jruby;
    }

    public Ruby getRuntime()
    {
        return jruby.getProvider().getRuntime();
    }

    public IRubyObject getConst(String constName)
    {
        // TODO cache?
        return (IRubyObject) jruby.runScriptlet(constName);
    }

    public RubyClass getConstMetaClass(String constName)
    {
        return getConst(constName).getMetaClass();
    }
}
