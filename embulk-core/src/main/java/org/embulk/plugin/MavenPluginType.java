package org.embulk.plugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.embulk.EmbulkSystemProperties;
import org.embulk.plugin.maven.MavenExcludeDependency;
import org.embulk.plugin.maven.MavenIncludeDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MavenPluginType extends PluginType {
    private MavenPluginType(
            final String name,
            final String group,
            final String classifier,
            final String version,
            final Set<MavenExcludeDependency> excludeDependencies,
            final Set<MavenIncludeDependency> includeDependencies) {
        super("maven", name);
        this.group = group;
        this.classifier = classifier;
        this.version = version;

        if (excludeDependencies != null) {
            this.excludeDependencies = Collections.unmodifiableSet(new LinkedHashSet<>(excludeDependencies));
        } else {
            this.excludeDependencies = Collections.emptySet();
        }
        if (includeDependencies != null) {
            this.includeDependencies = Collections.unmodifiableSet(new LinkedHashSet<>(includeDependencies));
        } else {
            this.includeDependencies = Collections.emptySet();
        }

        final StringBuilder fullNameBuilder = new StringBuilder();
        fullNameBuilder.append("maven:");
        fullNameBuilder.append(group);
        fullNameBuilder.append(":");
        fullNameBuilder.append(name);
        fullNameBuilder.append(":");
        fullNameBuilder.append(version);
        if (classifier != null) {
            fullNameBuilder.append(":");
            fullNameBuilder.append(classifier);
        }
        this.fullName = fullNameBuilder.toString();

        final HashMap<String, String> fullMapMutable = new HashMap<String, String>();
        fullMapMutable.put("source", "maven");
        fullMapMutable.put("name", name);
        fullMapMutable.put("group", group);
        fullMapMutable.put("version", version);
        this.fullMap = Collections.unmodifiableMap(fullMapMutable);
    }

    public static MavenPluginType create(
            final String name, final String group, final String classifier, final String version) {
        return create(name, group, classifier, version, null, null);
    }

    public static MavenPluginType create(
            final String name,
            final String group,
            final String classifier,
            final String version,
            final Set<MavenExcludeDependency> excludeDependencies,
            final Set<MavenIncludeDependency> includeDependencies) {
        if (name == null || group == null || version == null) {
            throw new NullPointerException("\"name\", \"group\" and \"version\" must be present.");
        }
        return new MavenPluginType(name, group, classifier, version, excludeDependencies, includeDependencies);
    }

    public static MavenPluginType createFromDefaultPluginType(
            final String prefix,
            final String category,
            final DefaultPluginType defaultPluginType,
            final EmbulkSystemProperties embulkSystemProperties) {
        final String propertyName = prefix + category + "." + defaultPluginType.getName();
        final String declaration = embulkSystemProperties.getProperty(propertyName);
        if (declaration == null) {
            logger.info("Embulk system property \"{}\" is not set.", propertyName);
            return null;
        }

        final String[] parts = declaration.split(":");  // "maven:com.example:name:1.2.3[:classifier]"
        if ((parts.length != 4 && parts.length != 5) || !"maven".equals(parts[0])) {
            logger.warn("Embulk system property \"{}\" is invalid: \"{}\"", propertyName, declaration);
            return null;
        }

        if (parts.length == 5) {
            return new MavenPluginType(parts[2], parts[1], parts[4], parts[3], null, null);
        }
        return new MavenPluginType(parts[2], parts[1], null, parts[3], null, null);
    }

    public final Map<String, String> getJsonValue() {
        return this.fullMap;
    }

    public final String getGroup() {
        return this.group;
    }

    public final String getArtifactId(final String category) {
        return "embulk-" + category + "-" + this.getName();
    }

    public final String getClassifier() {
        return this.classifier;
    }

    public final String getVersion() {
        return this.version;
    }

    public final Set<MavenExcludeDependency> getExcludeDependencies() {
        return this.excludeDependencies;
    }

    public final Set<MavenIncludeDependency> getIncludeDependencies() {
        return this.includeDependencies;
    }

    public final String getFullName() {
        return this.fullName;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(getSourceType(), getName(), this.group, this.classifier, this.version, this.excludeDependencies, this.includeDependencies);
    }

    @Override
    public boolean equals(Object objectOther) {
        if (!(objectOther instanceof MavenPluginType)) {
            return false;
        }
        MavenPluginType other = (MavenPluginType) objectOther;
        return Objects.equals(this.getSourceType(), other.getSourceType())
                && Objects.equals(this.getName(), other.getName())
                && Objects.equals(this.group, other.group)
                && Objects.equals(this.classifier, other.classifier)
                && Objects.equals(this.version, other.version)
                && Objects.equals(this.excludeDependencies, other.excludeDependencies)
                && Objects.equals(this.includeDependencies, other.includeDependencies);
    }

    @Override
    public String toString() {
        return this.fullName;
    }

    private static final Logger logger = LoggerFactory.getLogger(MavenPluginType.class);

    private final String group;
    private final String classifier;  // |classifier| can be null.
    private final String version;

    private final Set<MavenExcludeDependency> excludeDependencies;
    private final Set<MavenIncludeDependency> includeDependencies;

    private final String fullName;
    private final Map<String, String> fullMap;
}
