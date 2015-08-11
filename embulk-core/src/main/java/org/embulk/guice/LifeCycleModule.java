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
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.inject.matcher.Matchers.any;

/**
 * Guice module for binding the LifeCycle manager
 */
public class LifeCycleModule implements Module
{
    private final List<Object> injectedInstances = Lists.newArrayList();
    private final LifeCycleMethodsMap lifeCycleMethodsMap = new LifeCycleMethodsMap();
    private final AtomicReference<LifeCycleManager> lifeCycleManagerRef = new AtomicReference<LifeCycleManager>(null);

    @Override
    public void configure(Binder binder)
    {
        binder.disableCircularProxies();

        binder.bindListener(any(), new TypeListener()
        {
            @Override
            public <T> void hear(TypeLiteral<T> type, TypeEncounter<T> encounter)
            {
                encounter.register(new InjectionListener<T>()
                {
                    @Override
                    public void afterInjection(T obj)
                    {
                        if (isLifeCycleClass(obj.getClass())) {
                            LifeCycleManager lifeCycleManager = lifeCycleManagerRef.get();
                            if (lifeCycleManager != null) {
                                try {
                                    lifeCycleManager.addInstance(obj);
                                }
                                catch (Exception e) {
                                    throw new Error(e);
                                }
                            }
                            else {
                                injectedInstances.add(obj);
                            }
                        }
                    }
                });
            }
        });
    }

    @Provides
    @Singleton
    public LifeCycleManager getServerManager()
            throws Exception
    {
        LifeCycleManager lifeCycleManager = new LifeCycleManager(injectedInstances, lifeCycleMethodsMap);
        lifeCycleManagerRef.set(lifeCycleManager);
        return lifeCycleManager;
    }

    private boolean isLifeCycleClass(Class<?> clazz)
    {
        LifeCycleMethods methods = lifeCycleMethodsMap.get(clazz);
        return methods.hasFor(PostConstruct.class) || methods.hasFor(PreDestroy.class);
    }
}
