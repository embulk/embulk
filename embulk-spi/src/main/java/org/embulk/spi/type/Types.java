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
 * Constants of Embulk's data types.
 *
 * @since 0.4.0
 */
public class Types {
    /**
     * @since 0.4.0
     */
    public static final BooleanType BOOLEAN = BooleanType.BOOLEAN;

    /**
     * @since 0.4.0
     */
    public static final LongType LONG = LongType.LONG;

    /**
     * @since 0.4.0
     */
    public static final DoubleType DOUBLE = DoubleType.DOUBLE;

    /**
     * @since 0.4.0
     */
    public static final StringType STRING = StringType.STRING;

    /**
     * @since 0.4.0
     */
    public static final TimestampType TIMESTAMP = TimestampType.TIMESTAMP;

    /**
     * @since 0.8.0
     */
    public static final JsonType JSON = JsonType.JSON;
}
