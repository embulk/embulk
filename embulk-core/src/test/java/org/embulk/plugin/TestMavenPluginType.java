/*
 * Copyright 2022 The Embulk project
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.embulk.EmbulkTestRuntime;
import org.embulk.plugin.PluginClassLoader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestMavenPluginType {
    @Test
    public void testEquals() throws Exception {
        final MavenPluginType type1 = MavenPluginType.create("some", "org.embulk", null, "0.3.3");
        final MavenPluginType type2 = MavenPluginType.create("some", "org.embulk", null, "0.3.3", null, null);
        System.out.println(type1.hashCode());
        System.out.println(type2.hashCode());
        System.out.println(type1.equals(type2));
        System.out.println(type2.equals(type1));
    }
}
