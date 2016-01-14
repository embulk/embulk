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

import java.lang.reflect.Method;

public interface LifeCycleListener
{
    /**
     * Called when state changes from LATENT to STARTING
     */
    void startingLifeCycle();

    /**
     * Called when state changes from STARTING to STARTED
     */
    void startedLifeCycle();

    /**
     * Called when state changes from STARTED to STOPPING
     */
    void stoppingLifeCycle();

    /**
     * Called when state changes from STOPPING to STOPPED
     */
    void stoppedLifeCycle();

    /**
     * Called when post construction of an object starts
     */
    void startingInstance(Object object);

    /**
     * Called when a post construction method of an object is called
     */
    void postConstructingInstance(Object object, Method postConstructMethod);

    /**
     * Called when pre destruction of an object starts
     */
    void stoppingInstance(Object object);

    /**
     * Called when a pre destruction method of an object is called
     */
    void preDestroyingInstance(Object object, Method preDestroyMethod);
}
