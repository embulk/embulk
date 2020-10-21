package org.embulk.cli;

import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

final class CliManifest {
    private CliManifest() {
        // No instantiation.
    }

    static Manifest getManifest() {
        return Holder.INSTANCE;
    }

    private static class Holder {  // Initialization-on-demand holder idiom.
        static final Manifest INSTANCE = getManifest();

        private static Manifest getManifest() {
            try {
                final ProtectionDomain protectionDomain;
                try {
                    protectionDomain = CliManifest.class.getProtectionDomain();
                } catch (final SecurityException ex) {
                    System.err.println("Manifest unavailable: ProtectionDomain inaccessible.");
                    ex.printStackTrace();
                    return null;
                }

                final CodeSource codeSource = protectionDomain.getCodeSource();
                if (codeSource == null) {
                    System.err.println("Manifest unavailable: CodeSource inaccessible.");
                    return null;
                }

                final URL selfJarUrl = codeSource.getLocation();
                if (selfJarUrl == null) {
                    System.err.println("Manifest unavailable: location unavailable from CodeSource.");
                    return null;
                } else if (!selfJarUrl.getProtocol().equals("file")) {
                    System.err.println("Manifest unavailable: invalid location: " + selfJarUrl.toString());
                    return null;
                }

                final String selfJarPathString = selfJarUrl.getPath();
                if (selfJarPathString == null) {
                    System.err.println("Manifest unavailable: path unavailable in CodeSource location: " + selfJarUrl.toString());
                    return null;
                } else if (selfJarPathString.isEmpty()) {
                    System.err.println("Manifest unavailable: empty path from CodeSource location: " + selfJarUrl.toString());
                    return null;
                }

                try (final JarFile selfJarFile = new JarFile(selfJarPathString)) {
                    try {
                        return selfJarFile.getManifest();
                    } catch (final IllegalStateException ex) {
                        System.err.println("Manifest unavailable: JAR closed unexpectedly.");
                        ex.printStackTrace();
                        return null;
                    } catch (final IOException ex) {
                        System.err.println("Manifest unavailable: I/O error in reading manifest.");
                        ex.printStackTrace();
                        return null;
                    }
                } catch (final SecurityException ex) {
                    System.err.println("Manifest unavailable: JAR file inaccessible.");
                    ex.printStackTrace();
                    return null;
                } catch (final IOException ex) {
                    System.err.println("Manifest unavailable: I/O error in reading JAR.");
                    ex.printStackTrace();
                    return null;
                }
            } catch (final Throwable ex) {
                System.err.println("Manifest unavailable: unknown error.");
                ex.printStackTrace();
                return null;
            }
        }
    }
}
