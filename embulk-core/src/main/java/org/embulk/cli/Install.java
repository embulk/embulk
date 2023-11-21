/*
 * Copyright 2023 The Embulk project
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

package org.embulk.cli;

import java.io.FileNotFoundException;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.embulk.EmbulkSystemProperties;
import org.embulk.plugin.maven.MavenArtifactInstaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Install {
    private Install(
            final String artifactCoords,
            final EmbulkSystemProperties embulkSystemProperties) {
        this.artifactCoords = artifactCoords;
        this.embulkSystemProperties = embulkSystemProperties;
    }

    static int install(
            final List<String> arguments,
            final EmbulkSystemProperties embulkSystemProperties)
            throws IOException {
        final Install install = new Install(arguments.get(0), embulkSystemProperties);
        install.install();
        return 0;
    }

    private void install() throws IOException {
        final MavenArtifactInstaller installer = MavenArtifactInstaller.create(this.getLocalMavenRepository());
        installer.install(this.artifactCoords);
    }

    private Path getLocalMavenRepository() throws IOException {
        // It expects the Embulk system property "m2_repo" is set from org.embulk.cli.EmbulkSystemPropertiesBuilder.
        final String m2Repo = this.embulkSystemProperties.getProperty("m2_repo", null);
        if (m2Repo == null) {
            throw new FileNotFoundException("Embulk system property \"m2_repo\" is not set properly.");
        }

        final Path path = Paths.get(m2Repo);

        final Path absolutePath;
        try {
            absolutePath = path.normalize().toAbsolutePath();
        } catch (final IOError ex) {
            throw new IOException(ex);
        } catch (final SecurityException ex) {
            throw new IOException(ex);
        }

        if (!Files.exists(absolutePath)) {
            logger.info("The path \"{}\" (m2_repo) does not exist. Creating it as a directory.", absolutePath);
            try {
                Files.createDirectories(absolutePath);
            } catch (final UnsupportedOperationException ex) {
                throw new IOException(ex);
            } catch (final SecurityException ex) {
                throw new IOException(ex);
            }
        } else if (!Files.isDirectory(absolutePath)) {
            logger.error("The path \"{}\" (m2_repo) is not a directory.", absolutePath);
            throw new IOException("The path \"" + absolutePath + "\" is not a directory.");
        }

        return absolutePath;
    }


    private static final Logger logger = LoggerFactory.getLogger(Install.class);

    private final String artifactCoords;
    private final EmbulkSystemProperties embulkSystemProperties;
}
