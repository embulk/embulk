package org.embulk.spi.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import com.google.common.base.Throwables;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.value.Value;
import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.util.ByteList;
import org.jcodings.specific.ASCIIEncoding;

public class RubyValueApi
{
    private static final MessagePack msgpack = new MessagePack();

    public static Value fromMessagePack(RubyString content)
    {
        ByteList list = content.getByteList();
        try {
            return msgpack.newDefaultUnpacker(list.unsafeBytes(), list.begin(), list.length()).unpackValue();
        }
        catch (IOException ex) {
            throw Throwables.propagate(ex);
        }
    }

    private static class OpenByteArrayOutputStream
            extends ByteArrayOutputStream
    {
        public byte[] getBuffer()
        {
            return buf;
        }

        public int getCount()
        {
            return count;
        }
    }

    public static RubyString toMessagePack(Ruby runtime, Value value)
    {
        try {
            OpenByteArrayOutputStream out = new OpenByteArrayOutputStream();  // TODO optimize msgpack-core to reduce number of copy
            MessagePacker packer = msgpack.newDefaultPacker(out);
            value.writeTo(packer);
            packer.flush();
            ByteList list = new ByteList(out.getBuffer(), 0, out.getCount(), ASCIIEncoding.INSTANCE, false);
            return RubyString.newString(runtime, list);
        }
        catch (IOException ex) {
            throw Throwables.propagate(ex);
        }
    }
}
