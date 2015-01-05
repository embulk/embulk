package org.embulk.jruby;

import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.RubyString;
import org.jruby.util.ByteList;
import org.embulk.spi.Buffer;
import org.embulk.spi.Exec;

public class BufferBridge
{
    private BufferBridge() { }

    public static IRubyObject rubyObject(Buffer value)
    {
        return new RubyString(
                Exec.getJRubyBridge().getRuntime(),
                Exec.getJRubyBridge().getConstMetaClass("Embulk::Buffer"),
                new ByteList(value.array(), value.offset(), value.limit(), false));
    }

    public static Buffer newFromString(RubyString string)
    {
        ByteList b = string.getByteList();
        // TODO optimize
        //Buffer.wrap(b.unsafeBytes(), b.begin(), b.length());
        return Buffer.wrap(b.bytes());
    }
}
