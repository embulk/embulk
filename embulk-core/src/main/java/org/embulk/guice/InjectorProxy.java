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

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.MembersInjector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeConverterBinding;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;

abstract class InjectorProxy
        implements Injector
{
    protected abstract Injector injector();

    @Override
    public void injectMembers(Object instance)
    {
        injector().injectMembers(instance);
    }

    @Override
    public <T> MembersInjector<T> getMembersInjector(TypeLiteral<T> typeLiteral)
    {
        return injector().getMembersInjector(typeLiteral);
    }

    @Override
    public <T> MembersInjector<T> getMembersInjector(Class<T> type)
    {
        return injector().getMembersInjector(type);
    }

    @Override
    public Map<Key<?>, Binding<?>> getBindings()
    {
        return injector().getBindings();
    }

    @Override
    public Map<Key<?>, Binding<?>> getAllBindings()
    {
        return injector().getAllBindings();
    }

    @Override
    public <T> Binding<T> getBinding(Key<T> key)
    {
        return injector().getBinding(key);
    }

    @Override
    public <T> Binding<T> getBinding(Class<T> type)
    {
        return injector().getBinding(type);
    }

    @Override
    public <T> Binding<T> getExistingBinding(Key<T> key)
    {
        return injector().getExistingBinding(key);
    }

    @Override
    public <T> List<Binding<T>> findBindingsByType(TypeLiteral<T> type)
    {
        return injector().findBindingsByType(type);
    }

    @Override
    public <T> Provider<T> getProvider(Key<T> key)
    {
        return injector().getProvider(key);
    }

    @Override
    public <T> Provider<T> getProvider(Class<T> type)
    {
        return injector().getProvider(type);
    }

    @Override
    public <T> T getInstance(Key<T> key)
    {
        return injector().getInstance(key);
    }

    @Override
    public <T> T getInstance(Class<T> type)
    {
        return injector().getInstance(type);
    }

    @Override
    public Injector getParent()
    {
        return injector().getParent();
    }

    @Override
    public Injector createChildInjector(Iterable<? extends Module> modules)
    {
        return injector().createChildInjector(modules);
    }

    @Override
    public Injector createChildInjector(Module... modules)
    {
        return injector().createChildInjector(modules);
    }

    @Override
    public Map<Class<? extends Annotation>, Scope> getScopeBindings()
    {
        return injector().getScopeBindings();
    }

    @Override
    public Set<TypeConverterBinding> getTypeConverterBindings()
    {
        return injector().getTypeConverterBindings();
    }
}
