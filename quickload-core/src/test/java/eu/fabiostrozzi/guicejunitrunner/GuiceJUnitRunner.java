/**
 * Copyright (C) 2011 Fabio Strozzi (fabio.strozzi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// GuiceJunitRunner.java, created by Fabio Strozzi on Mar 27, 2011
package eu.fabiostrozzi.guicejunitrunner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * A JUnit class runner for Guice based applications.
 *
 * @author Fabio Strozzi
 */
public class GuiceJUnitRunner extends BlockJUnit4ClassRunner {
    private Injector injector;

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    public @interface GuiceModules {
        Class<?>[] value();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.junit.runners.BlockJUnit4ClassRunner#createTest()
     */
    @Override
    public Object createTest() throws Exception {
        Object obj = super.createTest();
        injector.injectMembers(obj);
        return obj;
    }

    /**
     * Instances a new JUnit runner.
     *
     * @param klass
     *            The test class
     * @throws InitializationError
     */
    public GuiceJUnitRunner(Class<?> klass) throws InitializationError {
        super(klass);
        Class<?>[] classes = getModulesFor(klass);
        injector = createInjectorFor(classes);
    }

    /**
     * @param classes
     * @return
     * @throws InitializationError
     */
    private Injector createInjectorFor(Class<?>[] classes) throws InitializationError {
        Module[] modules = new Module[classes.length];
        for (int i = 0; i < classes.length; i++) {
            try {
                modules[i] = (Module) (classes[i]).newInstance();
            } catch (InstantiationException e) {
                throw new InitializationError(e);
            } catch (IllegalAccessException e) {
                throw new InitializationError(e);
            }
        }
        return Guice.createInjector(modules);
    }

    /**
     * Gets the Guice modules for the given test class.
     *
     * @param klass
     *            The test class
     * @return The array of Guice {@link Module} modules used to initialize the
     *         injector for the given test.
     * @throws InitializationError
     */
    private Class<?>[] getModulesFor(Class<?> klass) throws InitializationError {
        GuiceModules annotation = klass.getAnnotation(GuiceModules.class);
        if (annotation == null)
            throw new InitializationError(
                    "Missing @GuiceModules annotation for unit test '" + klass.getName()
                            + "'");
        return annotation.value();
    }

}