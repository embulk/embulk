package org.embulk.jruby;

import com.google.inject.Inject;
import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.embed.ScriptingContainer;
import org.jruby.javasupport.JavaObject;

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

    public IRubyObject callOnConst(String constName, String methodName, Object... args)
    {
        return (IRubyObject) jruby.callMethod(getConst(constName), methodName, args);
    }

    public IRubyObject wrapJavaObject(Object object)
    {
        return JavaObject.wrap(getRuntime(), object);
    }
}
