package org.embulk.plugin.maven;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class MavenExcludeDependency {
    private MavenExcludeDependency(final String groupId, final String artifactId, final String classifier) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        if (classifier != null && classifier.isEmpty()) {
            this.classifier = null;
        } else {
            this.classifier = classifier;
        }
    }

    public static MavenExcludeDependency of(
            final String groupId,
            final String artifactId,
            final String classifier) {
        if (artifactId == null || groupId == null) {
            throw new NullPointerException("\"groupId\" and \"artifactId\" must be present.");
        }
        return new MavenExcludeDependency(groupId, artifactId, classifier);
    }

    public static MavenExcludeDependency fromString(final String declaration) {
        if (declaration == null) {
            throw new NullPointerException("Maven artifact declaration must not be null.");
        }
        final String[] parts = declaration.split(":");  // "com.example:name[:classifier]"
        if (parts.length != 2 && parts.length != 3) {
            throw new IllegalArgumentException("Invalid Maven artifact declaration: " + declaration);
        }
        return new MavenExcludeDependency(parts[0], parts[1], parts.length == 3 ? parts[2] : null);
    }

    public Map<String, String> asMap() {
        final LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        map.put("groupId", this.groupId);
        map.put("artifactId", this.artifactId);
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

    public Optional<String> getClassifier() {
        return Optional.ofNullable(this.classifier);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(this.groupId, this.artifactId, this.classifier);
    }

    @Override
    public boolean equals(final Object otherObject) {
        if (!(otherObject instanceof MavenExcludeDependency)) {
            return false;
        }
        final MavenExcludeDependency other = (MavenExcludeDependency) otherObject;
        return Objects.equals(this.groupId, other.groupId)
                && Objects.equals(this.artifactId, other.artifactId)
                && Objects.equals(this.classifier, other.classifier);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(this.groupId);
        builder.append(":");
        builder.append(this.artifactId);
        if (this.classifier != null) {
            builder.append(":");
            builder.append(this.classifier);
        }
        return builder.toString();
    }

    private final String groupId;
    private final String artifactId;
    private final String classifier;  // |classifier| can be null.
}
