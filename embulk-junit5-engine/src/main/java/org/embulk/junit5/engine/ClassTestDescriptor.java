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

package org.embulk.junit5.engine;

import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

final class ClassTestDescriptor extends AbstractTestDescriptor {
    ClassTestDescriptor(final UniqueId uniqueId, final Class<?> testClass) {
        super(uniqueId, testClass.getSimpleName());
        this.testClass = testClass;
    }

    @Override
    public Type getType() {
        return Type.CONTAINER;
    }

    private final Class<?> testClass;
}
