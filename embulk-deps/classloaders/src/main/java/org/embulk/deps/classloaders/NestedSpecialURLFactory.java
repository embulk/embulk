package org.embulk.deps.classloaders;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.UnknownServiceException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Generates a special URL for a resource in a nested JAR, which is combined with its special URLStreamHandler.
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
class NestedSpecialURLFactory {
    private NestedSpecialURLFactory(final URL topLevelJarFileUrl, final String specialProtocol) {
        this.topLevelJarFileUrl = topLevelJarFileUrl;
        this.specialProtocol = specialProtocol;

        this.urlStreamHandler = new SpecialURLStreamHandler();
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    static NestedSpecialURLFactory ofFileURL(
            final URL topLevelFileUrl,
            final String specialProtocol)
            throws MalformedURLException {
        // Given |topLevelFileUrl| must be a "file:" URL such as "file:///path/to/top.jar".
        if (!"file".equals(topLevelFileUrl.getProtocol())) {
            throw new MalformedURLException(
                    String.format("Invalid URL: protocol must be \"file:\": %s", topLevelFileUrl.toString()));
        }

        // Creating a "jar:file:" URL such as "jar:file:///path/to/top.jar!/".
        // See Javadoc of JarURLConnection: https://docs.oracle.com/javase/8/docs/api/java/net/JarURLConnection.html
        final URL topLevelJarFileUrl = new URL("jar", "", -1, topLevelFileUrl.toString() + "!/");

        return new NestedSpecialURLFactory(topLevelJarFileUrl, specialProtocol);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    static NestedSpecialURLFactory ofFileURL(
            final URL topLevelFileUrl)
            throws MalformedURLException {
        return ofFileURL(topLevelFileUrl, "jar");  // Can be dangerous!
    }

    URL create(
            final String resourceNameOfJarInJar,
            final String resourceNameInJarInJar)
            throws MalformedURLException {
        // Creating a "jar:file:" URL such as "jar:file:///path/to/top.jar!/another/path/to/contained.jar".
        final URL urlOfJarInJar = new URL(
                this.topLevelJarFileUrl,
                resourceNameOfJarInJar.startsWith("/") ? resourceNameOfJarInJar.substring(1) : resourceNameOfJarInJar);

        // Creating a final "special:jar:file:" URL such as
        //
        //   "special:jar:file:///path/to/top.jar!/another/path/to/contained.jar!/more/path/to/resource.file"
        //
        // with URLStreamHandler combined to process it into InputStream.
        return new URL(
                this.specialProtocol,
                "",
                -1,
                urlOfJarInJar.toString()
                        + "!" + (resourceNameInJarInJar.startsWith("/") ? "" : "/")
                        + resourceNameInJarInJar,
                this.urlStreamHandler);
    }

    /**
     * Implements a special URLStreamHandler to be coupled with URLs created by NestedSpecialURLFactory.
     *
     * <p>Its URL handling is specifically coupled with how {@code NestedSpecialURLFactory#create} builds a URL.
     * This class should be {@code private} so that it is not used apart from {@code NestedSpecialURLFactory}.
     */
    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    private class SpecialURLStreamHandler extends URLStreamHandler {
        @Override
        protected URLConnection openConnection(final URL url) throws IOException {
            // Note that declaring variables here may cause unexpected behaviors.
            // https://stackoverflow.com/questions/9952815/s3-java-client-fails-a-lot-with-premature-end-of-content-length-delimited-messa
            return new URLConnection(url) {
                @Override
                public void connect() {
                    // Do nothing.
                }

                /**
                 * Breaks down the special URL built by NestedSpecialURLFactory, and creates InputStream to read from.
                 */
                @Override
                public InputStream getInputStream() throws IOException {
                    final URL specialUrlOfResourceInJarInJar = getURL();
                    if (!specialUrlOfResourceInJarInJar.getProtocol().equals(specialProtocol)) {
                        throw new IOException(String.format(
                                "FATAL: Unexpected URL is combined with URLStreamHandler: \"%s\" is against \"%s\"",
                                specialUrlOfResourceInJarInJar,
                                specialProtocol));
                    }

                    // Extracts "jar:file:///path/to/top.jar!/path2/to/contained.jar!/path3/to/resource.file"
                    // from "special:jar:file:///path/to/top.jar!/path2/to/contained.jar!/path3/to/resource.file"
                    final String urlOfJarInJarWithResource = specialUrlOfResourceInJarInJar.getPath();

                    final int indexOfLastSeparator = urlOfJarInJarWithResource.lastIndexOf("!/");
                    if (indexOfLastSeparator < 0) {
                        throw new IOException(String.format(
                                "FATAL: No resource separator in URL: %s", specialUrlOfResourceInJarInJar));
                    }

                    // Breaks "jar:file:///path/to/top.jar!/path2/to/contained.jar!/path3/to/resource.file" down into
                    // 1. "jar:file:///path/to/top.jar!/path2/to/contained.jar"
                    // 2. "/path3/to/resource.file"
                    final URL urlOfJarInJar = new URL(urlOfJarInJarWithResource.substring(0, indexOfLastSeparator));
                    final String resourceNameInJarInJar = urlOfJarInJarWithResource.substring(indexOfLastSeparator + 1);

                    // Opens a URLConnection from "jar:file:///path/to/top.jar!/path2/to/contained.jar"
                    //
                    // Note that the Connection is released once the InputStream (below) is closed.
                    //
                    // "Invoking the close() methods on the InputStream or OutputStream of an URLConnection after a
                    // request may free network resources associated with this instance, unless particular protocol
                    // specifications specify different behaviours for it."
                    // See: https://docs.oracle.com/javase/8/docs/api/java/net/URLConnection.html
                    final JarURLConnection jarInJarConnection;
                    try {
                        final URLConnection urlConnection = urlOfJarInJar.openConnection();
                        jarInJarConnection = (JarURLConnection) urlConnection;
                    } catch (IOException ex) {
                        throw new IOException(
                                String.format("Failed to open a JAR resource in JAR: %s", urlOfJarInJar), ex);
                    } catch (ClassCastException ex) {
                        throw new IOException(
                                String.format("FATAL: URLConnection from \"%s\" URL is not JarURLConnection.",
                                              urlOfJarInJar), ex);
                    }

                    // Gets InputStream from "jar:file:///path/to/top.jar!/path2/to/contained.jar"
                    final InputStream inputStreamFromJarInJar;
                    try {
                        inputStreamFromJarInJar = jarInJarConnection.getInputStream();
                    } catch (UnknownServiceException ex) {
                        throw new IOException(
                                String.format("FATAL: Input from URL \"%s\" is unexpectedly not supported.",
                                              urlOfJarInJar), ex);
                    } catch (IOException ex) {
                        throw new IOException(
                                String.format("Failed to read a JAR resource in JAR: %s", urlOfJarInJar), ex);
                    }

                    // Parses "jar:file:///path/to/top.jar!/path2/to/contained.jar" as a JAR
                    final JarInputStream jarInputStreamFromJarInJar;
                    try {
                        jarInputStreamFromJarInJar = new JarInputStream(inputStreamFromJarInJar);
                    } catch (IOException ex) {
                        throw new IOException(
                                String.format("Failed in parsing a JAR resource in JAR: %s", urlOfJarInJar), ex);
                    }

                    // Looks for "/path3/to/resource.file" in "jar:file:///path/to/top.jar!/path2/to/contained.jar"
                    JarEntry jarEntry;
                    try {
                        jarEntry = jarInputStreamFromJarInJar.getNextJarEntry();
                    } catch (IOException ex) {
                        throw new IOException(
                                String.format("Failed in reading a resource in JAR in JAR: %s", urlOfJarInJar), ex);
                    }
                    while (jarEntry != null) {
                        if (jarEntry.getName().equals(resourceNameInJarInJar)) {
                            // |jarInputStreamFromJarInJar| points "/path3/to/resource.file" at the moment.
                            return jarInputStreamFromJarInJar;
                        }
                        try {
                            jarEntry = jarInputStreamFromJarInJar.getNextJarEntry();
                        } catch (IOException ex) {
                            throw new IOException(
                                    String.format("Failed in reading a resource in JAR in JAR: %s", urlOfJarInJar), ex);
                        }
                    }
                    return null;
                }
            };
        }
    }

    private final URL topLevelJarFileUrl;
    private final String specialProtocol;

    private final SpecialURLStreamHandler urlStreamHandler;
}
