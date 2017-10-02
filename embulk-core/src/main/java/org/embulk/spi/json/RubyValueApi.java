package org.embulk.spi.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import com.google.common.base.Throwables;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.buffer.MessageBuffer;
import org.msgpack.value.Value;
import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jcodings.specific.ASCIIEncoding;

@Deprecated
public class RubyValueApi
{
    @Deprecated
    public static Value fromMessagePack(RubyString content)
    {
        if (!fromMessagePackDeprecationWarned) {
            System.err.println("[WARN] Plugin uses deprecated org.embulk.spi.json.RubyValueApi.fromMessagePack");
            System.err.println("[WARN] Report plugins in your config at: https://github.com/embulk/embulk/issues/802");
            // The |fromMessagePackDeprecationWarned| flag is used only for warning messages.
            // Even in case of race conditions, messages are just duplicated -- should be acceptable.
            fromMessagePackDeprecationWarned = true;
        }

        return fromMessagePackInternal(content);
    }

    private static Value fromMessagePackInternal(RubyString content)
    {
        ByteList list = content.getByteList();
        try {
            return MessagePack.newDefaultUnpacker(list.unsafeBytes(), list.begin(), list.length()).unpackValue();
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

    @Deprecated
    public static RubyString toMessagePack(Ruby runtime, Value value)
    {
        if (!toMessagePackDeprecationWarned) {
            System.err.println("[WARN] Plugin uses deprecated org.embulk.spi.json.RubyValueApi.toMessagePack");
            System.err.println("[WARN] Report plugins in your config at: https://github.com/embulk/embulk/issues/802");
            // The |toMessagePackDeprecationWarned| flag is used only for warning messages.
            // Even in case of race conditions, messages are just duplicated -- should be acceptable.
            toMessagePackDeprecationWarned = true;
        }

        try {
            MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
            packer.packValue(value);
            MessageBuffer mb = packer.toMessageBuffer();
            ByteList list = new ByteList(mb.array(), mb.arrayOffset(), mb.size(), ASCIIEncoding.INSTANCE, false);
            return RubyString.newString(runtime, list);
        }
        catch (IOException ex) {
            throw Throwables.propagate(ex);
        }
    }

    @Deprecated
    public static Value toValue(Ruby runtime, IRubyObject object)
    {
        if (!toValueDeprecationWarned) {
            System.err.println("[WARN] Plugin uses deprecated org.embulk.spi.json.RubyValueApi.toValue");
            System.err.println("[WARN] Report plugins in your config at: https://github.com/embulk/embulk/issues/802");
            // The |toValueDeprecationWarned| flag is used only for warning messages.
            // Even in case of race conditions, messages are just duplicated -- should be acceptable.
            toValueDeprecationWarned = true;
        }

        RubyString string = (RubyString) object.callMethod(runtime.getCurrentContext(), "to_msgpack");
        return fromMessagePackInternal(string);
    }

    private static boolean fromMessagePackDeprecationWarned = false;
    private static boolean toMessagePackDeprecationWarned = false;
    private static boolean toValueDeprecationWarned = false;

}
