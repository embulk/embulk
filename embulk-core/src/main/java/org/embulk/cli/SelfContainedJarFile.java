package org.embulk.cli;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.LinkedHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

final class SelfContainedJarFile {
    SelfContainedJarFile(final String outerResourceName) {
        if (outerResourceName == null) {
            throw new NullPointerException("null specified for SelfContainedJarFile.");
        }

        if (CODE_SOURCE_URL_BASE == null) {
            System.err.println("org.embulk.cli.SelfContainedJarFile is loaded through invalid method or location.");
            this.codeSourceUrl = null;
            this.manifest = null;
            this.innerResources = null;
            this.innerResourcesBinary = null;
            return;
        }

        final URL codeSourceUrlBuilt;
        try {
            codeSourceUrlBuilt = new URL(
                    CODE_SOURCE_URL_BASE,
                    outerResourceName.startsWith("/") ? outerResourceName.substring(1) : outerResourceName);
        } catch (final MalformedURLException ex) {
            System.err.println("Invalid JAR resource: " + CODE_SOURCE_URL_BASE.toString() + " : " + outerResourceName);
            ex.printStackTrace();
            this.codeSourceUrl = null;
            this.manifest = null;
            this.innerResources = null;
            this.innerResourcesBinary = null;
            return;
        }

        final InputStream inputStream = SelfContainedJarFile.class.getResourceAsStream(outerResourceName);
        if (inputStream == null) {
            System.err.println("JAR resource not found: " + outerResourceName);
            this.codeSourceUrl = null;
            this.manifest = null;
            this.innerResources = null;
            this.innerResourcesBinary = null;
            return;
        }

        final JarInputStream jarInputStream;
        try {
            jarInputStream = new JarInputStream(inputStream, false);
        } catch (final IOException ex) {
            System.err.println("Invalid JAR format: " + outerResourceName);
            ex.printStackTrace();
            this.codeSourceUrl = null;
            this.manifest = null;
            this.innerResources = null;
            this.innerResourcesBinary = null;
            return;
        }

        final LinkedHashMap<String, Resource> innerResourcesBuilt = new LinkedHashMap<>();
        final ByteBuffer innerResourcesBinaryBuilt;
        try {
            innerResourcesBinaryBuilt = extract(outerResourceName, jarInputStream, this, innerResourcesBuilt);
        } catch (final IOException ex) {
            System.err.println("Failed to read JAR: " + outerResourceName);
            ex.printStackTrace();
            this.codeSourceUrl = null;
            this.manifest = null;
            this.innerResources = null;
            this.innerResourcesBinary = null;
            closeQuiet(jarInputStream);
            return;
        }

        this.codeSourceUrl = codeSourceUrlBuilt;
        this.manifest = jarInputStream.getManifest();
        this.innerResources = innerResourcesBuilt;
        this.innerResourcesBinary = innerResourcesBinaryBuilt;

        if ("true".equals(System.getProperty("org.embulk.trace_embedded_jar_resources"))) {
            System.err.println(
                    "Extracted an embedded JAR resource: ["
                    + this.codeSourceUrl.toString()
                    + "] ("
                    + this.innerResourcesBinary.capacity()
                    + " bytes)");
        }
    }

    private static void closeQuiet(final InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                // ignore.
            }
        }
    }

    URL getCodeSourceUrl() {
        return this.codeSourceUrl;
    }

    Manifest getManifest() {
        return this.manifest;
    }

    ByteBuffer getInnerResourcesBinary() {
        return this.innerResourcesBinary.asReadOnlyBuffer();
    }

    /**
     * Returns the resource object.
     *
     * @return resource if found, null otherwise
     */
    Resource getResource(final String resourceName) {
        if (this.innerResources == null) {
            return null;
        }
        return this.innerResources.get(resourceName);
    }

    private static ByteBuffer extract(
            final String outerResourceName,
            final JarInputStream jarInputStream,
            final SelfContainedJarFile jar,
            final LinkedHashMap<String, Resource> innerResources)
            throws IOException {
        final byte[] buffer = new byte[4096];
        final ByteArrayOutputStream innerResourcesBinaryOutputStream = new ByteArrayOutputStream();

        int index = 0;
        while (true) {
            final JarEntry jarEntry = (JarEntry) jarInputStream.getNextEntry();
            if (jarEntry == null) {
                break;  // End of embedded JAR
            }

            if (jarEntry.isDirectory()) {
                continue;
            }
            final String entryName = jarEntry.getName();
            if (innerResources.containsKey(entryName)) {
                throw new IOException(String.format("Duplicated in embedded JAR: %s/%s", outerResourceName, entryName));
            }

            final int lengthRead = (int) transfer(jarInputStream, innerResourcesBinaryOutputStream, buffer);
            innerResources.put(entryName, new Resource(entryName, jar, index, index + lengthRead, jarEntry.getCodeSigners()));
            index += lengthRead;
        }

        // TODO: Use ByteBuffer.allocateDirect so that it does not affect GC.
        return ByteBuffer.wrap(innerResourcesBinaryOutputStream.toByteArray()).asReadOnlyBuffer();
    }

    /**
     * Does the same with Java 9+'s {@code InputStream#transferTo}.
     */
    private static long transfer(final InputStream inputStream, final OutputStream outputStream, final byte[] buffer) throws IOException {
        long totalLength = 0L;
        while (true) {
            final int lengthRead = inputStream.read(buffer);
            if (lengthRead < 0) {
                break;
            }
            outputStream.write(buffer, 0, lengthRead);
            totalLength += lengthRead;
        }
        return totalLength;
    }

    // TODO: Apply the Initialization-on-demand holder idiom.
    static {
        final ProtectionDomain protectionDomain = SelfContainedJarFile.class.getProtectionDomain();
        final CodeSource codeSource = protectionDomain.getCodeSource();
        PROTECTION_DOMAIN = protectionDomain;
        CODE_SOURCE = codeSource;

        URL codeSourceUrlBase = null;
        boolean hasFailedInCodeSource = false;
        try {
            final URL codeSourceUrl = codeSource.getLocation();
            final String codeSourceProtocol = codeSourceUrl.getProtocol();
            if ("file".equals(codeSourceProtocol)) {
                codeSourceUrlBase = new URL("jar", "", -1, codeSourceUrl.toString() + "!/");
            } else if ("jar".equals(codeSourceProtocol)) {
                final URL innerUrl = new URL(codeSourceUrl.getPath());
                final String innerProtocol = innerUrl.getProtocol();
                if (!"file".equals(innerProtocol)) {
                    throw new MalformedURLException("Invalid CodeSource URL, non 'file:' in 'jar:': " + codeSourceUrl);
                }
                final String innerPath = innerUrl.getPath();
                if (innerPath.indexOf("!/") < innerPath.length() - 2) {  // If it contains "!/" not at last.
                    throw new MalformedURLException("Invalid CodeSource URL, 'jar:' does not end with '!/': " + codeSourceUrl);
                }
                final String innerUrlString = innerUrl.toString();
                codeSourceUrlBase = new URL("jar", "", -1, innerPath.endsWith("!/") ? innerUrlString : innerUrlString + "!/");
            } else {
                throw new MalformedURLException("Invalid CodeSource URL, neither 'file:' nor 'jar:': " + codeSourceUrl);
            }
        } catch (final Exception ex) {
            System.err.println("org.embulk.cli.SelfContainedJarFile is loaded through invalid method or location.");
            ex.printStackTrace();
            hasFailedInCodeSource = true;
        }

        if (hasFailedInCodeSource) {
            CODE_SOURCE_URL_BASE = null;
        } else {
            CODE_SOURCE_URL_BASE = codeSourceUrlBase;
        }
    }

    private static final ProtectionDomain PROTECTION_DOMAIN;
    private static final CodeSource CODE_SOURCE;
    private static final URL CODE_SOURCE_URL_BASE;

    private final URL codeSourceUrl;
    private final Manifest manifest;
    private final LinkedHashMap<String, Resource> innerResources;
    private final ByteBuffer innerResourcesBinary;
}
