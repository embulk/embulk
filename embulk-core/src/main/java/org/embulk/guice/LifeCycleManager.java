/*
 * Copyright 2010 Proofpoint, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Copyright 2015 Sadayuki Furuhashi
 */
package org.embulk.guice;

import com.google.common.collect.Lists;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages PostConstruct and PreDestroy life cycles
 */
public final class LifeCycleManager
{
    private final AtomicReference<State> state = new AtomicReference<State>(State.LATENT);
    private final Queue<Object> managedInstances = new ConcurrentLinkedQueue<Object>();
    private final LifeCycleMethodsMap methodsMap;
    private final LifeCycleListener listener;

    private enum State
    {
        LATENT,
        STARTING,
        STARTED,
        STOPPING,
        STOPPED
    }

    /**
     * @param managedInstances list of objects that have life cycle annotations
     * @param methodsMap existing or new methods map
     * @param listener listner called when state changes
     * @throws Exception exceptions starting instances (depending on mode)
     */
    public LifeCycleManager(List<Object> managedInstances, LifeCycleMethodsMap methodsMap, LifeCycleListener listener)
            throws Exception
    {
        this.methodsMap = (methodsMap != null) ? methodsMap : new LifeCycleMethodsMap();
        this.listener = listener;
        for (Object instance : managedInstances) {
            addInstance(instance);
        }
    }

    /**
     * Returns the number of managed instances
     *
     * @return qty
     */
    public int size()
    {
        return managedInstances.size();
    }

    /**
     * Start the life cycle - all instances will have their {@link javax.annotation.PostConstruct} method(s) called
     */
    public void start()
    {
        if (!state.compareAndSet(State.LATENT, State.STARTING)) {
            throw new IllegalStateException("System already starting");
        }
        if (listener != null) {
            listener.startingLifeCycle();
        }

        for (Object obj : managedInstances) {
            LifeCycleMethods methods = methodsMap.get(obj.getClass());
            if (!methods.hasFor(PreDestroy.class)) {
                managedInstances.remove(obj);   // remove reference to instances that aren't needed anymore
            }
        }

        state.set(State.STARTED);
        if (listener != null) {
            listener.startedLifeCycle();
        }
    }

    /**
     * Add a shutdown hook that calls {@link destroy} method
     */
    public void destroyOnShutdownHook()
    {
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                try {
                    LifeCycleManager.this.destroy();
                }
                catch (Exception e) {
                    System.err.print("Exception in life cycle shutdown handler ");
                    e.printStackTrace(System.err);
                }
            }
        });
    }

    /**
     * Stop the life cycle - all instances will have their {@link javax.annotation.PreDestroy} method(s) called
     *
     * @throws Exception errors
     */
    public void destroy()
            throws Exception
    {
        if (!state.compareAndSet(State.STARTED, State.STOPPING)) {
            return;
        }
        if (listener != null) {
            listener.stoppingLifeCycle();
        }

        List<Object> reversedInstances = Lists.newArrayList(managedInstances);
        Collections.reverse(reversedInstances);

        for (Object obj : reversedInstances) {
            if (listener != null) {
                listener.stoppingInstance(obj);
            }
            LifeCycleMethods methods = methodsMap.get(obj.getClass());
            for (Method preDestroy : methods.methodsFor(PreDestroy.class)) {
                if (listener != null) {
                    listener.preDestroyingInstance(obj, preDestroy);
                }
                preDestroy.invoke(obj);
            }
        }

        state.set(State.STOPPED);
        if (listener != null) {
            listener.stoppedLifeCycle();
        }
    }

    /**
     * Return true if {@link destroy} is called
     *
     * @return true if already destroyed
     */
    public boolean isDestroyed()
    {
        State currentState = state.get();
        return currentState == State.STOPPING || currentState == State.STOPPED;
    }

    /**
     * Add an additional managed instance
     *
     * @param instance instance to add
     * @throws Exception errors
     */
    public void addInstance(Object instance)
            throws Exception
    {
        if (isDestroyed()) {
            throw new IllegalStateException("System already stopped");
        }
        else {
            startInstance(instance);
            if (methodsMap.get(instance.getClass()).hasFor(PreDestroy.class)) {
                managedInstances.add(instance);
            }
        }
    }

    private void startInstance(Object obj)
            throws IllegalAccessException, InvocationTargetException
    {
        if (listener != null) {
            listener.startingInstance(obj);
        }
        LifeCycleMethods methods = methodsMap.get(obj.getClass());
        for (Method postConstruct : methods.methodsFor(PostConstruct.class)) {
            if (listener != null) {
                listener.postConstructingInstance(obj, postConstruct);
            }
            postConstruct.invoke(obj);
        }
    }
}
