package org.embulk.record;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.embulk.GuiceJUnitRunner;
import org.embulk.TestRuntimeModule;
import org.embulk.config.ModelManager;
import org.embulk.config.TaskSource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Inject;

@RunWith(GuiceJUnitRunner.class)
@GuiceJUnitRunner.GuiceModules({ TestRuntimeModule.class })
public class TestTypeManager
{
    @Inject
    protected ModelManager modelManager;

    private static class HasType
    {
        private Type type;

        @JsonCreator
        public HasType(
                @JsonProperty("type") Type type)
        {
            this.type = type;
        }

        @JsonProperty("type")
        public Type getType()
        {
            return type;
        }
    }

    @Test
    public void testGetType()
    {
        HasType type = new HasType(StringType.STRING);
        TaskSource taskSource = modelManager.writeAsTaskSource(type);
        HasType decoded = modelManager.readObject(taskSource, HasType.class);
        assertTrue(StringType.STRING == decoded.getType());
    }
}
