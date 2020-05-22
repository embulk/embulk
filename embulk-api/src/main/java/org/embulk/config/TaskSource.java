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

/**
 * Represents an Embulk configuration for a plugin task.
 *
 * <p>It had two following methods that return Jackson's instances till v0.9, but they are removed so that
 * Jackson on the core side can be hidden from plugins.
 *
 * <ul>
 * <li>{@code Iterable<Map.Entry<String, JsonNode>> getAttributes()}
 * <li>{@code ObjectNode getObjectNode()}
 * </ul>
 */
public interface TaskSource extends DataSource {
    /**
     * Loads this configuration as a task class like {@code PluginTask}.
     *
     * <p>This method will be deprecated. Loading tasks will be replaced by another library
     * {@code embulk-util-config} on plugin's side.
     *
     * @param <T>   the task class to load this configuration as
     * @param taskType  the task class to load this configuration as
     * @return a task class instance loaded
     */
    default <T> T loadTask(Class<T> taskType) {
        throw new UnsupportedOperationException(
                "TaskSource#loadTask does not work with: " + this.getClass().getCanonicalName());
    }

    /**
     * Returns a nested value under {@code attrName}.
     *
     * @param attrName  name of the nested attribute to look for
     * @return the nested attribute value of {@code attrName}
     */
    @Override
    TaskSource getNested(String attrName);

    /**
     * Returns a nested value under {@code attrName}. Sets an empty object node {@code "{}"} there if it is empty.
     *
     * @param attrName  name of the nested attribute to look for
     * @return the nested attribute value of {@code attrName}
     */
    @Override
    TaskSource getNestedOrSetEmpty(String attrName);

    /**
     * Returns a nested value under {@code attrName}. Returns an empty object node {@code "{}"} there if it is empty.
     *
     * @param attrName  name of the nested attribute to look for
     * @return the nested attribute value of {@code attrName}
     */
    @Override
    TaskSource getNestedOrGetEmpty(String attrName);

    /**
     * Sets a value for {@code attrName}.
     *
     * @param attrName  name of the attribute to set the value at
     * @param v  the value {@link java.lang.Object} to set
     * @return itself after the value is set
     */
    @Override
    TaskSource set(String attrName, Object v);

    /**
     * Sets a nested value for {@code attrName}.
     *
     * @param attrName  name of the attribute to set the nested value at
     * @param v  the nested value to set
     * @return itself after the nested value is set
     */
    @Override
    TaskSource setNested(String attrName, DataSource v);

    /**
     * Sets all attributes in {@code other} into itself.
     *
     * @param other  the other {@link org.embulk.config.TaskSource} to set
     * @return itself after the attributes are set
     */
    @Override
    TaskSource setAll(DataSource other);

    /**
     * Removes the attribute named {@code attrName}.
     *
     * @param attrName  name of the attribute to remove
     * @return itself after the attribute is removed
     */
    @Override
    TaskSource remove(String attrName);

    /**
     * Creates a deep copy of itself.
     *
     * @return the new {@link org.embulk.config.TaskSource} instance that is deep-copied from itself
     */
    @Override
    TaskSource deepCopy();

    /**
     * Merges another {@link org.embulk.config.TaskSource} into itself.
     *
     * @param other  the other {@link org.embulk.config.TaskSource} to merge
     * @return itself after the other {@link org.embulk.config.TaskSource} is merged
     */
    @Override
    TaskSource merge(DataSource other);
}
