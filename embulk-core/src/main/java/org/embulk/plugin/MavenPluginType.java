package org.embulk.plugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.embulk.EmbulkSystemProperties;

public final class MavenPluginType extends PluginType {
    private MavenPluginType(final String name, final String group, final String classifier, final String version) {
        super("maven", name);
        this.group = group;
        this.classifier = classifier;
        this.version = version;

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
        if (name == null || group == null || version == null) {
            throw new NullPointerException("\"name\", \"group\" and \"version\" must be present.");
        }
        return new MavenPluginType(name, group, classifier, version);
    }

    public static MavenPluginType createFromDefaultPluginType(
            final String category,
            final DefaultPluginType defaultPluginType,
            final EmbulkSystemProperties embulkSystemProperties)
            throws PluginSourceNotMatchException {
        final String propertyName = "plugins." + category + "." + defaultPluginType.getName();
        final String declaration = embulkSystemProperties.getProperty(propertyName);
        if (declaration == null) {
            throw new PluginSourceNotMatchException("Embulk system property \"" + propertyName + "\" is not set.");
        }

        final String[] parts = declaration.split(":");  // "maven:com.example:name:1.2.3[:classifier]"
        if ((parts.length != 4 && parts.length != 5) || !"maven".equals(parts[0])) {
            throw new PluginSourceNotMatchException(
                    "Embulk system property \"" + propertyName + "\" is invalid: \"" + declaration + "\"");
        }

        if (parts.length == 5) {
            return new MavenPluginType(parts[2], parts[1], parts[4], parts[3]);
        }
        return new MavenPluginType(parts[2], parts[1], null, parts[3]);
    }

    public final Map<String, String> getJsonValue() {
        return this.fullMap;
    }

    public final String getGroup() {
        return this.group;
    }

    public final String getClassifier() {
        return this.classifier;
    }

    public final String getVersion() {
        return this.version;
    }

    public final String getFullName() {
        return this.fullName;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(getSourceType(), getName(), this.group, this.classifier, this.version);
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
                && Objects.equals(this.version, other.version);
    }

    @Override
    public String toString() {
        return this.fullName;
    }

    private final String group;
    private final String classifier;  // |classifier| can be null.
    private final String version;
    private final String fullName;
    private final Map<String, String> fullMap;
}
