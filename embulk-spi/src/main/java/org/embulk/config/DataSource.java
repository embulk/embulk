/*
 * Copyright 2014 The Embulk project
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

package org.embulk.config;

import java.util.List;
import java.util.Map;

/**
 * Base interface of {@link ConfigSource}, {@link ConfigDiff}, {@link TaskSource}, and {@link TaskReport}.
 *
 * <p>It had two following methods that return Jackson's instances till v0.9, but they are removed so that
 * Jackson on the core side can be hidden from plugins.
 *
 * <ul>
 * <li>{@code Iterable<Map.Entry<String, JsonNode>> getAttributes()}
 * <li>{@code ObjectNode getObjectNode()}
 * </ul>
 *
 * @since 0.4.0
 */
public interface DataSource {
    /**
     * Returns a {@link java.util.List} of attribute names under this node.
     *
     * @return a {@link java.util.List} of attribute names
     *
     * @since 0.4.0
     */
    List<String> getAttributeNames();

    /**
     * Returns {@code true} if it is empty.
     *
     * @return {@code true} if it is empty
     *
     * @since 0.4.0
     */
    boolean isEmpty();

    /**
     * Returns {@code true} if it has an attribute named {@code attrName}.
     *
     * @param attrName  name of the attribute to look for
     * @return {@code true} if it has an attribute named {@code attrName}
     *
     * @since 0.6.21
     */
    boolean has(String attrName);

    /**
     * Returns {@code true} if it has a list attribute named {@code attrName}.
     *
     * <p>Do not use this method if you want your plugin to keep working with Embulk v0.9. A plugin that calls it
     * would work only on Embulk v0.10.41 or later.
     *
     * <p>An implementation of {@link DataSource} should implement its own {@code hasList} in its own efficient way.
     * The {@code default} implementation in {@link DataSource} is a "polyfill". It works, but in an inefficient way.
     *
     * @param attrName  name of the list attribute to look for
     * @return {@code true} if it has a list attribute named {@code attrName}
     *
     * @since 0.10.41
     */
    default boolean hasList(final String attrName) {
        if (!this.has(attrName)) {
            return false;
        }

        try {
            // If #get(List.class, attrName) succeeds, it as a list attribute at |attrName|.
            // Note that it is an inefficient way to check.
            this.get(List.class, attrName);
        } catch (final Exception ex) {
            return false;
        }
        return true;
    }

    /**
     * Returns {@code true} if it has a nested attribute named {@code attrName}.
     *
     * <p>Do not use this method if you want your plugin to keep working with Embulk v0.9. A plugin that calls it
     * would work only on Embulk v0.10.41 or later.
     *
     * <p>An implementation of {@link DataSource} should implement its own {@code hasNested} in its own efficient way.
     * The {@code default} implementation in {@link DataSource} is a "polyfill". It works, but in an inefficient way.
     *
     * @param attrName  name of the nested attribute to look for
     * @return {@code true} if it has a nested attribute named {@code attrName}
     *
     * @since 0.10.41
     */
    default boolean hasNested(String attrName) {
        if (!this.has(attrName)) {
            return false;
        }

        try {
            // If #getNested(attrName) succeeds, it as a nested attribute at |attrName|.
            // Note that it is an inefficient way to check.
            this.getNested(attrName);
        } catch (final Exception ex) {
            return false;
        }
        return true;
    }

    /**
     * Returns an attribute value of {@code attrName} as {@code type}.
     *
     * @param <E>   the class to get the value as
     * @param type  the class to get the value as
     * @param attrName  name of the attribute to look for
     * @return the attribute value of {@code attrName} as {@code type}
     *
     * @since 0.4.0
     */
    <E> E get(Class<E> type, String attrName);

    /**
     * Returns an attribute value of {@code attrName} as {@code type}.
     *
     * @param <E>   the class to get the value as
     * @param type  the class to get the value as
     * @param attrName  name of the attribute to look for
     * @param defaultValue  the default value in case a value does not exist for {@code attrName}
     * @return the attribute value of {@code attrName} as {@code type}
     *
     * @since 0.4.0
     */
    <E> E get(Class<E> type, String attrName, E defaultValue);

    /**
     * Returns listed attribute values under {@code attrName} as {@code List<type>}.
     *
     * <p>It returns an empty list if the attribute does not exist. It does not throw an exception for such a case
     * unlike {@link #getNested(String)}. It is because an empty list is observed often in the wild, and users have
     * to take care in representing an empty list in YAML (explicit {@code "[]"}).
     *
     * <p>Do not use this method if you want your plugin to keep working with Embulk v0.9. A plugin that calls it
     * would work only on Embulk v0.10.41 or later.
     *
     * <p>An implementation of {@link DataSource} should implement its own {@code getListOf} in its own appropriate way.
     * The {@code default} implementation in {@link DataSource} is a "polyfill". It somehow works, but is not perfect.
     * It does not check nor convert the type of each element in the list.
     *
     * @param <E>   the class to get the attribute values in {@link java.util.List} as
     * @param type  the class to get the attribute values in {@link java.util.List} as
     * @param attrName  name of the listed attribute values to look for
     * @return the listed attribute values under {@code attrName}
     *
     * @since 0.10.41
     */
    @SuppressWarnings("unchecked")
    default <E> List<E> getListOf(Class<E> type, String attrName) {
        return (List<E>) this.get(List.class, attrName);
    }

    /**
     * Returns a nested value under {@code attrName}.
     *
     * @param attrName  name of the nested attribute to look for
     * @return the nested attribute value of {@code attrName}
     *
     * @since 0.4.0
     */
    DataSource getNested(String attrName);

    /**
     * Returns a nested value under {@code attrName}. Sets an empty object node {@code "{}"} there if it is empty.
     *
     * @param attrName  name of the nested attribute to look for
     * @return the nested attribute value of {@code attrName}
     *
     * @since 0.4.0
     */
    DataSource getNestedOrSetEmpty(String attrName);

    /**
     * Returns a nested value under {@code attrName}. Returns an empty object node {@code "{}"} there if it is empty.
     *
     * @param attrName  name of the nested attribute to look for
     * @return the nested attribute value of {@code attrName}
     *
     * @since 0.7.5
     */
    DataSource getNestedOrGetEmpty(String attrName);

    /**
     * Sets a value for {@code attrName}.
     *
     * @param attrName  name of the attribute to set the value at
     * @param v  the value {@link java.lang.Object} to set
     * @return itself after the value is set
     *
     * @since 0.4.0
     */
    DataSource set(String attrName, Object v);

    /**
     * Sets a nested value for {@code attrName}.
     *
     * @param attrName  name of the attribute to set the nested value at
     * @param v  the nested value to set
     * @return itself after the nested value is set
     *
     * @since 0.4.0
     */
    DataSource setNested(String attrName, DataSource v);

    /**
     * Sets all attributes in {@code other} into itself.
     *
     * @param other  the other {@link org.embulk.config.DataSource} to set
     * @return itself after the attributes are set
     *
     * @since 0.4.0
     */
    DataSource setAll(DataSource other);

    /**
     * Removes the attribute named {@code attrName}.
     *
     * @param attrName  name of the attribute to remove
     * @return itself after the attribute is removed
     *
     * @since 0.6.14
     */
    DataSource remove(String attrName);

    /**
     * Creates a deep copy of itself.
     *
     * @return the new {@link org.embulk.config.DataSource} instance that is deep-copied from itself
     *
     * @since 0.4.0
     */
    DataSource deepCopy();

    /**
     * Merges another {@link org.embulk.config.DataSource} into itself.
     *
     * @param other  the other {@link org.embulk.config.DataSource} to merge
     * @return itself after the other {@link org.embulk.config.DataSource} is merged
     *
     * @since 0.4.0
     */
    DataSource merge(DataSource other);

    /**
     * Returns a JSON representation of itself.
     *
     * @return its JSON representation in {@link java.lang.String}
     *
     * @since 0.10.3
     */
    default String toJson() {
        throw new UnsupportedOperationException(
                "ConfigSource#toJson is not implemented with: " + this.getClass().getCanonicalName());
    }

    /**
     * Returns a {@link java.util.Map} representation of itself.
     *
     * <p>Do not use this method if you want your plugin to keep working with Embulk v0.9. A plugin that calls it
     * would work only on Embulk v0.10.41 or later.
     *
     * @return its {@link java.util.Map} representation
     *
     * @since 0.10.41
     */
    default Map<String, Object> toMap() {
        throw new UnsupportedOperationException(
                "ConfigSource#toMap is not implemented with: " + this.getClass().getCanonicalName());
    }
}
