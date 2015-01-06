package org.embulk.jruby;

import org.jruby.runtime.builtin.IRubyObject;
import org.embulk.spi.GuessPlugin;
import org.embulk.spi.Exec;

public class GuessPluginBridge
{
    private GuessPluginBridge() { }

    public static IRubyObject rubyObject(GuessPlugin value)
    {
        return Exec.getJRubyBridge().callOnConst("Embulk::Plugin::GuessPlugin", "ruby_object", value);
    }
}
