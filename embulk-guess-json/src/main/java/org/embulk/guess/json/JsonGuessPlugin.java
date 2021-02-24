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

package org.embulk.guess.json;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.spi.Buffer;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.Exec;
import org.embulk.spi.GuessPlugin;
import org.embulk.util.config.ConfigMapperFactory;
import org.embulk.util.file.FileInputInputStream;
import org.embulk.util.file.InputStreamFileInput;
import org.embulk.util.json.JsonParseException;
import org.embulk.util.json.JsonParser;
import org.msgpack.value.Value;

public class JsonGuessPlugin implements GuessPlugin {
    @Override
    public ConfigDiff guess(final ConfigSource config, final Buffer sample) {
        final ConfigDiff configDiff = CONFIG_MAPPER_FACTORY.newConfigDiff();

        if (!"json".equals(config.getNestedOrGetEmpty("parser").get(String.class, "type", "json"))) {
            return configDiff;
        }

        final BufferAllocator bufferAllocator = Exec.getBufferAllocator();

        // Use org.embulk.spi.json.JsonParser to respond to multi-line Json
        final JsonParser.Stream jsonParser = newJsonParser(sample, bufferAllocator);

        boolean oneJsonParsed = false;
        try {
            Value v = null;
            while ((v = jsonParser.next()) != null) {
                // "v" needs to be JSON object type (isMapValue) because:
                // 1) Single-column CSV can be mis-guessed as JSON if JSON non-objects are accepted.
                // 2) JsonParserPlugin accepts only the JSON object type.
                if (!v.isMapValue()) {
                    throw new JsonParseException("v must be JSON object type");
                }
                oneJsonParsed = true;
            }
        } catch (final JsonParseException ex) {
            // the exception is ignored
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }

        if (oneJsonParsed) {
            // if JsonParser can parse even one JSON data
            final ConfigDiff typeJson = CONFIG_MAPPER_FACTORY.newConfigDiff();
            typeJson.set("type", "json");
            configDiff.set("parser", typeJson);
        }

        return configDiff;
    }

    private static JsonParser.Stream newJsonParser(final Buffer buffer, final BufferAllocator bufferAllocator) {
        final ArrayList<InputStream> inputStreams = new ArrayList<>();
        inputStreams.add(buildByteArrayInputStream(buffer));

        final InputStreamFileInput.IteratorProvider iteratorProvider = new InputStreamFileInput.IteratorProvider(inputStreams);

        final FileInputInputStream input = new FileInputInputStream(new InputStreamFileInput(bufferAllocator, iteratorProvider));
        input.nextFile();
        try {
            return (new JsonParser()).open(input);
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @SuppressWarnings("deprecation")  // For the use of Buffer#array.
    private static ByteArrayInputStream buildByteArrayInputStream(final Buffer buffer) {
        return new ByteArrayInputStream(buffer.array());
    }

    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY = ConfigMapperFactory.builder().addDefaultModules().build();
}
