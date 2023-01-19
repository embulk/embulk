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

public final class FakeJsonBoolean implements JsonValue {
    private FakeJsonBoolean(final boolean value) {
        this.value = value;
    }

    public static final FakeJsonBoolean FAKE_FALSE = new FakeJsonBoolean(false);

    public static final FakeJsonBoolean FAKE_TRUE = new FakeJsonBoolean(true);

    @Override
    public EntityType getEntityType() {
        return EntityType.BOOLEAN;
    }

    public boolean booleanValue() {
        return this.value;
    }

    @Override
    public String toJson() {
        if (this.value) {
            return "true";
        }
        return "false";
    }

    @Override
    public String toString() {
        if (this.value) {
            return "true";
        }
        return "false";
    }

    @Override
    public boolean equals(final Object otherObject) {
        if (otherObject == this) {
            return true;
        }
        if (!(otherObject instanceof JsonValue)) {
            return false;
        }

        final JsonValue other = (JsonValue) otherObject;
        if (!other.isJsonBoolean()) {
            return false;
        }
        return value == other.asJsonBoolean().booleanValue();
    }

    @Override
    public int hashCode() {
        if (this.value) {
            return 1231;
        }
        return 1237;
    }

    private final boolean value;
}
