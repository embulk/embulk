package org.embulk.jruby;

import com.google.inject.Injector;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.embulk.EmbulkSystemProperties;
import org.slf4j.Logger;

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

    public static LazyScriptingContainerDelegate withGems(
            final Logger logger,
            final EmbulkSystemProperties embulkSystemProperties) {
        return of(false, true, null, logger, embulkSystemProperties);
    }

    public static LazyScriptingContainerDelegate withGemsIgnored(
            final Logger logger,
            final EmbulkSystemProperties embulkSystemProperties) {
        return of(false, false, null, logger, embulkSystemProperties);
    }

    public static LazyScriptingContainerDelegate withInjector(
            final Injector injector,
            final Logger logger,
            final EmbulkSystemProperties embulkSystemProperties) {
        return of(true, true, injector, logger, embulkSystemProperties);
    }

    private static LazyScriptingContainerDelegate of(
            final boolean isEmbulkSpecific,
            final boolean initializesGem,
            final Injector injector,
            final Logger logger,
            final EmbulkSystemProperties embulkSystemProperties) {
        // use_global_ruby_runtime is valid only when it's guaranteed that just one Injector is
        // instantiated in this JVM.
        final boolean useGlobalRubyRuntime = embulkSystemProperties.getPropertyAsBoolean("use_global_ruby_runtime", false);

        final String jrubyProperty = embulkSystemProperties.getProperty("jruby");

        final ArrayList<URL> jrubyUrlsBuilt = new ArrayList<>();
        if (jrubyProperty != null && !jrubyProperty.isEmpty()) {
            // File.pathSeparator is not available here because the property "jruby" expects a URL-like style:
            // "file:" and "mvn:". It now supports only "file:", though.
            //
            // Semicolons basically do not appear in URLs without quoting except for the "optional fields and values"
            // case described in RFC 1738. We don't need to take care of the "optional fields and values" case here.
            // https://tools.ietf.org/html/rfc1738
            for (final String jarLocator : jrubyProperty.split("\\;")) {
                if (jarLocator.startsWith("file:")) {
                    // TODO: Validate the path more.
                    final URL jarUrl;
                    try {
                        jarUrl = new URL(jarLocator);
                    } catch (final MalformedURLException ex) {
                        throw new JRubyInvalidRuntimeException("Embulk system property \"jruby\" is invalid: " + jrubyProperty, ex);
                    }
                    jrubyUrlsBuilt.add(jarUrl);
                } else {
                    throw new JRubyInvalidRuntimeException("Embulk system property \"jruby\" is invalid: " + jrubyProperty);
                }
            }
        }
        final List<URL> jrubyUrls = Collections.unmodifiableList(jrubyUrlsBuilt);

        final JRubyInitializer initializer = JRubyInitializer.of(
                isEmbulkSpecific,
                initializesGem,
                isEmbulkSpecific ? injector : null,
                logger,
                embulkSystemProperties);

        final JRubyClassLoader jrubyClassLoader;
        try {
            jrubyClassLoader = new JRubyClassLoader(jrubyUrls, LazyScriptingContainerDelegate.class.getClassLoader());
        } catch (final RuntimeException ex) {
            return null;
        }

        try {
            jrubyClassLoader.loadClass("org.jruby.Main");
        } catch (final ClassNotFoundException ex) {
            return null;
        }

        try {
            final LazyScriptingContainerDelegate jruby = new LazyScriptingContainerDelegate(
                    jrubyClassLoader,
                    useGlobalRubyRuntime
                            ? ScriptingContainerDelegate.LocalContextScope.SINGLETON
                            : ScriptingContainerDelegate.LocalContextScope.SINGLETHREAD,
                    ScriptingContainerDelegate.LocalVariableBehavior.PERSISTENT,
                    initializer);
            if (useGlobalRubyRuntime) {
                // In case the global JRuby instance is used, the instance should be always initialized.
                // Ruby tests (in embulk-ruby/) are examples.
                jruby.getInitialized();
            }
            return jruby;
        } catch (Exception ex) {
            return null;
        }
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
    public void setGemPaths(final String gemHome) throws JRubyInvalidRuntimeException {
        getInitialized().setGemPaths(gemHome);
    }

    @Override
    public void setGemPaths(final String gemHome, final String gemPath) throws JRubyInvalidRuntimeException {
        getInitialized().setGemPaths(gemHome, gemPath);
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
    public Object callMethodArray(
            final Object receiver,
            final String methodName,
            final Object[] args) throws JRubyInvalidRuntimeException {
        return getInitialized().callMethodArray(receiver, methodName, args);
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
