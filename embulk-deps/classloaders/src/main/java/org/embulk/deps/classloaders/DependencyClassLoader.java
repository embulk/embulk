package org.embulk.deps;

import java.security.SecureClassLoader;

public abstract class DependencyClassLoader extends SecureClassLoader {
    public DependencyClassLoader(final ClassLoader parentClassLoader) {
        super(parentClassLoader);
    }
}
