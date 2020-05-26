/*
 * Copyright 2020 The Embulk project
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
 */
public interface DataSource {
    /**
     * Returns a {@link java.util.List} of attribute names under this node.
     *
     * @return a {@link java.util.List} of attribute names
     */
    List<String> getAttributeNames();

    /**
     * Returns {@code true} if it is empty.
     *
     * @return {@code true} if it is empty
     */
    boolean isEmpty();

    /**
     * Returns {@code true} if it has an attribute named {@code attrName}.
     *
     * @param attrName  name of the attribute to look for
     * @return {@code true} if it has an attribute named {@code attrName}
     */
    boolean has(String attrName);

    /**
     * Returns an attribute value of {@code attrName} as {@code type}.
     *
     * @param <E>   the class to get the value as
     * @param type  the class to get the value as
     * @param attrName  name of the attribute to look for
     * @return the attribute value of {@code attrName} as {@code type}
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
     */
    <E> E get(Class<E> type, String attrName, E defaultValue);

    /**
     * Returns a nested value under {@code attrName}.
     *
     * @param attrName  name of the nested attribute to look for
     * @return the nested attribute value of {@code attrName}
     */
    DataSource getNested(String attrName);

    /**
     * Returns a nested value under {@code attrName}. Sets an empty object node {@code "{}"} there if it is empty.
     *
     * @param attrName  name of the nested attribute to look for
     * @return the nested attribute value of {@code attrName}
     */
    DataSource getNestedOrSetEmpty(String attrName);

    /**
     * Returns a nested value under {@code attrName}. Returns an empty object node {@code "{}"} there if it is empty.
     *
     * @param attrName  name of the nested attribute to look for
     * @return the nested attribute value of {@code attrName}
     */
    DataSource getNestedOrGetEmpty(String attrName);

    /**
     * Sets a value for {@code attrName}.
     *
     * @param attrName  name of the attribute to set the value at
     * @param v  the value {@link java.lang.Object} to set
     * @return itself after the value is set
     */
    DataSource set(String attrName, Object v);

    /**
     * Sets a nested value for {@code attrName}.
     *
     * @param attrName  name of the attribute to set the nested value at
     * @param v  the nested value to set
     * @return itself after the nested value is set
     */
    DataSource setNested(String attrName, DataSource v);

    /**
     * Sets all attributes in {@code other} into itself.
     *
     * @param other  the other {@link org.embulk.config.DataSource} to set
     * @return itself after the attributes are set
     */
    DataSource setAll(DataSource other);

    /**
     * Removes the attribute named {@code attrName}.
     *
     * @param attrName  name of the attribute to remove
     * @return itself after the attribute is removed
     */
    DataSource remove(String attrName);

    /**
     * Creates a deep copy of itself.
     *
     * @return the new {@link org.embulk.config.DataSource} instance that is deep-copied from itself
     */
    DataSource deepCopy();

    /**
     * Merges another {@link org.embulk.config.DataSource} into itself.
     *
     * @param other  the other {@link org.embulk.config.DataSource} to merge
     * @return itself after the other {@link org.embulk.config.DataSource} is merged
     */
    DataSource merge(DataSource other);

    /**
     * Returns a JSON representation of itself.
     *
     * @return its JSON representation in {@link java.lang.String}
     */
    default String toJson() {
        throw new UnsupportedOperationException(
                "ConfigSource#toJson is not implemented with: " + this.getClass().getCanonicalName());
    }
}
