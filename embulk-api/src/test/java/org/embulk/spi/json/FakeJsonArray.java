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

import java.util.AbstractList;
import java.util.Arrays;

public final class FakeJsonArray extends AbstractList<JsonValue> implements JsonValue {
    private FakeJsonArray(final JsonValue[] values) {
        this.values = values;
    }

    public static FakeJsonArray of(final JsonValue... values) {
        if (values.length == 0) {
            return EMPTY;
        }
        return new FakeJsonArray(Arrays.copyOf(values, values.length));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ARRAY;
    }

    @Override
    public int presumeReferenceSizeInBytes() {
        int sum = 0;
        for (int i = 0; i < this.values.length; i++) {
            sum += this.values[i].presumeReferenceSizeInBytes();
        }
        return sum;
    }

    @Override
    public int size() {
        return this.values.length;
    }

    @Override
    public JsonValue get(final int index) {
        return this.values[index];
    }

    @Override
    public String toJson() {
        if (this.values.length == 0) {
            return "[]";
        }

        final StringBuilder builder = new StringBuilder();
        builder.append("[");
        builder.append(this.values[0].toJson());
        for (int i = 1; i < this.values.length; i++) {
            builder.append(",");
            builder.append(this.values[i].toJson());
        }
        builder.append("]");
        return builder.toString();
    }

    @Override
    public String toString() {
        if (this.values.length == 0) {
            return "[]";
        }

        final StringBuilder builder = new StringBuilder();
        builder.append("[");
        builder.append(this.values[0].toString());
        for (int i = 1; i < this.values.length; i++) {
            builder.append(",");
            builder.append(this.values[i].toString());
        }
        builder.append("]");
        return builder.toString();
    }

    @Override
    public boolean equals(final Object otherObject) {
        if (otherObject == this) {
            return true;
        }

        // Fake!
        if (otherObject instanceof JsonArray) {
            final JsonArray other = (JsonArray) otherObject;
            return Arrays.equals(this.values, other.toArray(new JsonValue[other.size()]));
        }

        // Check by `instanceof` in case against unexpected arbitrary extension of JsonValue.
        if (!(otherObject instanceof FakeJsonArray)) {
            return false;
        }

        final FakeJsonArray other = (FakeJsonArray) otherObject;

        // The equality of FakeJsonArray should be checked exactly as Java arrays, unlike JsonObject.
        return Arrays.equals(this.values, other.values);
    }

    @Override
    public int hashCode() {
        int hash = 1;
        for (int i = 0; i < this.values.length; i++) {
            final JsonValue value = this.values[i];
            hash = 31 * hash + value.hashCode();
        }
        return hash;
    }

    private static final FakeJsonArray EMPTY = new FakeJsonArray(new JsonValue[0]);

    private final JsonValue[] values;
}
