package org.quickload.record;

import com.google.common.base.Function;
import com.google.inject.Inject;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import org.quickload.config.ModelManager;

public class TypeManager
{
    private final ModelManager models;

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
    }

    public Type getType(String name)
    {
        // TODO
        return null;
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
