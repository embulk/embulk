package org.embulk.jruby;

import org.jruby.runtime.builtin.IRubyObject;
import org.embulk.config.DataSourceImpl;
import org.embulk.config.ModelManager;
import org.embulk.spi.Exec;

public class DataSourceBridge
{
    private DataSourceBridge() { }

    public static IRubyObject rubyObject(DataSourceImpl value)
    {
        return Exec.getJRubyBridge().callOnConst("Embulk::DataSource", "ruby_object", value.toString());
    }

    public static IRubyObject newFromJson(ModelManager model, String json)
    {
        return Exec.getJRubyBridge().wrapJavaObject(
                model.readObject(DataSourceImpl.class, json));
    }
}
