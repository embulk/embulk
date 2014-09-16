package org.quickload.config;

import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import static java.lang.reflect.Modifier.isPublic;
import com.google.common.base.Function;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableMap;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class DynamicModeler
{
    private final static Class[] proxyConstructorParams = { InvocationHandler.class };

    private final ModelManager modelManager;

    // TODO inject by guava
    public DynamicModeler(ModelManager modelManager)
    {
        this.modelManager = modelManager;
    }

    public <T extends DynamicModel<T>> InstanceFactory<T> model(final Class<T> iface)
    {
        final Class<? extends T> proxyClass = (Class<? extends T>) Proxy.getProxyClass(iface.getClassLoader(), iface);
        final InstanceFactory<T> factory = new InstanceFactory<T>(proxyClass);
        modelManager.addModelSerDe(iface, new Function<SimpleModule, Void>() {
            public Void apply(SimpleModule module)
            {
                Map<String, TypeReference<?>> attrs = collectAttributes(iface);
                module.addSerializer(proxyClass, new DynamicModelSerializer(
                        attrs.keySet(), modelManager.getObjectMapper()));
                module.addDeserializer(iface, new DynamicModelDeserializer(
                        factory, attrs, modelManager.getObjectMapper()));
                return null;
            }
        });
        return factory;
    }

    public <T extends DynamicModel<T>> T newModelInstance(Class<T> iface)
    {
        return model(iface).newInstance();
    }

    public class InstanceFactory <T extends DynamicModel<T>>
    {
        private final Class<?> proxyClass;

        InstanceFactory(Class<? extends T> proxyClass)
        {
            this.proxyClass = proxyClass;
        }

        public T newInstance()
        {
            try {
                return (T) proxyClass.getConstructor(proxyConstructorParams)
                    .newInstance(new DynamicModelHandler(modelManager.getModelValidator()));
            } catch (NoSuchMethodException e) {
                throw new AssertionError(e);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            } catch (InstantiationException e) {
                throw new AssertionError(e);
            } catch (InvocationTargetException e) {
                throw new AssertionError(e);
            }
        }
    }

    private static Map<String, TypeReference<?>> collectAttributes(Class<?> iface)
    {
        ImmutableMap.Builder<String, TypeReference<?>> attrs = ImmutableMap.builder();

        // use all public getXxx methods
        for (Method method : iface.getMethods()) {
            if (!isPublic(method.getModifiers())) {
                continue;
            }

            String methodName = method.getName();
            if (!methodName.startsWith("get")) {
                continue;
            }
            if (methodName.equals("get")) {
                continue;
            }

            String attrName = methodName.substring(3);
            attrs.put(attrName, new GenericTypeReference(method.getGenericReturnType()));
        }

        return attrs.build();
    }

    private static class DynamicModelHandler
            implements InvocationHandler
    {
        private final Map<String, Object> attributes;
        private final ModelValidator validator;

        public DynamicModelHandler(ModelValidator validator)
        {
            this.attributes = new ConcurrentHashMap<String, Object>();
            this.validator = validator;
        }

        public Object invoke(Object proxy, Method method, Object[] args)
        {
            String methodName = method.getName();

            switch(methodName) {
            case "get":
                checkArgumentLength(args, 1, methodName);
                return attributes.get((String) args[0]);

            case "set":
                checkArgumentLength(args, 2, methodName);
                attributes.put((String) args[0], args[1]);
                return null;

            case "validate":
                validator.validateModel((DynamicModel<?>) proxy);
                return proxy;

            case "hashCode":
                checkArgumentLength(args, 0, methodName);
                return attributes.hashCode();

            case "equals":
                checkArgumentLength(args, 1, methodName);
                if (args[0] instanceof Proxy) {
                    Object other = Proxy.getInvocationHandler(args[0]);
                    if (other instanceof DynamicModelHandler) {
                        return attributes.equals(((DynamicModelHandler) other).attributes);
                    }
                }
                return false;

            case "toString":
                checkArgumentLength(args, 0, methodName);
                // TODO
                return this.toString();

            default:
                if (methodName.startsWith("get")) {
                    checkArgumentLength(args, 0, methodName);
                    String attrName = methodName.substring(3);
                    return attributes.get(attrName);

                } else if (methodName.startsWith("set")) {
                    checkArgumentLength(args, 1, methodName);
                    String attrName = methodName.substring(3);
                    attributes.put(attrName, args[0]);
                    return proxy;
                }
            }

            throw new IllegalArgumentException(String.format("Undefined method '%s'", methodName));
        }

        private static void checkArgumentLength(Object[] args, int expected, String methodName)
        {
            if (args == null ? expected != 0 : expected != args.length) {
                throw new IllegalArgumentException(
                        String.format("Method '%s' expected %d argument but got %d arguments", methodName, expected, (args == null ? 0 : args.length)));
            }
        }
    }
}
