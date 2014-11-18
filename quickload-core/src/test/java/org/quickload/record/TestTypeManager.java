package org.quickload.record;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import org.junit.runner.RunWith;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.quickload.config.ModelManager;
import org.quickload.config.TaskSource;
import org.quickload.GuiceJUnitRunner;
import org.quickload.TestRuntimeModule;

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
