package org.embulk.config;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class TaskInvocationHandler implements InvocationHandler {
    private final ModelManager model;
    private final Class<?> iface;
    private final Map<String, Object> objects;
    private final Set<String> injectedFields;

    public TaskInvocationHandler(ModelManager model, Class<?> iface, Map<String, Object> objects, Set<String> injectedFields) {
        this.model = model;
        this.iface = iface;
        this.objects = objects;
        this.injectedFields = injectedFields;
    }

    /**
     * Returns a Multimap from fieldName Strings to their getter Methods.
     *
     * It expects to be called only from TaskSerDe. Multimap is used inside org.embulk.config.
     */
    static Multimap<String, Method> fieldGetters(Class<?> iface) {
        ImmutableMultimap.Builder<String, Method> builder = ImmutableMultimap.builder();
        for (Method method : iface.getMethods()) {
            String methodName = method.getName();
            String fieldName = getterFieldNameOrNull(methodName);
            if (fieldName != null && hasExpectedArgumentLength(method, 0)
                    && (!method.isDefault() || method.getAnnotation(Config.class) != null)) {
                // If the method has default implementation, and @Config is not annotated there, the method is kept.
                builder.put(fieldName, method);
            }
        }
        return builder.build();
    }

    // visible for ModelManager.AccessorSerializer
    Map<String, Object> getObjects() {
        return objects;
    }

    // visible for ModelManager.AccessorSerializer
    Set<String> getInjectedFields() {
        return injectedFields;
    }

    protected Object invokeGetter(Method method, String fieldName) {
        return objects.get(fieldName);
    }

    protected void invokeSetter(Method method, String fieldName, Object value) {
        if (value == null) {
            objects.remove(fieldName);
        } else {
            objects.put(fieldName, value);
        }
    }

    private Map<String, Object> getSerializableFields() {
        Map<String, Object> data = new HashMap<String, Object>(objects);
        for (String injected : injectedFields) {
            data.remove(injected);
        }
        return data;
    }

    protected TaskSource invokeDump() {
        return new DataSourceImpl(model, model.writeObjectAsObjectNode(getSerializableFields()));
    }

    protected String invokeToString() {
        StringBuilder sb = new StringBuilder();
        sb.append(iface.getName());
        sb.append(getSerializableFields());
        return sb.toString();
    }

    protected int invokeHashCode() {
        return objects.hashCode();
    }

    protected boolean invokeEquals(Object other) {
        return (other instanceof TaskInvocationHandler)
                && objects.equals(((TaskInvocationHandler) other).objects);
    }

    public Object invoke(Object proxy, Method method, Object[] args) {
        String methodName = method.getName();

        switch (methodName) {
            case "validate":
                checkArgumentLength(method, 0, methodName);
                model.validate(proxy);
                return proxy;

            case "dump":
                checkArgumentLength(method, 0, methodName);
                return invokeDump();

            case "toString":
                checkArgumentLength(method, 0, methodName);
                return invokeToString();

            case "hashCode":
                checkArgumentLength(method, 0, methodName);
                return invokeHashCode();

            case "equals":
                checkArgumentLength(method, 1, methodName);
                if (args[0] instanceof Proxy) {
                    Object otherHandler = Proxy.getInvocationHandler(args[0]);
                    return invokeEquals(otherHandler);
                }
                return false;

            default: {
                String fieldName;
                fieldName = getterFieldNameOrNull(methodName);
                if (fieldName != null) {
                    if (method.isDefault() && !this.objects.containsKey(fieldName)) {
                        // If and only if the method has default implementation, and @Config is not annotated there,
                        // it is tried to call the default implementation directly without proxying.
                        //
                        // methodWithDefaultImpl.invoke(proxy) without this hack would cause infinite recursive calls.
                        //
                        // See hints:
                        // https://rmannibucau.wordpress.com/2014/03/27/java-8-default-interface-methods-and-jdk-dynamic-proxies/
                        // https://stackoverflow.com/questions/22614746/how-do-i-invoke-java-8-default-methods-reflectively
                        //
                        // This hack is required to support `org.joda.time.DateTimeZone` in some Tasks, for example
                        // TimestampParser.Task and TimestampParser.TimestampColumnOption.
                        //
                        // TODO: Remove the hack once a cleaner way is found, or Joda-Time is finally removed.
                        // https://github.com/embulk/embulk/issues/890
                        if (CONSTRUCTOR_MethodHandles_Lookup != null) {
                            synchronized (CONSTRUCTOR_MethodHandles_Lookup) {
                                boolean hasSetAccessible = false;
                                try {
                                    CONSTRUCTOR_MethodHandles_Lookup.setAccessible(true);
                                    hasSetAccessible = true;
                                } catch (SecurityException ex) {
                                    // Skip handling default implementation in case of errors.
                                }

                                if (hasSetAccessible) {
                                    try {
                                        return CONSTRUCTOR_MethodHandles_Lookup
                                                .newInstance(
                                                        method.getDeclaringClass(),
                                                        MethodHandles.Lookup.PUBLIC
                                                                | MethodHandles.Lookup.PRIVATE
                                                                | MethodHandles.Lookup.PROTECTED
                                                                | MethodHandles.Lookup.PACKAGE)
                                                .unreflectSpecial(method, method.getDeclaringClass())
                                                .bindTo(proxy)
                                                .invokeWithArguments();
                                    } catch (Throwable ex) {
                                        // Skip handling default implementation in case of errors.
                                    } finally {
                                        CONSTRUCTOR_MethodHandles_Lookup.setAccessible(false);
                                    }
                                }
                            }
                        }
                    }
                    checkArgumentLength(method, 0, methodName);
                    return invokeGetter(method, fieldName);
                }
                fieldName = setterFieldNameOrNull(methodName);
                if (fieldName != null) {
                    checkArgumentLength(method, 1, methodName);
                    invokeSetter(method, fieldName, args[0]);
                    return this;
                }
            }
        }

        throw new IllegalArgumentException(String.format("Undefined method '%s'", methodName));
    }

    private static String getterFieldNameOrNull(String methodName) {
        if (methodName.startsWith("get")) {
            return methodName.substring(3);
        }
        return null;
    }

    private static String setterFieldNameOrNull(String methodName) {
        if (methodName.startsWith("set")) {
            return methodName.substring(3);
        }
        return null;
    }

    protected static boolean hasExpectedArgumentLength(Method method, int expected) {
        return method.getParameterTypes().length == expected;
    }

    protected static void checkArgumentLength(Method method, int expected, String methodName) {
        if (!hasExpectedArgumentLength(method, expected)) {
            throw new IllegalArgumentException(
                    String.format("Method '%s' expected %d argument but got %d arguments", methodName, expected, method.getParameterTypes().length));
        }
    }

    static {
        Constructor<MethodHandles.Lookup> constructorMethodHandlesLookupTemporary = null;
        try {
            constructorMethodHandlesLookupTemporary =
                    MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
        } catch (NoSuchMethodException | SecurityException ex) {
            constructorMethodHandlesLookupTemporary = null;
        } finally {
            CONSTRUCTOR_MethodHandles_Lookup = constructorMethodHandlesLookupTemporary;
        }
    }

    private static final Constructor<MethodHandles.Lookup> CONSTRUCTOR_MethodHandles_Lookup;
}
