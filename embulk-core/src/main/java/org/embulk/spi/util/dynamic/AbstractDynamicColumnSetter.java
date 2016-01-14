package org.embulk.spi.util.dynamic;

import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.RubyNil;
import org.jruby.RubyBoolean;
import org.jruby.RubyInteger;
import org.jruby.RubyFloat;
import org.jruby.RubyString;
import org.jruby.RubyTime;
import org.jruby.exceptions.RaiseException;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.Column;
import org.embulk.spi.util.DynamicColumnSetter;
import org.embulk.spi.time.Timestamp;
import org.embulk.spi.json.RubyValueApi;
import org.msgpack.value.Value;

public abstract class AbstractDynamicColumnSetter
        implements DynamicColumnSetter
{
    protected final PageBuilder pageBuilder;
    protected final Column column;
    protected final DefaultValueSetter defaultValue;

    protected AbstractDynamicColumnSetter(PageBuilder pageBuilder, Column column,
            DefaultValueSetter defaultValue)
    {
        this.pageBuilder = pageBuilder;
        this.column = column;
        this.defaultValue = defaultValue;
    }

    public abstract void setNull();

    public abstract void set(boolean value);

    public abstract void set(long value);

    public abstract void set(double value);

    public abstract void set(String value);

    public abstract void set(Timestamp value);

    public abstract void set(Value value);

    public void setRubyObject(IRubyObject rubyObject)
    {
        if (rubyObject == null || rubyObject instanceof RubyNil) {
            setNull();
        } else if (rubyObject instanceof RubyBoolean) {
            RubyBoolean b = (RubyBoolean) rubyObject;
            set(b.isTrue());
        } else if (rubyObject instanceof RubyInteger) {
            RubyInteger i = (RubyInteger) rubyObject;
            try {
                set(i.getLongValue());
            } catch (RaiseException ex) {
                if ("RangeError".equals(ex.getException().getMetaClass().getBaseName())) {
                    // integer is too large
                    throw ex;  //TODO setDefaultValue();
                } else {
                    throw ex;
                }
            }
        } else if (rubyObject instanceof RubyFloat) {
            RubyFloat f = (RubyFloat) rubyObject;
            set(f.getDoubleValue());
        } else if (rubyObject instanceof RubyString) {
            RubyString s = (RubyString) rubyObject;
            set(s.asJavaString());
        } else if (rubyObject instanceof RubyTime) {
            RubyTime time = (RubyTime) rubyObject;
            long msec = time.getDateTime().getMillis();
            long nsec = time.getNSec();
            long sec = msec / 1000 + nsec / 1000000000;
            int nano = (int) ((msec % 1000) * 1000000 + nsec % 1000000000);
            set(Timestamp.ofEpochSecond(sec, nano));
        } else {
            set(RubyValueApi.toValue(rubyObject.getRuntime(), rubyObject));
        }
    }
}
