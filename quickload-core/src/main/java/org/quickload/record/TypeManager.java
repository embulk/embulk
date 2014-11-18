package org.quickload.record;

import java.util.HashMap;
import java.util.Map;
import com.google.inject.Inject;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import org.quickload.config.ModelManager;

public class TypeManager
{
    private final Map<String, Type> fromStringToTypeMap; // TODO inject?

    @Inject
    public TypeManager(ModelManager modelManager)
    {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Type.class, new TypeDeserializer());
        modelManager.addObjectMapperModule(module);

        this.fromStringToTypeMap = new HashMap<String, Type>();
        regsterTypes();
    }

    private void regsterTypes()
    {
        // TODO inject?
        fromStringToTypeMap.put(BooleanType.BOOLEAN.getName(), BooleanType.BOOLEAN);
        fromStringToTypeMap.put(LongType.LONG.getName(), LongType.LONG);
        fromStringToTypeMap.put(DoubleType.DOUBLE.getName(), DoubleType.DOUBLE);
        fromStringToTypeMap.put(StringType.STRING.getName(), StringType.STRING);
        fromStringToTypeMap.put(TimestampType.TIMESTAMP.getName(), TimestampType.TIMESTAMP);
    }

    public Type getType(String name)
    {
        // TODO if null?
        return fromStringToTypeMap.get(name);
    }

    class TypeDeserializer
            extends FromStringDeserializer<Type>
    {
        public TypeDeserializer()
        {
            super(Type.class);
        }

        @Override
        protected Type _deserialize(String value, DeserializationContext context)
        {
            return getType(value);
        }
    }
}
