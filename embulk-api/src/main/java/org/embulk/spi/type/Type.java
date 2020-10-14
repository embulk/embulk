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
 * Represents Embulk's data type.
 *
 * @since 0.4.0
 */
public interface Type {
    /**
     * Returns the name of the Embulk data type.
     *
     * @return the name of the Embulk data type
     *
     * @since 0.4.0
     */
    String getName();

    /**
     * Returns the Java type of the internal value in the Embulk data type.
     *
     * @deprecated It would be removed in Embulk v0.10 or v0.11. Do not use it.
     *
     * @return the Java type of the internal value in the Embulk data type
     *
     * @since 0.4.0
     */
    @Deprecated
    Class<?> getJavaType();

    /**
     * Returns the size of the internal value in the Embulk data type.
     *
     * @deprecated It would be removed in Embulk v0.10 or v0.11. Do not use it.
     *
     * @return the size of the internal value in the Embulk data type
     *
     * @since 0.4.0
     */
    @Deprecated
    byte getFixedStorageSize();
}
