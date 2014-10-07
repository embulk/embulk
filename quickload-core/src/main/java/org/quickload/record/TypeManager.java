package org.quickload.record;

import com.google.common.base.Function;
import com.google.inject.Inject;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import org.quickload.config.ModelManager;

import java.util.HashMap;
import java.util.Map;

public class TypeManager
{
    private final ModelManager models;
    private final Map<String, Type> fromStringToTypeMap; // TODO inject?

    @Inject
    public TypeManager(ModelManager models)
    {
        this.models = models;
        models.addModelSerDe(Type.class, new Function<SimpleModule, Void>() {
            public Void apply(SimpleModule module)
            {
                module.addDeserializer(Type.class, new TypeDeserializer(TypeManager.this));
                return null;
            }
        });

        this.fromStringToTypeMap = new HashMap<String, Type>();
        regsterTypes();
    }

    private void regsterTypes() {
        // TODO inject?
        fromStringToTypeMap.put(DoubleType.DOUBLE.getName(), DoubleType.DOUBLE);
        fromStringToTypeMap.put(LongType.LONG.getName(), LongType.LONG);
        fromStringToTypeMap.put(StringType.STRING.getName(), StringType.STRING);
    }

    public Type getType(String name)
    {
        // TODO if null?
        return fromStringToTypeMap.get(name);
    }

    static class TypeDeserializer
            extends FromStringDeserializer<Type>
    {
        private final TypeManager typeManager;

        public TypeDeserializer(TypeManager typeManager)
        {
            super(Type.class);
            this.typeManager = typeManager;
        }

        @Override
        protected Type _deserialize(String value, DeserializationContext context)
        {
            return typeManager.getType(value);
        }
    }
}
