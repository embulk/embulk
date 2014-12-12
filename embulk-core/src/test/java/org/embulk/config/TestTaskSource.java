package org.embulk.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.embulk.GuiceJUnitRunner;
import org.embulk.TestRuntimeModule;

import com.google.inject.Inject;

@RunWith(GuiceJUnitRunner.class)
@GuiceJUnitRunner.GuiceModules({ TestRuntimeModule.class })
public class TestTaskSource
{
    @Inject
    protected ModelManager modelManager;

    private TaskSource taskSource;

    @Before
    public void setup() throws Exception
    {
        taskSource = new TaskSource();
        taskSource.setBoolean("Boolean", false);
        taskSource.setDouble("Double", 0.5);
        taskSource.setInt("Int", 0);
        taskSource.setLong("Long", 0);
        taskSource.setString("String", "");
    }

    private static interface TypeFields
            extends Task
    {
        public boolean getBoolean();
        public void setBoolean(boolean v);

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
        TypeFields task = modelManager.readObject(taskSource, TypeFields.class);
        task.setBoolean(true);
        task.setDouble(0.2);
        task.setInt(3);
        task.setLong(Long.MAX_VALUE);
        task.setString("sf");

        TaskSource taskSource2 = modelManager.writeAsTaskSource(task);
        TypeFields task2 = modelManager.readObject(taskSource2, TypeFields.class);

        assertTrue(task.equals(task2));
        assertTrue(task.hashCode() == task2.hashCode());

        task.setBoolean(false);
        assertFalse(task.equals(task2));
        assertFalse(task.hashCode() == task2.hashCode());
    }
}
