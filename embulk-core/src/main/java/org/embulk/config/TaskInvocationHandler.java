package org.embulk.config;

import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationHandler;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import com.google.common.collect.ImmutableMap;

class TaskInvocationHandler
        implements InvocationHandler
{
    private final ModelManager model;
    private final Class<?> iface;
    private final Map<String, Object> objects;
    private final Set<String> injectedFields;

    public TaskInvocationHandler(ModelManager model, Class<?> iface, Map<String, Object> objects, Set<String> injectedFields)
    {
        this.model = model;
        this.iface = iface;
        this.objects = objects;
        this.injectedFields = injectedFields;
    }

    /**
     * fieldName = Method of the getter
     */
    public static Map<String, Method> fieldGetters(Class<?> iface)
    {
        ImmutableMap.Builder<String, Method> builder = ImmutableMap.builder();
        for (Method method : iface.getMethods()) {
            String methodName = method.getName();
            String fieldName = getterFieldNameOrNull(methodName);
            if (fieldName != null && hasExpectedArgumentLength(method, 0)) {
                builder.put(fieldName, method);
            }
        }
        return builder.build();
    }

    // visible for ModelManager.AccessorSerializer
    Map<String, Object> getObjects()
    {
        return objects;
    }

    // visible for ModelManager.AccessorSerializer
    Set<String> getInjectedFields()
    {
        return injectedFields;
    }

    protected Object invokeGetter(Method method, String fieldName)
    {
        return objects.get(fieldName);
    }

    protected void invokeSetter(Method method, String fieldName, Object value)
    {
        if (value == null) {
            objects.remove(fieldName);
        } else {
            objects.put(fieldName, value);
        }
    }

    private Map<String, Object> getSerializableFields()
    {
        Map<String, Object> data = new HashMap<String, Object>(objects);
        for (String injected : injectedFields) {
            data.remove(injected);
        }
        return data;
    }

    protected TaskSource invokeDump()
    {
        return new DataSourceImpl(model, model.writeObjectAsObjectNode(getSerializableFields()));
    }

    protected String invokeToString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(iface.getName());
        sb.append(getSerializableFields());
        return sb.toString();
    }

    protected int invokeHashCode()
    {
        return objects.hashCode();
    }

    protected boolean invokeEquals(Object other)
    {
        return (other instanceof TaskInvocationHandler) &&
            objects.equals(((TaskInvocationHandler) other).objects);
    }

    public Object invoke(Object proxy, Method method, Object[] args)
    {
        String methodName = method.getName();

        switch(methodName) {
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

        default:
            {
                String fieldName;
                fieldName = getterFieldNameOrNull(methodName);
                if (fieldName != null) {
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

    private static String getterFieldNameOrNull(String methodName)
    {
        if (methodName.startsWith("get")) {
            return methodName.substring(3);
        }
        return null;
    }

    private static String setterFieldNameOrNull(String methodName)
    {
        if (methodName.startsWith("set")) {
            return methodName.substring(3);
        }
        return null;
    }

    protected static boolean hasExpectedArgumentLength(Method method, int expected)
    {
        return method.getParameterTypes().length == expected;
    }

    protected static void checkArgumentLength(Method method, int expected, String methodName)
    {
        if (!hasExpectedArgumentLength(method, expected)) {
            throw new IllegalArgumentException(
                    String.format("Method '%s' expected %d argument but got %d arguments", methodName, expected, method.getParameterTypes().length));
        }
    }
}
