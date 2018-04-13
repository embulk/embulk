package org.embulk.jruby;

/**
 * Indirects onto JRuby with initializing ScriptingContainer lazily.
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public final class LazyScriptingContainerDelegate extends ScriptingContainerDelegate {
    public LazyScriptingContainerDelegate(
            final ClassLoader classLoader,
            final LocalContextScope delegateLocalContextScope,
            final LocalVariableBehavior delegateLocalVariableBehavior,
            final JRubyInitializer initializer) {
        this.impl = null;

        this.classLoader = classLoader;
        this.delegateLocalContextScope = delegateLocalContextScope;
        this.delegateLocalVariableBehavior = delegateLocalVariableBehavior;
        this.initializer = initializer;
    }

    @Override
    public String getJRubyVersion() {
        return getInitialized().getJRubyVersion();
    }

    @Override
    public String getRubyVersion() {
        return getInitialized().getRubyVersion();
    }

    // It is intentionally package-private. It is just for logging from JRubyScriptingModule.
    @Override
    String getGemHome() throws JRubyInvalidRuntimeException {
        return getInitialized().getGemHome();
    }

    // It is intentionally package-private. It is just for logging from JRubyScriptingModule.
    @Override
    String getGemPathInString() throws JRubyInvalidRuntimeException {
        return getInitialized().getGemPathInString();
    }

    @Override
    public void clearGemPaths() throws JRubyInvalidRuntimeException {
        getInitialized().clearGemPaths();
    }

    @Override
    public void setGemPaths(final String gemPath) throws JRubyInvalidRuntimeException {
        getInitialized().setGemPaths(gemPath);
    }

    @Override
    public boolean isBundleGemfileDefined() throws JRubyInvalidRuntimeException {
        return getInitialized().isBundleGemfileDefined();
    }

    @Override
    public String getBundleGemfile() throws JRubyInvalidRuntimeException {
        return getInitialized().getBundleGemfile();
    }

    @Override
    public void setBundleGemfile(final String gemfilePath) throws JRubyInvalidRuntimeException {
        getInitialized().setBundleGemfile(gemfilePath);
    }

    @Override
    public void unsetBundleGemfile() throws JRubyInvalidRuntimeException {
        getInitialized().unsetBundleGemfile();
    }

    // It is intentionally private. It should return RubyObject while it is Object in the signature.
    @Override
    Object getGemPaths() throws JRubyInvalidRuntimeException {
        return getInitialized().getGemPaths();
    }

    @Override
    public void processJRubyOption(final String jrubyOption)
            throws JRubyInvalidRuntimeException, UnrecognizedJRubyOptionException, NotWorkingJRubyOptionException {
        getInitialized().processJRubyOption(jrubyOption);
    }

    @Override
    public Object callMethod(
            final Object receiver,
            final String methodName,
            final Object... args) throws JRubyInvalidRuntimeException {
        return getInitialized().callMethod(receiver, methodName, args);
    }

    /*
    @Override
    public Object callMethod(
            final Object receiver,
            final String methodName,
            final Block block,
            final Object... args) throws JRubyInvalidRuntimeException;
    */

    @Override
    public <T> T callMethod(
            final Object receiver,
            final String methodName,
            final Class<T> returnType) throws JRubyInvalidRuntimeException {
        return getInitialized().callMethod(receiver, methodName, returnType);
    }

    @Override
    public <T> T callMethod(
            final Object receiver,
            final String methodName,
            final Object singleArg,
            final Class<T> returnType) throws JRubyInvalidRuntimeException {
        return getInitialized().callMethod(receiver, methodName, singleArg, returnType);
    }

    /*
    @Override
    public <T> T callMethod(
            final Object receiver,
            final String methodName,
            final Object[] args,
            final Class<T> returnType) throws JRubyInvalidRuntimeException;
    */

    /*
    @Override
    public <T> T callMethod(
            final Object receiver,
            final String methodName,
            final Object[] args,
            final Block block,
            final Class<T> returnType) throws JRubyInvalidRuntimeException;
    */

    /*
    @Override
    public <T> T callMethod(
            final Object receiver,
            final String methodName,
            final Class<T> returnType,
            final EmbedEvalUnit unit) throws JRubyInvalidRuntimeException;
    */

    /*
    @Override
    public <T> T callMethod(
            final Object receiver,
            final String methodName,
            final Object[] args,
            final Class<T> returnType,
            final EmbedEvalUnit unit) throws JRubyInvalidRuntimeException;
    */

    /*
    @Override
    public <T> T callMethod(
            final Object receiver,
            final String methodName,
            final Object[] args,
            final Block block,
            final Class<T> returnType,
            final EmbedEvalUnit unit) throws JRubyInvalidRuntimeException;
    */

    // It is intentionally private. It should return LocalContextProvider while it is Object in the signature.
    @Override
    Object getProvider() throws JRubyInvalidRuntimeException {
        return getInitialized().getProvider();
    }

    @Override
    public Object put(final String key, final Object value) throws JRubyInvalidRuntimeException {
        return getInitialized().put(key, value);
    }

    @Override
    public Object remove(final String key) throws JRubyInvalidRuntimeException {
        return getInitialized().remove(key);
    }

    @Override
    public Object runScriptlet(final String script) throws JRubyInvalidRuntimeException {
        return getInitialized().runScriptlet(script);
    }

    // It is intentionally private. It should return RubyInstanceConfig while it is Object in the signature.
    @Override
    Object getRubyInstanceConfig() throws JRubyInvalidRuntimeException {
        return getInitialized().getRubyInstanceConfig();
    }

    // It is intentionally private. It should return Runtime while it is Object in the signature.
    @Override
    Object getRuntime() throws JRubyInvalidRuntimeException {
        return getInitialized().getRuntime();
    }

    // TODO: Remove this method finally. https://github.com/embulk/embulk/issues/1007
    // It is intentionally package-private. It should return ScriptingContainer while it is Object in the signature.
    @Override
    Object getScriptingContainer() throws JRubyNotLoadedException {
        return getInitialized().getScriptingContainer();
    }

    synchronized ScriptingContainerDelegateImpl getInitialized() {
        if (this.impl == null) {
            this.impl = ScriptingContainerDelegateImpl.create(
                    this.classLoader, this.delegateLocalContextScope, this.delegateLocalVariableBehavior);
            if (this.initializer != null) {
                this.initializer.initialize(this.impl);
            }
        }
        return this.impl;
    }

    private ScriptingContainerDelegateImpl impl;

    private final ClassLoader classLoader;
    private final LocalContextScope delegateLocalContextScope;
    private final LocalVariableBehavior delegateLocalVariableBehavior;
    private final JRubyInitializer initializer;
}
