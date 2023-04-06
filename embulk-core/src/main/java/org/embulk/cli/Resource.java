package org.embulk.cli;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.CodeSigner;
import java.util.jar.Manifest;

final class Resource {
    Resource(
            final String name,
            final SelfContainedJarFile jarFile,
            final int begin,
            final int end,
            final CodeSigner[] codeSigners) {
        this.name = name;
        this.jarFile = jarFile;
        this.begin = begin;
        this.end = end;
        this.codeSigners = codeSigners;
    }

    URL getCodeSourceUrl() {
        return this.jarFile.getCodeSourceUrl();
    }

    URL buildJarEmbeddedUrl() {
        try {
            return new URL(
                    "jar+embedded",
                    "",
                    -1,
                    this.getCodeSourceUrl().toString()
                            + "!" + (this.name.startsWith("/") ? "" : "/")
                            + this.name,
                    new JarEmbeddedUrlStreamHandler(this.jarFile.getInnerResourcesBinary(), this.begin, this.end));
        } catch (final MalformedURLException ex) {
            System.err.println("Failed to build an internal resource URL unexpectedly.");
            ex.printStackTrace();
            return null;
        }
    }

    Manifest getManifest() {
        return this.jarFile.getManifest();
    }

    ByteBuffer getAdjustedByteBuffer() {
        final ByteBuffer buffer = this.jarFile.getInnerResourcesBinary();

        // limit() must be set before position().
        // position() satisfies the condition |newPosition| must be less than the current limit.
        buffer.limit(this.end);
        buffer.position(this.begin);
        return buffer;
    }

    CodeSigner[] getCodeSigners() {
        return this.codeSigners;
    }

    @Override
    public String toString() {
        return "[" + this.name + ":" + this.begin + "-" + this.end + "]";
    }

    private final String name;
    private final SelfContainedJarFile jarFile;
    private final int begin;
    private final int end;
    private final CodeSigner[] codeSigners;
}
