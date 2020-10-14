/*
 * Copyright 2015 The Embulk project
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

package org.embulk.spi;

import org.embulk.config.ConfigException;

/**
 * Represents an Exception caused by schema config errors.
 *
 * @since 0.6.14
 */
public class SchemaConfigException extends ConfigException {
    /**
     * @since 0.6.14
     */
    public SchemaConfigException(final String message) {
        super(message);
    }

    /**
     * @since 0.6.14
     */
    public SchemaConfigException(final Throwable cause) {
        super(cause);
    }

    /**
     * @since 0.6.14
     */
    public SchemaConfigException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
