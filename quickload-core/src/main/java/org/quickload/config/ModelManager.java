package org.quickload.config;

import java.util.Set;
import java.util.HashSet;
import javax.validation.Validation;
import com.google.common.base.Function;
import org.apache.bval.jsr303.ApacheValidationProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class ModelManager
{
    private final Set<Class<?>> registered;
    private final ObjectMapper objectMapper;

    private final ModelValidator modelValidator;
    private final DynamicModeler dynamicModeler;

    public ModelManager()
    {
        this.registered = new HashSet<Class<?>>();
        this.objectMapper = new ObjectMapper().findAndRegisterModules();

        this.modelValidator = new ModelValidator(
                Validation.byProvider(ApacheValidationProvider.class).configure().buildValidatorFactory().getValidator());
        this.dynamicModeler = new DynamicModeler(this);
    }

    public synchronized void addModelSerDe(Class<?> uniqueClass, Function<SimpleModule, Void> generator)
    {
        if (registered.contains(uniqueClass)) {
            return;
        }
        SimpleModule module = new SimpleModule();
        generator.apply(module);
        objectMapper.registerModule(module);
        registered.add(uniqueClass);
    }

    public ModelValidator getModelValidator()
    {
        return modelValidator;
    }

    public DynamicModeler getDynamicModeler()
    {
        return dynamicModeler;
    }

    public ObjectMapper getObjectMapper()
    {
        return objectMapper;
    }
}
