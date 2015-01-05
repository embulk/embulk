package org.embulk.jruby;

import org.jruby.runtime.builtin.IRubyObject;
import org.embulk.config.DataSourceImpl;
import org.embulk.config.ModelManager;
import org.embulk.spi.Exec;

public class DataSourceBridge
{
    private DataSourceBridge() { }

    public static interface Meta
    {
        public IRubyObject convert(String json);
    }

    public static IRubyObject rubyObject(DataSourceImpl value)
    {
        DataSourceBridge.Meta meta = (DataSourceBridge.Meta) Exec.getJRubyBridge().getConstMetaClass("Embulk::DataSource");
        return meta.convert(value.toString());
    }

    public static DataSourceImpl newFromJson(ModelManager model, String json)
    {
        return model.readObject(DataSourceImpl.class, json);
    }
}
