package org.quickload;

//import org.junit.AssumptionViolatedException;
import java.util.concurrent.TimeUnit;

import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class DurationWatch
        implements TestRule
{
    private volatile long startNanoTime;
    private volatile long endNanoTime;

    public DurationWatch()
    {
        endNanoTime = System.nanoTime();
    }

    public long runtime(TimeUnit unit)
    {
        long end = endNanoTime;
        if (end == 0) {
            end = System.nanoTime();
        }
        return unit.convert(end - startNanoTime, TimeUnit.NANOSECONDS);
    }

    public long runtimeMillis()
    {
        return runtime(TimeUnit.MILLISECONDS);
    }

    private void reset()
    {
        startNanoTime = System.nanoTime();
        endNanoTime = 0;
    }

    private void finish()
    {
        endNanoTime = System.nanoTime();
    }

    @Override
    public final Statement apply(Statement base, Description description)
    {
        return new DurationTestWatcher().apply(base, description);
    }

    private class DurationTestWatcher
            extends TestWatcher
    {
        @Override
        protected void starting(Description description)
        {
            reset();
        }

        @Override
        protected void finished(Description description)
        {
            finish();
        }
    }
}
