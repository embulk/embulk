/*
 * Copyright 2022 The Embulk project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.embulk.spi.json;

import java.util.Objects;

/**
 * Represents a string in JSON.
 *
 * <p>It represents the string as a {@link String}, which is the same as Embulk's {@code STRING} column type.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc8259">RFC 8259 - The JavaScript Object Notation (JSON) Data Interchange Format</a>
 *
 * @since 0.10.42
 */
public final class JsonString implements JsonValue {
    private JsonString(final String value, final String literal) {
        this.value = value;
        this.literal = literal;
    }

    /**
     * Returns a JSON string that is represented by the specified {@link String}.
     *
     * @param value  the string
     * @return a JSON string represented by the specified {@link String}
     *
     * @since 0.10.42
     */
    public static JsonString of(final String value) {
        return new JsonString(value, null);
    }

    /**
     * Returns a JSON string that is represented by the specified {@link String}, with the specified JSON literal.
     *
     * <p>The literal is just subsidiary information used when stringifying this JSON string as JSON by {@link #toJson()}.
     *
     * @param value  the string
     * @param literal  the JSON literal of the string
     * @return a JSON string represented by the specified {@link String}
     *
     * @since 0.10.42
     */
    public static JsonString withLiteral(final String value, final String literal) {
        return new JsonString(value, literal);
    }

    /**
     * Returns {@link JsonValue.EntityType#STRING}, which is the entity type of {@link JsonString}.
     *
     * @return {@link JsonValue.EntityType#STRING}, which is the entity type of {@link JsonString}
     *
     * @since 0.10.42
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.STRING;
    }

    /**
     * Returns this value as {@link JsonString}.
     *
     * @return itself as {@link JsonString}
     *
     * @since 0.10.42
     */
    @Override
    public JsonString asJsonString() {
        return this;
    }

    /**
     * Returns the approximate size of this JSON string in bytes presumed to occupy in {@link org.embulk.spi.Page} as a reference.
     *
     * <p>This approximate size is used only as a threshold whether {@link org.embulk.spi.PageBuilder} is flushed, or not.
     * It is not accurate, it does not need to be accurate, and it is impossible in general to tell an accurate size that
     * a Java object occupies in the Java heap. But, a reasonable approximate would help to keep {@link org.embulk.spi.Page}
     * performant in the Java heap.
     *
     * <p>It is better to flush more frequently for bigger JSON value objects, less often for smaller JSON value objects,
     * but no infinite accumulation even for empty JSON value objects.
     *
     * @return the approximate size of this JSON string in bytes presumed to occupy in {@link org.embulk.spi.Page} as a reference
     *
     * @see "org.embulk.spi.PageBuilderImpl#addRecord"
     */
    @Override
    public int presumeReferenceSizeInBytes() {
        // Indeed, null is stored in Page as a special form as |nullBitSet|, but considered as 1 here in JsonValue just for ease.
        return this.value.length() * 2 + 4;
    }

    /**
     * Returns this JSON string as {@link String}.
     *
     * @return the {@link String} representation of this JSON string
     *
     * @since 0.10.42
     */
    public String getString() {
        return this.value;
    }

    /**
     * Returns this JSON string as {@link CharSequence}.
     *
     * @return the {@link CharSequence} representation of this JSON string
     *
     * @since 0.10.42
     */
    public CharSequence getChars() {
        return this.value;
    }

    /**
     * Returns the stringified JSON representation of this JSON string.
     *
     * <p>If this JSON string is created with a literal by {@link #withLiteral(String, String)}, it returns the literal. Otherwise,
     * it returns an escaped and quoted representation, which is valid as JSON, of this JSON string.
     *
     * @return the stringified JSON representation of this JSON string
     *
     * @since 0.10.42
     */
    @Override
    public String toJson() {
        if (this.literal != null) {
            return this.literal;
        }
        return escapeStringForJsonLiteral(this.value).toString();
    }

    /**
     * Returns the string representation of this JSON string.
     *
     * <p>It returns an escaped and quoted representation like {@code "string"}, which is valid as JSON, of this JSON string.
     *
     * <p>It does not consider a literal by {@link #withLiteral(String, String)} unlike {@link #toJson()}.
     *
     * @return the string representation of this JSON string
     *
     * @since 0.10.42
     */
    @Override
    public String toString() {
        return escapeStringForJsonLiteral(this.value).toString();
    }

    /**
     * Compares the specified object with this JSON string for equality.
     *
     * @return {@code true} if the specified object is equal to this JSON string
     *
     * @since 0.10.42
     */
    @Override
    public boolean equals(final Object otherObject) {
        if (this == otherObject) {
            return true;
        }

        // Check by `instanceof` in case against unexpected arbitrary extension of JsonValue.
        if (!(otherObject instanceof JsonString)) {
            return false;
        }

        final JsonString other = (JsonString) otherObject;

        return Objects.equals(this.value, other.value);
    }

    /**
     * Returns the hash code value for this JSON string.
     *
     * @return the hash code value for this JSON string
     *
     * @since 0.10.42
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(this.value);
    }

    static void appendEscapedStringForJsonLiteral(final String original, final StringBuilder builder) {
        if (original == null) {
            return;
        }
        if (original.isEmpty()) {
            builder.append("\"\"");
            return;
        }

        builder.append("\"");

        final int length = original.length();
        for (int i = 0; i < length; i++) {
            final char current = original.charAt(i);
            switch (current) {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '"':
                    builder.append("\\\"");
                    break;
                case '\b':  // 0008
                    builder.append("\\b");
                    break;
                case '\f':  // 000c
                    builder.append("\\f");
                    break;
                case '\n':  // 000a
                    builder.append("\\n");
                    break;
                case '\r':  // 000d
                    builder.append("\\r");
                    break;
                case '\t':  // 0009
                    builder.append("\\t");
                    break;
                case '\0':
                case '\u0001':
                case '\u0002':
                case '\u0003':
                case '\u0004':
                case '\u0005':
                case '\u0006':
                case '\u0007':
                case '\u000b':
                case '\u000e':
                case '\u000f':
                case '\u0010':
                case '\u0011':
                case '\u0012':
                case '\u0013':
                case '\u0014':
                case '\u0015':
                case '\u0016':
                case '\u0017':
                case '\u0018':
                case '\u0019':
                case '\u001a':
                case '\u001b':
                case '\u001c':
                case '\u001d':
                case '\u001e':
                case '\u001f':
                    builder.append("\\u00");
                    final String hex = Integer.toHexString(current);
                    builder.append("00", 0, 2 - hex.length());
                    builder.append(hex);
                    break;
                default:
                    builder.append(current);
            }
        }

        builder.append("\"");
    }

    private static String escapeStringForJsonLiteral(final String original) {
        final StringBuilder builder = new StringBuilder();
        appendEscapedStringForJsonLiteral(original, builder);
        return builder.toString();
    }

    private final String value;
    private final String literal;
}
