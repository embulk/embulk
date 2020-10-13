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

package org.embulk.config;

/**
 * Represents an Exception caused by user's configuration error.
 *
 * @since 0.4.0
 */
public class ConfigException extends RuntimeException implements UserDataException {
    protected ConfigException() {
        super();
    }

    /**
     * Constructs a new {@link ConfigException} with the specified detail message.
     *
     * @param message  the detail message
     *
     * @since 0.4.0
     */
    public ConfigException(final String message) {
        super(message);
    }

    /**
     * Constructs a new {@link ConfigException} with the specified cause and a detail message of the cause.
     *
     * @param cause  the cause
     *
     * @since 0.4.0
     */
    public ConfigException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@link ConfigException} with the specified detail message and cause.
     *
     * @param message  the detail message
     * @param cause  the cause
     *
     * @since 0.4.0
     */
    public ConfigException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
