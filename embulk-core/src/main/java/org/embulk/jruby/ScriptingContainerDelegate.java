package org.embulk.jruby;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * ScriptingContainerDelegate is an indirection layer onto JRuby not to require JRuby classes directly.
 *
 * It calls methods of ScriptingContainer through reflection.
 *
 * This trick enables Embulk:
 * <ul>
 * <li>To load a JRuby runtime (jruby-complete-*.jar) specified at runtime out of embulk-*.jar.
 * <li>To switch versions of JRuby without releasing or repackaging.
 * <li>To distribute Embulk without JRuby runtimes embedded.
 * </ul>
 */
public final class ScriptingContainerDelegate {
    private ScriptingContainerDelegate(final String jrubyVersion,
                                       final String rubyVersion,

                                       final Object scriptingContainer,
                                       final Method method_callMethod_ALObject,
                                       final Method method_callMethod_LClass,
                                       final Method method_callMethod_LObject_LClass,
                                       final Method method_getProvider,
                                       final Method method_put_LString_LObject,
                                       final Method method_remove_LString,
                                       final Method method_runScriptlet_LString,

                                       final Class<?> class_RubyObject,

                                       final Method method_getRubyInstanceConfig,

                                       final Field field_COMPILE_INVOKEDYNAMIC,
                                       final Method method_force_LString,

                                       final Object const_CompileMode_OFF,
                                       final Method method_setCompileMode_LCompileMode,

                                       final Method method_getRuntime) {
        this.jrubyVersion = jrubyVersion;
        this.rubyVersion = rubyVersion;

        this.scriptingContainer = scriptingContainer;
        this.method_callMethod_ALObject = method_callMethod_ALObject;
        this.method_callMethod_LClass = method_callMethod_LClass;
        this.method_callMethod_LObject_LClass = method_callMethod_LObject_LClass;
        this.method_getProvider = method_getProvider;
        this.method_put_LString_LObject = method_put_LString_LObject;
        this.method_remove_LString = method_remove_LString;
        this.method_runScriptlet_LString = method_runScriptlet_LString;

        this.class_RubyObject = class_RubyObject;

        this.method_getRubyInstanceConfig = method_getRubyInstanceConfig;

        this.field_COMPILE_INVOKEDYNAMIC = field_COMPILE_INVOKEDYNAMIC;
        this.method_force_LString = method_force_LString;

        this.const_CompileMode_OFF = const_CompileMode_OFF;
        this.method_setCompileMode_LCompileMode = method_setCompileMode_LCompileMode;

        this.method_getRuntime = method_getRuntime;
    }

    private ScriptingContainerDelegate(final String jrubyVersion,
                                       final String rubyVersion,

                                       final Object scriptingContainer,
                                       final Method method_callMethod_ALObject,
                                       final Method method_callMethod_LClass,
                                       final Method method_callMethod_LObject_LClass,
                                       final Method method_getProvider,
                                       final Method method_put_LString_LObject,
                                       final Method method_remove_LString,
                                       final Method method_runScriptlet_LString,

                                       final Class<?> class_RubyObject) {
        this(jrubyVersion,
             rubyVersion,
             scriptingContainer,
             method_callMethod_ALObject,
             method_callMethod_LClass,
             method_callMethod_LObject_LClass,
             method_getProvider,
             method_put_LString_LObject,
             method_remove_LString,
             method_runScriptlet_LString,
             class_RubyObject,
             null,
             null,
             null,
             null,
             null,
             null);
    }

    private ScriptingContainerDelegate() {
        this(null,
             null,
             null,
             null,
             null,
             null,
             null,
             null,
             null,
             null,
             null);
    }

    public static ScriptingContainerDelegate create(final ClassLoader classLoader,
                                                    final LocalContextScope delegateLocalContextScope,
                                                    final LocalVariableBehavior delegateLocalVariableBehavior) {
        final Object scriptingContainer = createScriptingContainer(
            classLoader, delegateLocalContextScope, delegateLocalVariableBehavior);

        if (scriptingContainer == null) {
            return new ScriptingContainerDelegate();
        }

        final Method method_callMethod_ALObject;
        final Method method_callMethod_LObject_LClass;
        final Method method_callMethod_LClass;
        final Method method_getProvider;
        final Method method_put_LString_LObject;
        final Method method_remove_LString;
        final Method method_runScriptlet_LString;
        try {
            method_callMethod_ALObject = scriptingContainer.getClass().getMethod(
                "callMethod", Object.class, String.class, Object[].class);
            method_callMethod_LObject_LClass = scriptingContainer.getClass().getMethod(
                "callMethod", Object.class, String.class, Object.class, Class.class);
            method_callMethod_LClass = scriptingContainer.getClass().getMethod(
                "callMethod", Object.class, String.class, Class.class);
            method_getProvider = scriptingContainer.getClass().getMethod(
                "getProvider");
            method_put_LString_LObject = scriptingContainer.getClass().getMethod(
                "put", String.class, Object.class);
            method_remove_LString = scriptingContainer.getClass().getMethod(
                "remove", String.class);
            method_runScriptlet_LString = scriptingContainer.getClass().getMethod(
                "runScriptlet", String.class);
        } catch (NoSuchMethodException ex) {
            // Required methods are not implemented in ScriptingContainer of the JRuby version.
            return new ScriptingContainerDelegate();  // TODO: Log.
        }

        final Class<?> class_RubyObject;
        try {
            class_RubyObject = classLoader.loadClass("org.jruby.RubyObject");
        } catch (ClassNotFoundException ex) {
            // `org.jruby.RubyObject` is not implemented in the JRuby version -- unlikely.
            return new ScriptingContainerDelegate();  // TODO: Log.
        }

        final Class<?> class_Constants;
        try {
            class_Constants = classLoader.loadClass("org.jruby.runtime.Constants");
        } catch (ClassNotFoundException ex) {
            // `org.jruby.runtime.Constants` is not implemented in the JRuby version -- unlikely.
            return new ScriptingContainerDelegate();  // TODO: Log.
        }
        final String jrubyVersion = (String)getStaticField_Constants(class_Constants, "VERSION");
        final String rubyVersion = (String)getStaticField_Constants(class_Constants, "RUBY_VERSION");

        final Method method_getRubyInstanceConfig;
        final Method method_getRuntime;
        try {
            final Class<?> class_LocalContextProvider =
                classLoader.loadClass("org.jruby.embed.internal.LocalContextProvider");
            method_getRubyInstanceConfig = class_LocalContextProvider.getMethod("getRubyInstanceConfig");
            method_getRuntime = class_LocalContextProvider.getMethod("getRuntime");
        } catch (ClassNotFoundException | NoSuchMethodException ex) {
            // `org.jruby.embed.internal.LocalContextProvider` nor its required methods are not implemented.
            // It unlikely happens, but it is acceptable.
            return new ScriptingContainerDelegate(
                jrubyVersion,
                rubyVersion,

                scriptingContainer,
                method_callMethod_ALObject,
                method_callMethod_LClass,
                method_callMethod_LObject_LClass,
                method_getProvider,
                method_put_LString_LObject,
                method_remove_LString,
                method_runScriptlet_LString,

                class_RubyObject);
        }

        final Field field_COMPILE_INVOKEDYNAMIC = createField_Options(classLoader, "COMPILE_INVOKEDYNAMIC");
        final Method method_force_LString = createMethod_force_LString(classLoader);

        final Object const_CompileMode_OFF = createCompileMode(classLoader, "OFF");
        final Method method_setCompileMode_LCompileMode = createMethod_setCompileMode_LCompileMode(
            classLoader, const_CompileMode_OFF.getClass());

        return new ScriptingContainerDelegate(
            jrubyVersion,
            rubyVersion,

            scriptingContainer,
            method_callMethod_ALObject,
            method_callMethod_LClass,
            method_callMethod_LObject_LClass,
            method_getProvider,
            method_put_LString_LObject,
            method_remove_LString,
            method_runScriptlet_LString,

            class_RubyObject,

            method_getRubyInstanceConfig,

            field_COMPILE_INVOKEDYNAMIC,
            method_force_LString,

            const_CompileMode_OFF,
            method_setCompileMode_LCompileMode,

            method_getRuntime);
    }

    private static Object createScriptingContainer(final ClassLoader classLoader,
                                                   final LocalContextScope delegateLocalContextScope,
                                                   final LocalVariableBehavior delegateLocalVariableBehavior) {
        final Object object_LocalContextScope = createLocalContextScope(classLoader, delegateLocalContextScope);
        if (object_LocalContextScope == null) {
            return null;
        }

        final Object object_LocalVariableBehavior = createLocalVariableBehavior(classLoader, delegateLocalVariableBehavior);
        if (object_LocalVariableBehavior == null) {
            return null;
        }

        final Class<?> clazz;
        try {
            clazz = classLoader.loadClass("org.jruby.embed.ScriptingContainer");
        } catch (ClassNotFoundException ex) {
            return null;  // TODO: Log.
        }

        final Constructor<?> constructor;
        try {
            constructor = clazz.getConstructor(object_LocalContextScope.getClass(),
                                               object_LocalVariableBehavior.getClass());
        } catch (NoSuchMethodException ex) {
            return null;  // TODO: Log.
        }

        try {
            return constructor.newInstance(object_LocalContextScope, object_LocalVariableBehavior);
        } catch (ReflectiveOperationException ex) {
            return null;  // TODO: Log.
        }
    }

    private static Object createLocalContextScope(final ClassLoader classLoader,
                                                  final LocalContextScope delegateLocalContextScope) {
        final Class<?> clazz;
        try {
            clazz = classLoader.loadClass("org.jruby.embed.LocalContextScope");
        } catch (ClassNotFoundException ex) {
            return null;  // TODO: Log.
        }

        final Method valueOf;
        try {
            valueOf = clazz.getMethod("valueOf", String.class);
        } catch (NoSuchMethodException ex) {
            return null;  // TODO: Log.
        }

        try {
            return valueOf.invoke(null, delegateLocalContextScope.toString());
        } catch (ReflectiveOperationException ex) {
            return null;  // TODO: Log.
        }
    }

    private static Object createLocalVariableBehavior(final ClassLoader classLoader,
                                                      final LocalVariableBehavior delegateLocalVariableBehavior) {
        final Class<?> clazz;
        try {
            clazz = classLoader.loadClass("org.jruby.embed.LocalVariableBehavior");
        } catch (ClassNotFoundException ex) {
            return null;  // TODO: Log.
        }

        final Method valueOf;
        try {
            valueOf = clazz.getMethod("valueOf", String.class);
        } catch (NoSuchMethodException ex) {
            return null;  // TODO: Log.

        }

        try {
            return valueOf.invoke(null, delegateLocalVariableBehavior.toString());
        } catch (ReflectiveOperationException ex) {
            return null;  // TODO: Log.
        }
    }

    private static Object createCompileMode(final ClassLoader classLoader, final String name) {
        final Class<?> clazz;
        try {
            clazz = classLoader.loadClass("org.jruby.RubyInstanceConfig$CompileMode");
        } catch (ClassNotFoundException ex) {
            return null;  // TODO: Log.
        }

        final Method valueOf;
        try {
            valueOf = clazz.getMethod("valueOf", String.class);
        } catch (NoSuchMethodException ex) {
            return null;  // TODO: Log.
        }

        try {
            return valueOf.invoke(null, name);
        } catch (ReflectiveOperationException ex) {
            return null;  // TODO: Log.
        }
    }

    private static Method createMethod_setCompileMode_LCompileMode(final ClassLoader classLoader,
                                                                   final Class<?> clazz_CompileMode) {
        final Class<?> clazz;
        try {
            clazz = classLoader.loadClass("org.jruby.RubyInstanceConfig");
        } catch (ClassNotFoundException ex) {
            return null;  // TODO: Log.
        }

        try {
            return clazz.getMethod("setCompileMode", clazz_CompileMode);
        } catch (NoSuchMethodException ex) {
            return null;  // TODO: Log.
        }
    }

    private static Field createField_Options(final ClassLoader classLoader, final String fieldName) {
        final Class<?> clazz;
        try {
            clazz = classLoader.loadClass("org.jruby.util.cli.Options");
        } catch (ClassNotFoundException ex) {
            return null;  // TODO: Log.
        }

        try {
            return clazz.getField(fieldName);
        } catch (NoSuchFieldException ex) {
            return null;  // TODO: Log.
        }
    }

    private static Method createMethod_force_LString(final ClassLoader classLoader) {
        final Class<?> clazz;
        try {
            clazz = classLoader.loadClass("com.headius.options.Option");
        } catch (ClassNotFoundException ex) {
            return null;  // TODO: Log.
        }

        try {
            return clazz.getMethod("force", String.class);
        } catch (NoSuchMethodException ex) {
            return null;  // TODO: Log.
        }
    }

    private static Object getStaticField_Constants(final Class<?> class_Constants, final String fieldName) {
        final Field field;
        try {
            field = class_Constants.getField(fieldName);
        } catch (NoSuchFieldException ex) {
            return null;  // TODO: Log.
        }

        try {
            return field.get(null);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            return null;  // TODO: Log.
        }
    }

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
        public NotWorkingJRubyOptionException() { super(); }
        public NotWorkingJRubyOptionException(final Throwable cause) { super(cause); }
    }

    public static String getJRubyVersion(final ClassLoader classLoader) {
        final Class<?> class_Constants;
        try {
            class_Constants = classLoader.loadClass("org.jruby.runtime.Constants");
        } catch (ClassNotFoundException ex) {
            // `org.jruby.runtime.Constants` is not implemented in the JRuby version -- unlikely.
            return null;
        }
        return (String)getStaticField_Constants(class_Constants, "VERSION");
    }

    public String getJRubyVersion() {
        return this.jrubyVersion;
    }

    public String getRubyVersion() {
        return this.rubyVersion;
    }

    // It is intentionally package-private. It is just for logging from JRubyScriptingModule.
    String getGemHome() throws JRubyNotLoadedException {
        return this.callMethod(this.getGemPaths(), "home", String.class);
    }

    // It is intentionally package-private. It is just for logging from JRubyScriptingModule.
    String getGemPathInString() throws JRubyNotLoadedException {
        final List gemPath = this.callMethod(this.getGemPaths(), "path", List.class);
        return gemPath.toString();
    }

    public void clearGemPaths() throws JRubyNotLoadedException {
        this.callMethod(this.runScriptlet("Gem"), "use_paths", (Object)null, (Object)null);
    }

    public void setGemPaths(final String gemPath) throws JRubyNotLoadedException {
        this.callMethod(this.runScriptlet("Gem"), "use_paths", gemPath, gemPath);
    }

    public boolean isBundleGemfileDefined() throws JRubyNotLoadedException {
        return this.callMethod(this.runScriptlet("ENV"), "has_key?", "BUNDLE_GEMFILE", Boolean.class);
    }

    public String getBundleGemfile() throws JRubyNotLoadedException {
        return this.callMethod(this.runScriptlet("ENV"), "fetch", "BUNDLE_GEMFILE", String.class);
    }

    public void setBundleGemfile(final String gemfilePath) throws JRubyNotLoadedException {
        this.callMethod(this.runScriptlet("ENV"), "store", "BUNDLE_GEMFILE", gemfilePath);
    }

    public void unsetBundleGemfile() throws JRubyNotLoadedException {
        this.callMethod(this.runScriptlet("ENV"), "delete", "BUNDLE_GEMFILE");
    }

    // It is intentionally private. It should return RubyObject while it is Object in the signature.
    private Object getGemPaths() throws JRubyNotLoadedException {
        return this.callMethod(this.runScriptlet("Gem"), "paths", this.class_RubyObject);
    }

    public void processJRubyOption(final String jrubyOption)
            throws JRubyNotLoadedException, UnrecognizedJRubyOptionException, NotWorkingJRubyOptionException {
        final Object rubyInstanceConfig = this.getRubyInstanceConfig();

        if (jrubyOption.charAt(0) != '-') {
            throw new UnrecognizedJRubyOptionException();
        }

        for (int index = 1; index < jrubyOption.length(); ++index) {
            switch (jrubyOption.charAt(index)) {
            case '-':
                if (jrubyOption.equals("--dev")) {
                    // They are not all of "--dev", but they are most possible configurations after JVM boot.
                    try {
                        if (this.method_force_LString != null && this.field_COMPILE_INVOKEDYNAMIC != null) {
                            // NOTE: Options is global.
                            this.method_force_LString.invoke(this.field_COMPILE_INVOKEDYNAMIC.get(null), "false");
                        }
                        if (this.method_setCompileMode_LCompileMode != null && this.const_CompileMode_OFF != null) {
                            this.method_setCompileMode_LCompileMode.invoke(
                                rubyInstanceConfig, this.const_CompileMode_OFF);
                        }
                    }
                    catch (IllegalAccessException | InvocationTargetException ex) {
                        throw new NotWorkingJRubyOptionException(ex);
                    }
                    return;
                }
                else if (jrubyOption.equals("--client")) {
                    throw new NotWorkingJRubyOptionException();
                }
                else if (jrubyOption.equals("--server")) {
                    throw new NotWorkingJRubyOptionException();
                }
                throw new UnrecognizedJRubyOptionException();
            default:
                throw new UnrecognizedJRubyOptionException();
            }
        }
    }

    public Object callMethod(final Object receiver,
                             final String methodName,
                             final Object... args) throws JRubyNotLoadedException {
        if (this.scriptingContainer == null || this.method_callMethod_ALObject == null) {
            throw new JRubyNotLoadedException();
        }
        try {
            return this.method_callMethod_ALObject.invoke(this.scriptingContainer, receiver, methodName, args);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new JRubyNotLoadedException(ex);
        }
    }

    /*
    public Object callMethod(final Object receiver,
                             final String methodName,
                             final Block block,
                             final Object... args) {
    }
    */

    public <T> T callMethod(final Object receiver,
                            final String methodName,
                            final Class<T> returnType) throws JRubyNotLoadedException {
        if (this.scriptingContainer == null || this.method_callMethod_LClass == null) {
            throw new JRubyNotLoadedException();
        }
        try {
            @SuppressWarnings("unchecked")
            final T returnValue = (T)(this.method_callMethod_LClass.invoke(
                                          this.scriptingContainer, receiver, methodName, returnType));
            return returnValue;
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new JRubyNotLoadedException(ex);
        } catch (RuntimeException ex) {
            throw wrapJRubyInvokeFailedException(ex);
        }
    }

    public <T> T callMethod(final Object receiver,
                            final String methodName,
                            final Object singleArg,
                            final Class<T> returnType) throws JRubyNotLoadedException {
        if (this.scriptingContainer == null || this.method_callMethod_LObject_LClass == null) {
            throw new JRubyNotLoadedException();
        }
        try {
            @SuppressWarnings("unchecked")
            final T returnValue = (T)(this.method_callMethod_LObject_LClass.invoke(
                                          this.scriptingContainer, receiver, methodName, singleArg, returnType));
            return returnValue;
        }
        catch (IllegalAccessException | InvocationTargetException ex) {
            throw new JRubyNotLoadedException(ex);
        } catch (RuntimeException ex) {
            throw wrapJRubyInvokeFailedException(ex);
        }
    }

    /*
    public <T> T callMethod(final Object receiver,
                            final String methodName,
                            final Object[] args,
                            final Class<T> returnType) {
    }
    */

    /*
    public <T> T callMethod(final Object receiver,
                            final String methodName,
                            final Object[] args,
                            final Block block,
                            final Class<T> returnType) {
    }
    */

    /*
    public <T> T callMethod(final Object receiver,
                            final String methodName,
                            final Class<T> returnType,
                            final EmbedEvalUnit unit) {
    }
    */

    /*
    public <T> T callMethod(final Object receiver,
                            final String methodName,
                            final Object[] args,
                            final Class<T> returnType,
                            final EmbedEvalUnit unit) {
    }
    */

    /*
    public <T> T callMethod(final Object receiver,
                            final String methodName,
                            final Object[] args,
                            final Block block,
                            final Class<T> returnType,
                            final EmbedEvalUnit unit) {
    }
    */

    // It is intentionally private. It should return LocalContextProvider while it is Object in the signature.
    private Object getProvider() throws JRubyNotLoadedException {
        if (this.scriptingContainer == null || this.method_getProvider == null) {
            throw new JRubyNotLoadedException();
        }
        try {
            return this.method_getProvider.invoke(this.scriptingContainer);
        }
        catch (IllegalAccessException | InvocationTargetException ex) {
            throw new JRubyNotLoadedException(ex);
        } catch (RuntimeException ex) {
            throw wrapJRubyInvokeFailedException(ex);
        }
    }

    public Object put(final String key, final Object value) throws JRubyNotLoadedException {
        if (this.scriptingContainer == null || this.method_put_LString_LObject == null) {
            throw new JRubyNotLoadedException();
        }
        try {
            return this.method_put_LString_LObject.invoke(this.scriptingContainer, key, value);
        }
        catch (IllegalAccessException | InvocationTargetException ex) {
            throw new JRubyNotLoadedException(ex);
        } catch (RuntimeException ex) {
            throw wrapJRubyInvokeFailedException(ex);
        }
    }

    public Object remove(final String key) throws JRubyNotLoadedException {
        if (this.scriptingContainer == null || this.method_remove_LString == null) {
            throw new JRubyNotLoadedException();
        }
        try {
            return this.method_remove_LString.invoke(this.scriptingContainer, key);
        }
        catch (IllegalAccessException | InvocationTargetException ex) {
            throw new JRubyNotLoadedException(ex);
        } catch (RuntimeException ex) {
            throw wrapJRubyInvokeFailedException(ex);
        }
    }

    public Object runScriptlet(final String script) throws JRubyNotLoadedException {
        if (this.scriptingContainer == null || this.method_runScriptlet_LString == null) {
            throw new JRubyNotLoadedException();
        }
        try {
            return this.method_runScriptlet_LString.invoke(this.scriptingContainer, script);
        }
        catch (IllegalAccessException | InvocationTargetException ex) {
            throw new JRubyNotLoadedException(ex);
        } catch (RuntimeException ex) {
            throw wrapJRubyInvokeFailedException(ex);
        }
    }

    // It is intentionally private. It should return RubyInstanceConfig while it is Object in the signature.
    private Object getRubyInstanceConfig() throws JRubyNotLoadedException {
        final Object provider = this.getProvider();
        if (provider == null || this.method_runScriptlet_LString == null) {
            throw new JRubyNotLoadedException();
        }
        try {
            return this.method_getRubyInstanceConfig.invoke(provider);
        }
        catch (IllegalAccessException | InvocationTargetException ex) {
            throw new JRubyNotLoadedException(ex);
        } catch (RuntimeException ex) {
            throw wrapJRubyInvokeFailedException(ex);
        }
    }

    // It is intentionally private. It should return Runtime while it is Object in the signature.
    private Object getRuntime() throws JRubyNotLoadedException {
        final Object provider = this.getProvider();
        if (provider == null || this.method_getRuntime == null) {
            throw new JRubyNotLoadedException();
        }
        try {
            return this.method_getRuntime.invoke(provider);
        }
        catch (IllegalAccessException | InvocationTargetException ex) {
            throw new JRubyNotLoadedException(ex);
        } catch (RuntimeException ex) {
            throw wrapJRubyInvokeFailedException(ex);
        }
    }

    private static RuntimeException wrapJRubyInvokeFailedException(final RuntimeException exception) {
        if (exception.getClass().getName().equals("org.jruby.embed.InvokeFailedException")) {
            return new JRubyInvokeFailedException(exception);
        }
        return exception;
    }

    private final String jrubyVersion;
    private final String rubyVersion;

    private final Object scriptingContainer;
    private final Method method_callMethod_ALObject;
    private final Method method_callMethod_LClass;
    private final Method method_callMethod_LObject_LClass;
    private final Method method_getProvider;
    private final Method method_put_LString_LObject;
    private final Method method_remove_LString;
    private final Method method_runScriptlet_LString;

    private final Class<?> class_RubyObject;

    private final Method method_getRubyInstanceConfig;

    private final Field field_COMPILE_INVOKEDYNAMIC;
    private final Method method_force_LString;

    private final Object const_CompileMode_OFF;
    private final Method method_setCompileMode_LCompileMode;

    private final Method method_getRuntime;
}
