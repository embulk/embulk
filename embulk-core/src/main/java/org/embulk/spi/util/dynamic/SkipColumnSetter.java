package org.embulk.spi.util.dynamic;

import org.jruby.runtime.builtin.IRubyObject;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.Column;
import org.embulk.spi.time.Timestamp;
import org.embulk.spi.time.TimestampParser;
import org.embulk.spi.time.TimestampParseException;
import org.msgpack.value.Value;

public class SkipColumnSetter
        extends AbstractDynamicColumnSetter
{
    private static final SkipColumnSetter instance = new SkipColumnSetter();

    public static SkipColumnSetter get()
    {
        return instance;
    }

    private SkipColumnSetter()
    {
        super(null, null, null);
    }

    @Override
    public void setNull()
    { }

    @Override
    public void set(boolean v)
    { }

    @Override
    public void set(long v)
    { }

    @Override
    public void set(double v)
    { }

    @Override
    public void set(String v)
    { }

    @Override
    public void set(Timestamp v)
    { }

    @Override
    public void set(Value v)
    { }

    @Deprecated
    @Override
    public void setRubyObject(IRubyObject rubyObject)
    {
        if (!deprecationWarned) {
            System.err.println("[WARN] Plugin uses deprecated org.embulk.spi.util.dynamic.SkipColumnSetter#setRubyObject");
            System.err.println("[WARN] Report plugins in your config at: https://github.com/embulk/embulk/issues/799");
            // The |deprecationWarned| flag is used only for warning messages.
            // Even in case of race conditions, messages are just duplicated -- should be acceptable.
            deprecationWarned = true;
        }
    }

    private static boolean deprecationWarned = false;
}
