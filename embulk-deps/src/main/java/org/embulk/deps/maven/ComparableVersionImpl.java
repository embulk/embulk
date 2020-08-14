package org.embulk.deps.maven;

public final class ComparableVersionImpl extends org.embulk.deps.maven.ComparableVersion {
    public ComparableVersionImpl(final String version) {
        this.version = new org.apache.maven.artifact.versioning.ComparableVersion(version);
    }

    @Override
    public int compareTo(final org.embulk.deps.maven.ComparableVersion other) {
        if (other instanceof ComparableVersionImpl) {
            return this.version.compareTo(((ComparableVersionImpl) other).getNative());
        }
        return this.version.compareTo(new org.apache.maven.artifact.versioning.ComparableVersion(other.toString()));
    }

    @Override
    public String toString() {
        return this.version.toString();
    }

    org.apache.maven.artifact.versioning.ComparableVersion getNative() {
        return this.version;
    }

    private final org.apache.maven.artifact.versioning.ComparableVersion version;
}
