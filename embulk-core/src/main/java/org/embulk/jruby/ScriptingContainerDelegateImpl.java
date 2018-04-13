package org.embulk.jruby;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Indirects onto JRuby not to require JRuby classes directly.
 *
 * It calls methods of ScriptingContainer through reflection.
 *
 * This trick enables Embulk:
 *
 * <ul>
 * <li>To load a JRuby runtime (jruby-complete-*.jar) specified at runtime out of embulk-*.jar.
 * <li>To switch versions of JRuby without releasing or repackaging.
 * <li>To distribute Embulk without JRuby runtimes embedded.
 * </ul>
 */
@SuppressWarnings({"checkstyle:AbbreviationAsWordInName", "checkstyle:MemberName", "checkstyle:ParameterName"})
public final class ScriptingContainerDelegateImpl extends ScriptingContainerDelegate {
    private ScriptingContainerDelegateImpl(
            final String jrubyVersion,
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

    private ScriptingContainerDelegateImpl(
            final String jrubyVersion,
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

    private ScriptingContainerDelegateImpl() {
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

    public static ScriptingContainerDelegateImpl create(
            final ClassLoader classLoader,
            final LocalContextScope delegateLocalContextScope,
            final LocalVariableBehavior delegateLocalVariableBehavior) {
        final Object scriptingContainer = createScriptingContainer(
                classLoader, delegateLocalContextScope, delegateLocalVariableBehavior);

        if (scriptingContainer == null) {
            return new ScriptingContainerDelegateImpl();
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
            return new ScriptingContainerDelegateImpl();  // TODO: Log.
        }

        final Class<?> class_RubyObject;
        try {
            class_RubyObject = classLoader.loadClass("org.jruby.RubyObject");
        } catch (ClassNotFoundException ex) {
            // `org.jruby.RubyObject` is not implemented in the JRuby version -- unlikely.
            return new ScriptingContainerDelegateImpl();  // TODO: Log.
        }

        final Class<?> class_Constants;
        try {
            class_Constants = classLoader.loadClass("org.jruby.runtime.Constants");
        } catch (ClassNotFoundException ex) {
            // `org.jruby.runtime.Constants` is not implemented in the JRuby version -- unlikely.
            return new ScriptingContainerDelegateImpl();  // TODO: Log.
        }
        final String jrubyVersion = (String) getStaticField_Constants(class_Constants, "VERSION");
        final String rubyVersion = (String) getStaticField_Constants(class_Constants, "RUBY_VERSION");

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
            return new ScriptingContainerDelegateImpl(
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

        return new ScriptingContainerDelegateImpl(
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

    public static String getJRubyVersion(final ClassLoader classLoader) {
        final Class<?> class_Constants;
        try {
            class_Constants = classLoader.loadClass("org.jruby.runtime.Constants");
        } catch (ClassNotFoundException ex) {
            // `org.jruby.runtime.Constants` is not implemented in the JRuby version -- unlikely.
            return null;
        }
        return (String) getStaticField_Constants(class_Constants, "VERSION");
    }

    @Override
    public String getJRubyVersion() {
        return this.jrubyVersion;
    }

    @Override
    public String getRubyVersion() {
        return this.rubyVersion;
    }

    // It is intentionally package-private. It is just for logging from JRubyScriptingModule.
    @Override
    String getGemHome() throws JRubyInvalidRuntimeException {
        return this.callMethod(this.getGemPaths(), "home", String.class);
    }

    // It is intentionally package-private. It is just for logging from JRubyScriptingModule.
    @Override
    String getGemPathInString() throws JRubyInvalidRuntimeException {
        final List gemPath = this.callMethod(this.getGemPaths(), "path", List.class);
        return gemPath.toString();
    }

    @Override
    public void clearGemPaths() throws JRubyInvalidRuntimeException {
        this.callMethod(this.runScriptlet("Gem"), "use_paths", (Object) null, (Object) null);
    }

    @Override
    public void setGemPaths(final String gemPath) throws JRubyInvalidRuntimeException {
        this.callMethod(this.runScriptlet("Gem"), "use_paths", gemPath, gemPath);
    }

    @Override
    public boolean isBundleGemfileDefined() throws JRubyInvalidRuntimeException {
        return this.callMethod(this.runScriptlet("ENV"), "has_key?", "BUNDLE_GEMFILE", Boolean.class);
    }

    @Override
    public String getBundleGemfile() throws JRubyInvalidRuntimeException {
        return this.callMethod(this.runScriptlet("ENV"), "fetch", "BUNDLE_GEMFILE", String.class);
    }

    @Override
    public void setBundleGemfile(final String gemfilePath) throws JRubyInvalidRuntimeException {
        this.callMethod(this.runScriptlet("ENV"), "store", "BUNDLE_GEMFILE", gemfilePath);
    }

    @Override
    public void unsetBundleGemfile() throws JRubyInvalidRuntimeException {
        this.callMethod(this.runScriptlet("ENV"), "delete", "BUNDLE_GEMFILE");
    }

    // It is intentionally private. It should return RubyObject while it is Object in the signature.
    @Override
    Object getGemPaths() throws JRubyInvalidRuntimeException {
        return this.callMethod(this.runScriptlet("Gem"), "paths", this.class_RubyObject);
    }

    @Override
    public void processJRubyOption(final String jrubyOption)
            throws JRubyInvalidRuntimeException, UnrecognizedJRubyOptionException, NotWorkingJRubyOptionException {
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
                        } catch (IllegalAccessException | InvocationTargetException ex) {
                            throw new NotWorkingJRubyOptionException(ex);
                        }
                        return;
                    } else if (jrubyOption.equals("--client")) {
                        throw new NotWorkingJRubyOptionException();
                    } else if (jrubyOption.equals("--server")) {
                        throw new NotWorkingJRubyOptionException();
                    }
                    throw new UnrecognizedJRubyOptionException();
                default:
                    throw new UnrecognizedJRubyOptionException();
            }
        }
    }

    @Override
    public Object callMethod(final Object receiver,
                             final String methodName,
                             final Object... args) throws JRubyInvalidRuntimeException {
        if (this.scriptingContainer == null) {
            throw new JRubyNotLoadedException();
        }
        if (this.method_callMethod_ALObject == null) {
            throw new JRubyInvalidRuntimeException(
                    "ScriptingContainer#callMethod(Object, String, Object...) is unavailable unexpectedly.");
        }
        try {
            return this.method_callMethod_ALObject.invoke(this.scriptingContainer, receiver, methodName, args);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (IllegalAccessException ex) {
            throw new JRubyInvalidRuntimeException(
                    "ScriptingContainer#callMethod(Object, String, Object...) is inaccessible unexpectedly.", ex);
        } catch (InvocationTargetException ex) {
            final Throwable cause = ex.getCause();
            if (cause instanceof Error) {
                throw (Error) cause;
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new JRubyRuntimeException(cause);
            }
        }
    }

    /*
    @Override
    public Object callMethod(final Object receiver,
                             final String methodName,
                             final Block block,
                             final Object... args) {
    }
    */

    @Override
    public <T> T callMethod(final Object receiver,
                            final String methodName,
                            final Class<T> returnType) throws JRubyInvalidRuntimeException {
        if (this.scriptingContainer == null) {
            throw new JRubyNotLoadedException();
        }
        if (this.method_callMethod_LClass == null) {
            throw new JRubyInvalidRuntimeException(
                    "ScriptingContainer#callMethod(Object, String, Class) is unavailable unexpectedly.");
        }

        try {
            @SuppressWarnings("unchecked")
            final T returnValue = (T) (this.method_callMethod_LClass.invoke(
                    this.scriptingContainer, receiver, methodName, returnType));
            return returnValue;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (IllegalAccessException ex) {
            throw new JRubyInvalidRuntimeException(
                    "ScriptingContainer#callMethod(Object, String, Class) is inaccessible unexpectedly.", ex);
        } catch (InvocationTargetException ex) {
            final Throwable cause = ex.getCause();
            if (cause instanceof Error) {
                throw (Error) cause;
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new JRubyRuntimeException(cause);
            }
        }
    }

    @Override
    public <T> T callMethod(final Object receiver,
                            final String methodName,
                            final Object singleArg,
                            final Class<T> returnType) throws JRubyInvalidRuntimeException {
        if (this.scriptingContainer == null) {
            throw new JRubyNotLoadedException();
        }
        if (this.method_callMethod_LObject_LClass == null) {
            throw new JRubyInvalidRuntimeException(
                    "ScriptingContainer#callMethod(Object, String, Object, Class) is unavailable unexpectedly.");
        }

        try {
            @SuppressWarnings("unchecked")
            final T returnValue = (T) (this.method_callMethod_LObject_LClass.invoke(
                    this.scriptingContainer, receiver, methodName, singleArg, returnType));
            return returnValue;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (IllegalAccessException ex) {
            throw new JRubyInvalidRuntimeException(
                    "ScriptingContainer#callMethod(Object, String, Object, Class) is inaccessible unexpectedly.", ex);
        } catch (InvocationTargetException ex) {
            final Throwable cause = ex.getCause();
            if (cause instanceof Error) {
                throw (Error) cause;
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new JRubyRuntimeException(cause);
            }
        }
    }

    /*
    @Override
    public <T> T callMethod(final Object receiver,
                            final String methodName,
                            final Object[] args,
                            final Class<T> returnType) {
    }
    */

    /*
    @Override
    public <T> T callMethod(final Object receiver,
                            final String methodName,
                            final Object[] args,
                            final Block block,
                            final Class<T> returnType) {
    }
    */

    /*
    @Override
    public <T> T callMethod(final Object receiver,
                            final String methodName,
                            final Class<T> returnType,
                            final EmbedEvalUnit unit) {
    }
    */

    /*
    @Override
    public <T> T callMethod(final Object receiver,
                            final String methodName,
                            final Object[] args,
                            final Class<T> returnType,
                            final EmbedEvalUnit unit) {
    }
    */

    /*
    @Override
    public <T> T callMethod(final Object receiver,
                            final String methodName,
                            final Object[] args,
                            final Block block,
                            final Class<T> returnType,
                            final EmbedEvalUnit unit) {
    }
    */

    // It is intentionally private. It should return LocalContextProvider while it is Object in the signature.
    @Override
    Object getProvider() throws JRubyInvalidRuntimeException {
        if (this.scriptingContainer == null) {
            throw new JRubyNotLoadedException();
        }
        if (this.method_getProvider == null) {
            throw new JRubyInvalidRuntimeException(
                    "ScriptingContainer#getProvider() is unavailable unexpectedly.");
        }

        try {
            return this.method_getProvider.invoke(this.scriptingContainer);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (IllegalAccessException ex) {
            throw new JRubyInvalidRuntimeException(
                    "ScriptingContainer#getProvider() is inaccessible unexpectedly.", ex);
        } catch (InvocationTargetException ex) {
            final Throwable cause = ex.getCause();
            if (cause instanceof Error) {
                throw (Error) cause;
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new JRubyRuntimeException(cause);
            }
        }
    }

    @Override
    public Object put(final String key, final Object value) throws JRubyInvalidRuntimeException {
        if (this.scriptingContainer == null) {
            throw new JRubyNotLoadedException();
        }
        if (this.method_put_LString_LObject == null) {
            throw new JRubyInvalidRuntimeException(
                    "ScriptingContainer#put(String, Object) is unavailable unexpectedly.");
        }

        try {
            return this.method_put_LString_LObject.invoke(this.scriptingContainer, key, value);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (IllegalAccessException ex) {
            throw new JRubyInvalidRuntimeException(
                    "ScriptingContainer#put(String, Object) is inaccessible unexpectedly.", ex);
        } catch (InvocationTargetException ex) {
            final Throwable cause = ex.getCause();
            if (cause instanceof Error) {
                throw (Error) cause;
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new JRubyRuntimeException(cause);
            }
        }
    }

    @Override
    public Object remove(final String key) throws JRubyInvalidRuntimeException {
        if (this.scriptingContainer == null) {
            throw new JRubyNotLoadedException();
        }
        if (this.method_remove_LString == null) {
            throw new JRubyInvalidRuntimeException(
                    "ScriptingContainer#remove(String) is unavailable unexpectedly.");
        }

        try {
            return this.method_remove_LString.invoke(this.scriptingContainer, key);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (IllegalAccessException ex) {
            throw new JRubyInvalidRuntimeException(
                    "ScriptingContainer#remove(String) is inaccessible unexpectedly.", ex);
        } catch (InvocationTargetException ex) {
            final Throwable cause = ex.getCause();
            if (cause instanceof Error) {
                throw (Error) cause;
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new JRubyRuntimeException(cause);
            }
        }
    }

    @Override
    public Object runScriptlet(final String script) throws JRubyInvalidRuntimeException {
        if (this.scriptingContainer == null) {
            throw new JRubyNotLoadedException();
        }
        if (this.method_runScriptlet_LString == null) {
            throw new JRubyInvalidRuntimeException(
                    "ScriptingContainer#runScriptlet(String) is unavailable unexpectedly.");
        }

        try {
            return this.method_runScriptlet_LString.invoke(this.scriptingContainer, script);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (IllegalAccessException ex) {
            throw new JRubyInvalidRuntimeException(
                    "ScriptingContainer#runScriptlet(String) is inaccessible unexpectedly.", ex);
        } catch (InvocationTargetException ex) {
            final Throwable cause = ex.getCause();
            if (cause instanceof Error) {
                throw (Error) cause;
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new JRubyRuntimeException(cause);
            }
        }
    }

    // It is intentionally package-private. It should return RubyInstanceConfig while it is Object in the signature.
    @Override
    Object getRubyInstanceConfig() throws JRubyInvalidRuntimeException {
        final Object provider = this.getProvider();
        if (provider == null) {
            throw new JRubyInvalidRuntimeException(
                    "ScriptingContainer#getProvider() returned invalid LocalContextProvider unexpectedly.");
        }
        if (this.method_getRubyInstanceConfig == null) {
            throw new JRubyInvalidRuntimeException(
                    "LocalContextProvider#getRubyInstanceConfig is unavailable unexpectedly.");
        }

        try {
            return this.method_getRubyInstanceConfig.invoke(provider);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (IllegalAccessException ex) {
            throw new JRubyInvalidRuntimeException(
                    "LocalContextProvider#getRubyInstanceConfig is inaccessible unexpectedly.", ex);
        } catch (InvocationTargetException ex) {
            final Throwable cause = ex.getCause();
            if (cause instanceof Error) {
                throw (Error) cause;
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new JRubyRuntimeException(cause);
            }
        }
    }

    // It is intentionally package-private. It should return Runtime while it is Object in the signature.
    @Override
    Object getRuntime() throws JRubyInvalidRuntimeException {
        final Object provider = this.getProvider();
        if (provider == null) {
            throw new JRubyInvalidRuntimeException(
                    "ScriptingContainer#getProvider() returned invalid LocalContextProvider unexpectedly.");
        }
        if (this.method_getRuntime == null) {
            throw new JRubyInvalidRuntimeException(
                    "LocalContextProvider#getRuntime is unavailable unexpectedly.");
        }

        try {
            return this.method_getRuntime.invoke(provider);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (IllegalAccessException ex) {
            throw new JRubyInvalidRuntimeException(
                    "LocalContextProvider#getRuntime is inaccessible unexpectedly.", ex);
        } catch (InvocationTargetException ex) {
            final Throwable cause = ex.getCause();
            if (cause instanceof Error) {
                throw (Error) cause;
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new JRubyRuntimeException(cause);
            }
        }
    }

    // TODO: Remove this method finally. https://github.com/embulk/embulk/issues/1007
    // It is intentionally package-private. It should return ScriptingContainer while it is Object in the signature.
    @Override
    Object getScriptingContainer() throws JRubyNotLoadedException {
        if (this.scriptingContainer == null) {
            throw new JRubyNotLoadedException();
        }
        return this.scriptingContainer;
    }

    /** Instance of org.jruby.embed.ScriptingContainer. It may be created lazily. */
    private final Object scriptingContainer;

    private final String jrubyVersion;
    private final String rubyVersion;

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
