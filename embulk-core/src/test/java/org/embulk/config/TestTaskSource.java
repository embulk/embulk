package org.embulk.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.embulk.EmbulkTestRuntime;
import org.embulk.spi.Exec;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestTaskSource {
    private static interface TypeFields extends Task {
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

    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    private TaskSource taskSource;

    @Before
    public void setup() throws Exception {
        taskSource = Exec.newTaskSource();
        taskSource.set("Boolean", false);
        taskSource.set("Double", 0.5);
        taskSource.set("Int", 0);
        taskSource.set("Long", 0);
        taskSource.set("String", "");
    }

    @Test
    public void testEqualsOfLoadedTasks() {
        TypeFields task = taskSource.loadTask(TypeFields.class);
        task.setBoolean(true);
        task.setDouble(0.2);
        task.setInt(3);
        task.setLong(Long.MAX_VALUE);
        task.setString("sf");

        TaskSource taskSource2 = task.dump();
        TypeFields task2 = taskSource2.loadTask(TypeFields.class);

        assertTrue(task.equals(task2));
        assertTrue(task.hashCode() == task2.hashCode());

        task.setBoolean(false);
        assertFalse(task.equals(task2));
        assertFalse(task.hashCode() == task2.hashCode());
    }
}
