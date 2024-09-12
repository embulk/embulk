/*
 * Copyright 2024 The Embulk project
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

package org.embulk.plugin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestPluginClassLoader {
    @Test
    public void testIsParentFirstPackage() {
        assertFalse(PluginClassLoader.isParentFirstPackage(""));
        assertFalse(PluginClassLoader.isParentFirstPackage("TopLevelClass"));
        assertFalse(PluginClassLoader.isParentFirstPackage("ch.qos.logbacker.Dummy"));
        assertFalse(PluginClassLoader.isParentFirstPackage("ch.qos.logback.classical.Dummy"));
        assertTrue(PluginClassLoader.isParentFirstPackage("ch.qos.logback.classic.LoggerContext"));
        assertTrue(PluginClassLoader.isParentFirstPackage("ch.qos.logback.classic.joran.JoranConfigurator"));
        assertTrue(PluginClassLoader.isParentFirstPackage("ch.qos.logback.core.Context"));
        assertTrue(PluginClassLoader.isParentFirstPackage("ch.qos.logback.core.hook.ShutdownHook"));
        assertFalse(PluginClassLoader.isParentFirstPackage("ch.qos.logback.coredo.Dummy"));
        assertTrue(PluginClassLoader.isParentFirstPackage("java.lang.Class"));
        assertTrue(PluginClassLoader.isParentFirstPackage("java.util.ArrayList"));
        assertFalse(PluginClassLoader.isParentFirstPackage("javax.xml.bind.JAXB"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulkdummy.DummyClass"));
        assertTrue(PluginClassLoader.isParentFirstPackage("org.embulk.EmbulkVersion"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.dummy.DummyClass"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.base.restclient.RestClientTaskBase"));
        assertTrue(PluginClassLoader.isParentFirstPackage("org.embulk.cli.CommandLine"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.client.DummyClient"));
        assertTrue(PluginClassLoader.isParentFirstPackage("org.embulk.config.Config"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.configure.DummyConfiguration"));
        assertTrue(PluginClassLoader.isParentFirstPackage("org.embulk.exec.BulkLoader"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.executor.DummyExecutor"));
        assertTrue(PluginClassLoader.isParentFirstPackage("org.embulk.jruby.JRubyClassLoader"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.jruby9.JRuby9ClassLoader"));
        assertTrue(PluginClassLoader.isParentFirstPackage("org.embulk.plugin.PluginManager"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.plugins.DummyPluginManager"));
        assertTrue(PluginClassLoader.isParentFirstPackage("org.embulk.spi.Exec"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.spike.Dummy"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.deps.Dummy"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.deps.buffer.PooledBufferAllocatorImpl"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.deps.buffering.Dummy"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.deps.cli.CommandWriters"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.deps.clients.Dummy"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.deps.config.TaskSerDe"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.deps.configure.DummySerDe"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.deps.json.JsonParserDelegateImpl"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.deps.jsonnet.JsonnetDummy"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.deps.maven.MavenResolutionException"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.deps.maven1.Maven1ResolutionException"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.deps.timestamp.TimestampFormatterDelegateImpl"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.deps.timestamper.Dummy"));
        assertTrue(PluginClassLoader.isParentFirstPackage("org.embulk.test.EmbulkTests"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.testing.EmbulkTester"));
        assertTrue(PluginClassLoader.isParentFirstPackage("org.embulk.junit5.EmbulkTest"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.junit51.EmbulkTest51"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.util.timestamp.TimestampFormatter"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.decoder.gzip.GzipDecoderPlugin"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.decoders.Dummy"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.encoder.gzip.GzipEncoderPlugin"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.encoders.Dummy"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.filter.rename.RenameFilterPlugin"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.filters.Dummy"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.formatter.csv.CsvFormatterPlugin"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.formatters.Dummy"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.guess.csv.CsvGuessPlugin"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.guessr.Dummy"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.input.file.FileInputPlugin"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.inputs.Dummy"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.output.file.FileOutputPlugin"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.outputs.Dummy"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.parser.csv.CsvParserPlugin"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.embulk.parsers.Dummy"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.msgpacker.Dummy"));
        assertTrue(PluginClassLoader.isParentFirstPackage("org.msgpack.core.MessageUnpacker"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.msgpack.cores.DummyPacker"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.msgpack.MessagePack"));
        assertTrue(PluginClassLoader.isParentFirstPackage("org.msgpack.value.ValueFactory"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.msgpack.values.DummyValuesFactory"));
        assertTrue(PluginClassLoader.isParentFirstPackage("org.slf4j.Logger"));
        assertTrue(PluginClassLoader.isParentFirstPackage("org.slf4j.simple.SimpleLogger"));
        assertFalse(PluginClassLoader.isParentFirstPackage("org.slf4j2.DummyLogger2"));
    }

    @Test
    public void testIsParentFirstPath() {
        assertFalse(PluginClassLoader.isParentFirstPath(""));
        assertFalse(PluginClassLoader.isParentFirstPath("foo/bar.txt"));
        assertFalse(PluginClassLoader.isParentFirstPath("ch/qos/logback/core/dummy.txt"));
        assertFalse(PluginClassLoader.isParentFirstPath("ch/qos/logback/classic/boolexe/Dummy"));
        assertTrue(PluginClassLoader.isParentFirstPath("ch/qos/logback/classic/boolex/IEvaluator"));
        assertFalse(PluginClassLoader.isParentFirstPath("ch/qos/logback/classic/boolex"));
        assertTrue(PluginClassLoader.isParentFirstPath("ch/qos/logback/classic/boolex/"));
        assertFalse(PluginClassLoader.isParentFirstPath("ch/qos/logback/classic/db/script"));
        assertTrue(PluginClassLoader.isParentFirstPath("ch/qos/logback/classic/db/script/"));
        assertTrue(PluginClassLoader.isParentFirstPath("ch/qos/logback/classic/db/script/postgresql.sql"));
        assertFalse(PluginClassLoader.isParentFirstPath("ch/qos/logback/classic/db/scripts/dummy.sql"));
        assertFalse(PluginClassLoader.isParentFirstPath("embulk"));
        assertTrue(PluginClassLoader.isParentFirstPath("embulk/"));
        assertTrue(PluginClassLoader.isParentFirstPath("embulk/logback-file.xml"));
        assertFalse(PluginClassLoader.isParentFirstPath("embulkdummy/logback-file.xml"));
        assertFalse(PluginClassLoader.isParentFirstPath("msgpack"));
        assertFalse(PluginClassLoader.isParentFirstPath("msgpack/"));
        assertFalse(PluginClassLoader.isParentFirstPath("msgpack/dummy.txt"));
        assertFalse(PluginClassLoader.isParentFirstPath("msgpacker/dummy.txt"));
        assertFalse(PluginClassLoader.isParentFirstPath("org/embulk"));
        assertTrue(PluginClassLoader.isParentFirstPath("org/embulk/"));
        assertTrue(PluginClassLoader.isParentFirstPath("org/embulk/jruby/bundler/template/Gemfile"));
        assertFalse(PluginClassLoader.isParentFirstPath("org/embulkdummy/jruby/bundler/template/Gemfile"));
    }
}
