package org.quickload.config;

import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationHandler;
import javax.validation.Validation;
import org.apache.bval.jsr303.ApacheValidationProvider;
import static com.google.common.base.Preconditions.checkNotNull;

public class DynamicModeler
{
    private ModelValidator validator;
    private ModelRegistry modelRegistry;

    // TODO inject by guava
    public DynamicModeler(ModelRegistry modelRegistry)
    {
        this.validator = new ModelValidator(
                Validation.byProvider(ApacheValidationProvider.class).configure().buildValidatorFactory().getValidator());
        this.modelRegistry = modelRegistry;
    }

    public <T extends DynamicModel<T>> T model(Class<T> iface)
    {
        Object proxy = Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] { iface },
                new DynamicModelHandler(validator));
        return (T) proxy;
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

    private interface XModel
            extends DynamicModel <XModel>
    {
        public List<String> getPaths();
    }
}
