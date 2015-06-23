package org.embulk.spi.util.dynamic;

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

    public void set(RubyBoolean rubyObject)
    {
        set(rubyObject.isTrue());
    }

    public void set(RubyInteger rubyObject)
    {
        try {
            set(rubyObject.getLongValue());
        } catch (RaiseException ex) {
            // integer is too large
            if ("RangeError".equals(ex.getException().getMetaClass().getBaseName())) {
                // TODO setDefaultValue();
                throw ex;
            } else {
                throw ex;
            }
        }
    }

    public void set(RubyFloat rubyObject)
    {
        set(rubyObject.getDoubleValue());
    }

    public void set(RubyString rubyObject)
    {
        set(rubyObject.asJavaString());
    }

    public void set(RubyTime rubyObject)
    {
        long msec = rubyObject.getDateTime().getMillis();
        long nsec = rubyObject.getNSec();
        long sec = msec / 1000 + nsec / 1000000000;
        int nano = (int) ((msec % 1000) * 1000000 + nsec % 1000000000);
        set(Timestamp.ofEpochSecond(sec, nano));
    }

    //public abstract void set(IRubyObject rubyObject);
}
