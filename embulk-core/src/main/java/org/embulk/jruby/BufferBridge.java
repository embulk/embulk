package org.embulk.jruby;

import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.RubyClass;
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
                (RubyClass) Exec.getJRubyBridge().getConst("Embulk::Buffer"),
                new ByteList(value.array(), value.offset(), value.limit(), false));
    }

    public static IRubyObject newFromString(RubyString string)
    {
        ByteList b = string.getByteList();
        // TODO optimize
        //Buffer.wrap(b.unsafeBytes(), b.begin(), b.length());
        return Exec.getJRubyBridge().wrapJavaObject(
                Buffer.wrap(b.bytes()));
    }
}
