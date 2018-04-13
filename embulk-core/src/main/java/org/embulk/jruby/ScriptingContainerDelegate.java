package org.embulk.jruby;

/**
 * Indirects onto JRuby not to require JRuby classes directly.
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public abstract class ScriptingContainerDelegate {
    public enum LocalContextScope {
        CONCURRENT,
        SINGLETHREAD,
        SINGLETON,
        THREADSAFE,
        ;
    }

    public enum LocalVariableBehavior {
        BSF,
        GLOBAL,
        PERSISTENT,
        TRANSIENT,
        ;
    }

    public static final class UnrecognizedJRubyOptionException extends Exception {}

    public static final class NotWorkingJRubyOptionException extends Exception {
        public NotWorkingJRubyOptionException() {
            super();
        }

        public NotWorkingJRubyOptionException(final Throwable cause) {
            super(cause);
        }
    }

    public abstract String getJRubyVersion();

    public abstract String getRubyVersion();

    // It is intentionally package-private. It is just for logging from JRubyScriptingModule.
    abstract String getGemHome() throws JRubyNotLoadedException;

    // It is intentionally package-private. It is just for logging from JRubyScriptingModule.
    abstract String getGemPathInString() throws JRubyNotLoadedException;

    public abstract void clearGemPaths() throws JRubyNotLoadedException;

    public abstract void setGemPaths(final String gemPath) throws JRubyNotLoadedException;

    public abstract boolean isBundleGemfileDefined() throws JRubyNotLoadedException;

    public abstract String getBundleGemfile() throws JRubyNotLoadedException;

    public abstract void setBundleGemfile(final String gemfilePath) throws JRubyNotLoadedException;

    public abstract void unsetBundleGemfile() throws JRubyNotLoadedException;

    // It is intentionally private. It should return RubyObject while it is Object in the signature.
    abstract Object getGemPaths() throws JRubyNotLoadedException;

    public abstract void processJRubyOption(final String jrubyOption)
            throws JRubyNotLoadedException, UnrecognizedJRubyOptionException, NotWorkingJRubyOptionException;

    public abstract Object callMethod(
            final Object receiver,
            final String methodName,
            final Object... args) throws JRubyNotLoadedException;

    /*
    public abstract Object callMethod(
            final Object receiver,
            final String methodName,
            final Block block,
            final Object... args) throws JRubyNotLoadedException;
    */

    public abstract <T> T callMethod(
            final Object receiver,
            final String methodName,
            final Class<T> returnType) throws JRubyNotLoadedException;

    public abstract <T> T callMethod(
            final Object receiver,
            final String methodName,
            final Object singleArg,
            final Class<T> returnType) throws JRubyNotLoadedException;

    /*
    public abstract <T> T callMethod(
            final Object receiver,
            final String methodName,
            final Object[] args,
            final Class<T> returnType) throws JRubyNotLoadedException;
    */

    /*
    public abstract <T> T callMethod(
            final Object receiver,
            final String methodName,
            final Object[] args,
            final Block block,
            final Class<T> returnType) throws JRubyNotLoadedException;
    */

    /*
    public abstract <T> T callMethod(
            final Object receiver,
            final String methodName,
            final Class<T> returnType,
            final EmbedEvalUnit unit) throws JRubyNotLoadedException;
    */

    /*
    public abstract <T> T callMethod(
            final Object receiver,
            final String methodName,
            final Object[] args,
            final Class<T> returnType,
            final EmbedEvalUnit unit) throws JRubyNotLoadedException;
    */

    /*
    public abstract <T> T callMethod(
            final Object receiver,
            final String methodName,
            final Object[] args,
            final Block block,
            final Class<T> returnType,
            final EmbedEvalUnit unit) throws JRubyNotLoadedException;
    */

    // It is intentionally private. It should return LocalContextProvider while it is Object in the signature.
    abstract Object getProvider() throws JRubyNotLoadedException;

    public abstract Object put(final String key, final Object value) throws JRubyNotLoadedException;

    public abstract Object remove(final String key) throws JRubyNotLoadedException;

    public abstract Object runScriptlet(final String script) throws JRubyNotLoadedException;

    // It is intentionally private. It should return RubyInstanceConfig while it is Object in the signature.
    abstract Object getRubyInstanceConfig() throws JRubyNotLoadedException;

    // It is intentionally private. It should return Runtime while it is Object in the signature.
    abstract Object getRuntime() throws JRubyNotLoadedException;

    // TODO: Remove this method finally. https://github.com/embulk/embulk/issues/1007
    abstract Object getScriptingContainer() throws JRubyNotLoadedException;
}
