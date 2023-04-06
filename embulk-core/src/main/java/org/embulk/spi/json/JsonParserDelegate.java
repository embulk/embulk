package org.embulk.spi.json;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.embulk.EmbulkDependencyClassLoader;
import org.msgpack.value.Value;

public abstract class JsonParserDelegate {
    public static JsonParserDelegate of() {
        try {
            return CONSTRUCTOR.newInstance();
        } catch (final IllegalAccessException | IllegalArgumentException | InstantiationException ex) {
            throw new LinkageError("Dependencies for JSON are not loaded correctly: " + CLASS_NAME, ex);
        } catch (final InvocationTargetException ex) {
            final Throwable targetException = ex.getTargetException();
            if (targetException instanceof RuntimeException) {
                throw (RuntimeException) targetException;
            } else if (targetException instanceof Error) {
                throw (Error) targetException;
            } else {
                throw new RuntimeException("Unexpected Exception in creating: " + CLASS_NAME, ex);
            }
        }
    }

    @SuppressWarnings("deprecation")  // For use of org.embulk.spi.json.JsonParser
    public abstract org.embulk.spi.json.JsonParser.Stream open(final InputStream in) throws IOException;

    @SuppressWarnings("deprecation")  // For use of org.embulk.spi.json.JsonParser
    public abstract org.embulk.spi.json.JsonParser.Stream openWithOffsetInJsonPointer(
            final InputStream in, final String offsetInJsonPointer) throws IOException;

    public abstract Value parse(final String json);

    public abstract Value parseWithOffsetInJsonPointer(final String json, final String offsetInJsonPointer);

    @SuppressWarnings("unchecked")
    private static Class<JsonParserDelegate> loadImplClass() {
        try {
            return (Class<JsonParserDelegate>) CLASS_LOADER.loadClass(CLASS_NAME);
        } catch (final ClassNotFoundException ex) {
            throw new LinkageError("Dependencies for JSON are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final ClassLoader CLASS_LOADER = EmbulkDependencyClassLoader.get();
    private static final String CLASS_NAME = "org.embulk.deps.json.JsonParserDelegateImpl";

    static {
        final Class<JsonParserDelegate> clazz = loadImplClass();
        try {
            CONSTRUCTOR = clazz.getConstructor();
        } catch (final NoSuchMethodException ex) {
            throw new LinkageError("Dependencies for JSON are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final Constructor<JsonParserDelegate> CONSTRUCTOR;
}
