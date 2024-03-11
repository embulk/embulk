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

package org.embulk.plugin.maven;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import org.embulk.EmbulkDependencyClassLoader;

public abstract class MavenArtifactInstaller {
    public static MavenArtifactInstaller create(final Path localMavenRepositoryPath) throws FileNotFoundException {
        try {
            return CONSTRUCTOR.newInstance(localMavenRepositoryPath);
        } catch (final IllegalAccessException | IllegalArgumentException | InstantiationException ex) {
            throw new LinkageError("Dependencies for Maven are not loaded correctly: " + CLASS_NAME, ex);
        } catch (final InvocationTargetException ex) {
            final Throwable targetException = ex.getTargetException();
            if (targetException instanceof RuntimeException) {
                throw (RuntimeException) targetException;
            } else if (targetException instanceof Error) {
                throw (Error) targetException;
            } else if (targetException instanceof FileNotFoundException) {
                throw (FileNotFoundException) targetException;
            } else {
                throw new RuntimeException("Unexpected Exception in creating: " + CLASS_NAME, ex);
            }
        }
    }

    /**
     * Installs a Maven-based plugin in the local Maven repository.
     */
    public abstract void install(
            final String coords,
            final String... repositoryUrls) throws IOException;

    @SuppressWarnings("unchecked")
    private static Class<MavenArtifactInstaller> loadImplClass() {
        try {
            return (Class<MavenArtifactInstaller>) CLASS_LOADER.loadClass(CLASS_NAME);
        } catch (final ClassNotFoundException ex) {
            throw new LinkageError("Dependencies for Maven are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final ClassLoader CLASS_LOADER = EmbulkDependencyClassLoader.get();
    private static final String CLASS_NAME = "org.embulk.deps.maven.MavenArtifactInstallerImpl";

    static {
        final Class<MavenArtifactInstaller> clazz = loadImplClass();
        try {
            CONSTRUCTOR = clazz.getConstructor(Path.class);
        } catch (final NoSuchMethodException ex) {
            throw new LinkageError("Dependencies for Maven are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final Constructor<MavenArtifactInstaller> CONSTRUCTOR;
}
