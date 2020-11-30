package org.embulk.deps.preview;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import org.embulk.deps.EmbulkDependencyClassLoaders;
import org.embulk.spi.Page;
import org.embulk.spi.Schema;

public abstract class PreviewPrinter implements Closeable {
    public static final PreviewPrinter ofTable(final PrintStream out, final Schema schema) {
        final Object printer;
        try {
            printer = CREATOR_TABLE.invoke(null, out, schema);
        } catch (final IllegalAccessException | IllegalArgumentException ex) {
            throw new LinkageError("Dependencies for preview are not loaded correctly: " + CLASS_NAME, ex);
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

        if (printer == null) {
            throw new NullPointerException("PreviewPrinter.ofTable unexpectedly returned null.");
        }
        if (!(printer instanceof PreviewPrinter)) {
            throw new ClassCastException("PreviewPrinter.ofTable unexpectedly returned non-PreviewPrinter.");
        }
        return (PreviewPrinter) printer;
    }

    public static final PreviewPrinter ofVertical(final PrintStream out, final Schema schema) {
        final Object printer;
        try {
            printer = CREATOR_VERTICAL.invoke(null, out, schema);
        } catch (final IllegalAccessException | IllegalArgumentException ex) {
            throw new LinkageError("Dependencies for preview are not loaded correctly: " + CLASS_NAME, ex);
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

        if (printer == null) {
            throw new NullPointerException("PreviewPrinter.ofVertical unexpectedly returned null.");
        }
        if (!(printer instanceof PreviewPrinter)) {
            throw new ClassCastException("PreviewPrinter.ofVertical unexpectedly returned non-PreviewPrinter.");
        }
        return (PreviewPrinter) printer;
    }

    public abstract void printAllPages(List<Page> pages) throws IOException;

    @Override
    public abstract void close() throws IOException;

    public abstract void finish() throws IOException;

    @SuppressWarnings("unchecked")
    private static Class<PreviewPrinter> loadImplClass() {
        try {
            return (Class<PreviewPrinter>) CLASS_LOADER.loadClass(CLASS_NAME);
        } catch (final ClassNotFoundException ex) {
            throw new LinkageError("Dependencies for preview are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final ClassLoader CLASS_LOADER = EmbulkDependencyClassLoaders.get();
    private static final String CLASS_NAME = "org.embulk.deps.preview.PreviewPrinterImpl";

    static {
        final Class<PreviewPrinter> clazz = loadImplClass();
        try {
            CREATOR_TABLE = clazz.getMethod("ofTable", PrintStream.class, Schema.class);
            CREATOR_VERTICAL = clazz.getMethod("ofVertical", PrintStream.class, Schema.class);
        } catch (final NoSuchMethodException ex) {
            throw new LinkageError("Dependencies for preview are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final Method CREATOR_TABLE;
    private static final Method CREATOR_VERTICAL;
}
