/*
 * Copyright 2023 The Embulk project
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

package org.embulk.junit5;

import java.lang.reflect.Method;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

public class PluginClassLoaderExtension implements InvocationInterceptor {
    @Override
    public void interceptTestMethod(
            final InvocationInterceptor.Invocation<Void> invocation,
            final ReflectiveInvocationContext<Method> invocationContext,
            final ExtensionContext extensionContext)
            throws Throwable {
        System.out.println("hoge");
        System.out.println(invocationContext.getExecutable());
        invocation.proceed();
        // intercept(invocation, invocationContext, extensionContext);
    }

    /*
    @Override
    public void interceptTestTemplateMethod(Invocation<Void> invocation,
                                            ReflectiveInvocationContext<Method> invocationContext,
                                            ExtensionContext extensionContext) throws Throwable {
        intercept(invocation, invocationContext, extensionContext);
    }

    @Override
    public void interceptTestMethod(InvocationInterceptor.Invocation<Void> invocation,
                                    ReflectiveInvocationContext<Method> invocationContext,
                                    ExtensionContext extensionContext) {
        intercept(invocation, invocationContext, extensionContext);
    }

    private void intercept(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) {
        invocation.skip();

        BuildTool buildTool = BuildToolLocator.locate(extensionContext);
        Classpath classpath = supplyClasspath(invocationContext, extensionContext, buildTool);
        BuildToolLocator.store(extensionContext, classpath.buildTool);

        invokeMethodWithModifiedClasspath(invocationContext, classpath);
    }

    protected abstract Classpath supplyClasspath(ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext, BuildTool buildTool);

    private void invokeMethodWithModifiedClasspath(ReflectiveInvocationContext<Method> invocationContext, Classpath classpath) {
        ClassLoader modifiedClassLoader = classpath.newClassLoader();

        ClassLoader currentThreadPreviousClassLoader = replaceCurrentThreadClassLoader(modifiedClassLoader);
        String previousClassPathProperty = replaceClassPathProperty(classpath);

        try {
            invokeMethodWithModifiedClasspath(
                invocationContext.getExecutable().getDeclaringClass().getName(),
                invocationContext.getExecutable().getName(),
                modifiedClassLoader);
        } finally {
            System.setProperty(Classpath.SYSTEM_PROPERTY, previousClassPathProperty);
            Thread.currentThread().setContextClassLoader(currentThreadPreviousClassLoader);
        }
    }

    private ClassLoader replaceCurrentThreadClassLoader(ClassLoader modifiedClassLoader) {
        ClassLoader currentThreadPreviousClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(modifiedClassLoader);
        return currentThreadPreviousClassLoader;
    }

    private String replaceClassPathProperty(Classpath classpath) {
        String previousClassPathProperty = System.getProperty(Classpath.SYSTEM_PROPERTY);
        System.setProperty(Classpath.SYSTEM_PROPERTY, classpath.pathElements.stream().map(PathElement::toString).collect(Collectors.joining(File.pathSeparator)));
        return previousClassPathProperty;
    }

    private void invokeMethodWithModifiedClasspath(String className, String methodName, ClassLoader classLoader) {
        final Class<?> testClass;
        try {
            testClass = classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot load test class [" + className + "] from modified classloader, verify that you did not exclude a path containing the test", e);
        }

        Object testInstance = ReflectionUtils.newInstance(testClass);
        final Optional<Method> method = ReflectionUtils.findMethod(testClass, methodName);
        ReflectionUtils.invokeMethod(
            method.orElseThrow(() -> new IllegalStateException("No test method named " + methodName)),
            testInstance);
    }
    */

    private final ExtensionContext.Namespace namespace = ExtensionContext.Namespace.create(PluginClassLoaderExtension.class);
}
