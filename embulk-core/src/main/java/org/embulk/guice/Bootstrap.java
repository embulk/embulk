/*
 * Copyright 2015 Sadayuki Furuhashi
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
package org.embulk.guice;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.annotations.Beta;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.Injector;
import java.util.Arrays;
import java.util.List;

@Beta
public class Bootstrap
{
    private List<Module> modules;

    private boolean requireExplicitBindings = true;

    private boolean started;

    public Bootstrap(Module... modules)
    {
        this(Arrays.asList(modules));
    }

    public Bootstrap(Iterable<? extends Module> modules)
    {
        this.modules = ImmutableList.copyOf(modules);
    }

    public Bootstrap requireExplicitBindings(boolean requireExplicitBindings)
    {
        this.requireExplicitBindings = requireExplicitBindings;
        return this;
    }

    public Bootstrap addModules(Module... additionalModules)
    {
        return addModules(Arrays.asList(additionalModules));
    }

    public Bootstrap addModules(Iterable<? extends Module> additionalModules)
    {
        modules = ImmutableList.copyOf(Iterables.concat(modules, additionalModules));
        return this;
    }

    //public Bootstrap onPreDestroy()
    //{
    //}

    //public Bootstrap onPreDestroyException()
    //{
    //}

    //public Bootstrap onStop()
    //{
    //}

    //public Bootstrap forEachModule(Consumer<? super Module> function)
    //{
    //    for (Module module : modules) {
    //        function.accept(module);
    //    }
    //    return this;
    //}

    //public <T> Bootstrap forEachModule(Class<T> ifClass, Consumer<? super T> function)
    //{
    //    for (Module module : modules) {
    //        if (ifClass.instance(module) {
    //            function.accept(module);
    //        }
    //    }
    //    return this;
    //}

    public Bootstrap overrideModules(Function<? super List<Module>, ? extends Iterable<? extends Module>> function)
    {
        modules = ImmutableList.copyOf(function.apply(modules));
        return this;
    }

    public Injector initialize()
    {
        Injector injector = start();
        injector.getInstance(LifeCycleManager.class).destroyOnShutdownHook();
        return injector;
    }

    public CloseableInjector initializeCloseable()
    {
        Injector injector = start();
        return new CloseableInjectorProxy(injector, injector.getInstance(LifeCycleManager.class));
    }

    private Injector start()
    {
        if (started) {
            throw new IllegalStateException("System already initialized");
        }
        started = true;

        ImmutableList.Builder<Module> moduleList = ImmutableList.builder();

        moduleList.addAll(modules);

        moduleList.add(new Module()
        {
            @Override
            public void configure(Binder binder)
            {
                binder.disableCircularProxies();
                if (requireExplicitBindings) {
                    binder.requireExplicitBindings();
                }
            }
        });

        moduleList.add(new LifeCycleModule());

        Injector injector = Guice.createInjector(Stage.PRODUCTION, moduleList.build());

        LifeCycleManager lifeCycleManager = injector.getInstance(LifeCycleManager.class);
        if (lifeCycleManager.size() > 0) {
            lifeCycleManager.start();
        }

        return injector;
    }
}
