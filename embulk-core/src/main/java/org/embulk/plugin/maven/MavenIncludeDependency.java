package org.embulk.plugin.maven;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class MavenIncludeDependency {
    private MavenIncludeDependency(final String groupId, final String artifactId, final String version, final String classifier) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        if (classifier != null && classifier.isEmpty()) {
            this.classifier = null;
        } else {
            this.classifier = classifier;
        }
    }

    public static MavenIncludeDependency of(
            final String groupId,
            final String artifactId,
            final String version,
            final String classifier) {
        if (artifactId == null || groupId == null || version == null) {
            throw new NullPointerException("\"groupId\", \"artifactId\", and \"version\" must be present.");
        }
        return new MavenIncludeDependency(groupId, artifactId, version, classifier);
    }

    public static MavenIncludeDependency fromString(final String declaration) {
        if (declaration == null) {
            throw new NullPointerException("Maven artifact declaration must not be null.");
        }
        final String[] parts = declaration.split(":");  // "com.example:name:1.2.3[:classifier]"
        if (parts.length != 3 && parts.length != 4) {
            throw new IllegalArgumentException("Invalid Maven artifact declaration: " + declaration);
        }
        return new MavenIncludeDependency(parts[0], parts[1], parts[2], parts.length == 4 ? parts[3] : null);
    }

    public Map<String, String> asMap() {
        final LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        map.put("groupId", this.groupId);
        map.put("artifactId", this.artifactId);
        map.put("version", this.version);
        if (this.classifier != null) {
            map.put("classifier", this.classifier);
        }
        return Collections.unmodifiableMap(map);
    }

    public String getGroupId() {
        return this.groupId;
    }

    public String getArtifactId() {
        return this.artifactId;
    }

    public String getVersion() {
        return this.version;
    }

    public Optional<String> getClassifier() {
        return Optional.ofNullable(this.classifier);
    }

    public MavenExcludeDependency toMavenExcludeDependency() {
        return MavenExcludeDependency.of(this.groupId, this.artifactId, this.classifier);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(this.groupId, this.artifactId, this.version, this.classifier);
    }

    @Override
    public boolean equals(final Object otherObject) {
        if (!(otherObject instanceof MavenIncludeDependency)) {
            return false;
        }
        final MavenIncludeDependency other = (MavenIncludeDependency) otherObject;
        return Objects.equals(this.groupId, other.groupId)
                && Objects.equals(this.artifactId, other.artifactId)
                && Objects.equals(this.version, other.version)
                && Objects.equals(this.classifier, other.classifier);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(this.groupId);
        builder.append(":");
        builder.append(this.artifactId);
        builder.append(":");
        builder.append(this.version);
        if (this.classifier != null) {
            builder.append(":");
            builder.append(this.classifier);
        }
        return builder.toString();
    }

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String classifier;  // |classifier| can be null.
}
