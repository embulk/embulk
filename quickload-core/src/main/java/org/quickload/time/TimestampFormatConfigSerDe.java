package org.quickload.time;

import com.google.inject.Inject;
import org.jruby.embed.ScriptingContainer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import org.quickload.config.ModelManager;

public class TimestampFormatConfigSerDe
{
    @Inject
    public TimestampFormatConfigSerDe(ModelManager modelManager, ScriptingContainer jruby)
    {
        SimpleModule module = new SimpleModule();

        module.addDeserializer(TimestampFormatConfig.class, new TimestampFormatConfigDeserializer(jruby));

        modelManager.addObjectMapperModule(module);
    }

    public static class TimestampFormatConfigDeserializer
            extends FromStringDeserializer<TimestampFormatConfig>
    {
        private final ScriptingContainer jruby;

        public TimestampFormatConfigDeserializer(ScriptingContainer jruby)
        {
            super(TimestampFormatConfig.class);
            this.jruby = jruby;
        }

        @Override
        protected TimestampFormatConfig _deserialize(String value, DeserializationContext context)
        {
            return new TimestampFormatConfig(jruby, value);
        }
    }
}
