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

package org.embulk.guess.bzip2;

import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.spi.Buffer;
import org.embulk.spi.GuessPlugin;
import org.embulk.util.config.ConfigMapperFactory;

public class Bzip2GuessPlugin implements GuessPlugin {
    @Override
    public ConfigDiff guess(final ConfigSource config, final Buffer sample) {
        final byte[] header = new byte[10];
        sample.getBytes(0, header, 0, 10);

        final ConfigDiff configDiff = CONFIG_MAPPER_FACTORY.newConfigDiff();

        // magic: BZ
        // version: 'h' = bzip2
        // blocksize: 1 .. 9
        // block magic: 0x314159265359 (6 bytes)
        if (header[0] == (byte) 'B'
                && header[1] == (byte) 'Z'
                && header[2] == (byte) 'h'
                && (byte) '1' <= header[3] && header[3] <= (byte) '9'
                && header[4] == (byte) 0x31
                && header[5] == (byte) 0x41
                && header[6] == (byte) 0x59
                && header[7] == (byte) 0x26
                && header[8] == (byte) 0x53
                && header[9] == (byte) 0x59) {
            final ConfigDiff typeBzip2 = CONFIG_MAPPER_FACTORY.newConfigDiff();
            typeBzip2.set("type", "bzip2");
            final ConfigDiff[] decoders = new ConfigDiff[1];
            decoders[0] = typeBzip2;
            configDiff.set("decoders", decoders);
        }

        return configDiff;
    }

    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY = ConfigMapperFactory.builder().addDefaultModules().build();
}
