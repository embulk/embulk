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

package org.embulk.spi;

import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.spi.Buffer;

/**
 * The main class that a Guess Plugin implements.
 *
 * <p>It guesses a configuration {@link org.embulk.config.ConfigDiff} from a byte sequence {@link org.embulk.spi.Buffer}.
 */
public interface GuessPlugin {
    /**
     * Performs the guess.
     *
     * @param config  a configuration for the guess plugin given from a user
     * @param sample  a sample data to guess
     * @return a new configuration guessed based on {@code config} and {@code sample}
     */
    ConfigDiff guess(ConfigSource config, Buffer sample);
}
