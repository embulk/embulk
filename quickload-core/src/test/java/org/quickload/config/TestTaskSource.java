package org.quickload.config;

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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.quickload.exec.ExecModule;
import org.quickload.GuiceJUnitRunner;

@RunWith(GuiceJUnitRunner.class)
@GuiceJUnitRunner.GuiceModules({ ExecModule.class })
public class TestTaskSource
{
    @Inject
    protected ModelManager modelManager;

    private TaskSource taskSource;

    @Before
    public void setup() throws Exception
    {
        taskSource = new TaskSource();
    }

    private static interface TypeFields
            extends Task
    {
        public boolean getBoolean();
        public void setBoolean(boolean v);

        public byte[] getByteArray();
        public void setByteArray(byte[] v);

        public double getDouble();
        public void setDouble(double v);

        public int getInt();
        public void setInt(int v);

        public long getLong();
        public void setLong(long v);

        public String getString();
        public void setString(String v);
    }

    @Test
    public void testEqualsOfLoadedTasks()
    {
        TypeFields task = taskSource.loadModel(modelManager, TypeFields.class);
        task.setBoolean(true);
        // TODO byte[].equals doesn't work
        //task.setByteArray(new byte[] { (byte) 0xff, (byte) 0xfe } );
        task.setDouble(0.2);
        task.setInt(3);
        task.setLong(Long.MAX_VALUE);
        task.setString("sf");

        ObjectNode json = modelManager.writeJsonObjectNode(task);
        TypeFields task2 = new TaskSource(json).loadModel(modelManager, TypeFields.class);

        assertTrue(task.equals(task2));
        assertTrue(task.hashCode() == task2.hashCode());

        task.setBoolean(false);
        assertFalse(task.equals(task2));
        assertFalse(task.hashCode() == task2.hashCode());
    }
}
