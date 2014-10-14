package org.quickload.record;

import java.util.HashMap;
import java.util.Map;
import com.google.inject.Inject;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import org.quickload.model.ModelManager;

public class TypeManager
{
    private final Map<String, Type> fromStringToTypeMap; // TODO inject?

    @Inject
    public TypeManager(ModelManager modelManager)
    {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Type.class, new TypeDeserializer(TypeManager.this));
        modelManager.addObjectMapperModule(module);

        this.fromStringToTypeMap = new HashMap<String, Type>();
        regsterTypes();
    }

    private void regsterTypes()
    {
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
