package org.embulk.config;

import java.util.List;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.resolver.Resolver;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.nodes.NodeId;

public class YamlTagResolver
    extends Resolver
{
    // Resolver converts a node (scalar, sequence, map, or !!tag with them)
    // to a tag (INT, FLOAT, STR, SEQ, MAP, ...). For example, converting
    // "123" (scalar) to 123 (INT), or "true" (scalar) to true (BOOL).
    // This is called by snakeyaml Composer which converts parser events
    // into an object.
    //
    // jackson-dataformat-yaml doesn't use this because it traverses parser
    // events without using Composer.

    public static final Pattern FLOAT_EXCEPTING_ZERO_START = Pattern
        .compile("^([-+]?(\\.[0-9]+|[1-9][0-9_]*(\\.[0-9_]*)?)([eE][-+]?[0-9]+)?|[-+]?[0-9][0-9_]*(?::[0-5]?[0-9])+\\.[0-9_]*|[-+]?\\.(?:inf|Inf|INF)|\\.(?:nan|NaN|NAN))$");

    @Override
    public void addImplicitResolver(Tag tag, Pattern regexp, String first)
    {
        // This method is called by constructor through addImplicitResolvers
        // to setup default implicit resolvers.

        if (tag.equals(Tag.FLOAT)) {
            super.addImplicitResolver(Tag.FLOAT, FLOAT_EXCEPTING_ZERO_START, "-+0123456789.");
        }
        else if (tag.equals(Tag.BOOL)) {
            // use stricter rule (reject 'On', 'Off', 'Yes', 'No')
            super.addImplicitResolver(Tag.BOOL, Pattern.compile("^(?:[Tt]rue|[Ff]alse)$"), "TtFf");
        }
        else if (tag.equals(Tag.TIMESTAMP)) {
            // This solves some unexpected behavior that snakeyaml
            // deserializes "2015-01-01 00:00:00" to java.util.Date
            // but jackson serializes java.util.Date to an integer.
            return;
        }
        else {
            super.addImplicitResolver(tag, regexp, first);
        }
    }

    @Override
    public Tag resolve(NodeId kind, String value, boolean implicit)
    {
        return super.resolve(kind, value, implicit);  // checks implicit resolvers
    }
}
