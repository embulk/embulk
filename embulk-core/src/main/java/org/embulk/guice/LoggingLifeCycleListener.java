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

import java.util.logging.Logger;
import java.lang.reflect.Method;

public class LoggingLifeCycleListener implements LifeCycleListener
{
    private final Logger log;

    public LoggingLifeCycleListener(Logger log)
    {
        this.log = log;
    }

    public LoggingLifeCycleListener()
    {
        this.log = Logger.getLogger(LifeCycleManager.class.getName());
    }

    @Override
    public void startingLifeCycle()
    {
        log.fine("Life cycle starting...");
    }

    @Override
    public void startedLifeCycle()
    {
        log.fine("Life cycle startup complete. System ready.");
    }

    @Override
    public void stoppingLifeCycle()
    {
        log.fine("Life cycle stopping...");
    }

    @Override
    public void stoppedLifeCycle()
    {
        log.fine("Life cycle stopped.");
    }

    @Override
    public void startingInstance(Object obj)
    {
        log.fine("Starting " + obj.getClass().getName());
    }

    @Override
    public void postConstructingInstance(Object obj, Method postConstruct)
    {
        log.fine("\t" + postConstruct.getName() + "()");
    }

    @Override
    public void stoppingInstance(Object obj)
    {
        log.fine("Stopping " + obj.getClass().getName());
    }

    @Override
    public void preDestroyingInstance(Object obj, Method preDestroy)
    {
        log.fine("\t" + preDestroy.getName() + "()");
    }
}
