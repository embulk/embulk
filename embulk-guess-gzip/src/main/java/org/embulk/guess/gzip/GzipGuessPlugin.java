/*
 * Copyright 2021 The Embulk project
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

package org.embulk.guess.gzip;

import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.spi.Buffer;
import org.embulk.spi.GuessPlugin;
import org.embulk.util.config.ConfigMapperFactory;

public class GzipGuessPlugin implements GuessPlugin {
    @Override
    public ConfigDiff guess(final ConfigSource config, final Buffer sample) {
        final byte[] header = new byte[2];
        sample.getBytes(0, header, 0, 2);

        final ConfigDiff configDiff = CONFIG_MAPPER_FACTORY.newConfigDiff();

        if (header[0] == (byte) 0x1f && header[1] == (byte) 0x8b) {
            final ConfigDiff typeGzip = CONFIG_MAPPER_FACTORY.newConfigDiff();
            typeGzip.set("type", "gzip");
            final ConfigDiff[] decoders = new ConfigDiff[1];
            decoders[0] = typeGzip;
            configDiff.set("decoders", decoders);
        }

        return configDiff;
    }

    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY = ConfigMapperFactory.builder().addDefaultModules().build();
}
