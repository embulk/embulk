/*
 * Copyright 2014 The Embulk project
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

package org.embulk.spi.type;

/**
 * Singleton type class for Embulk's LONG.
 *
 * <p>Plugins should not refer this class directly. Recommended to use constants in {@link Types} instead.
 *
 * @since 0.4.0
 */
@SuppressWarnings("deprecation")
public class LongType extends AbstractType {
    static final LongType LONG = new LongType();

    private LongType() {
        super("long", long.class, 8);
    }
}
