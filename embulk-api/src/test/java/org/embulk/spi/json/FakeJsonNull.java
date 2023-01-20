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

public final class FakeJsonNull implements JsonValue {
    private FakeJsonNull() {
        // No direct instantiation.
    }

    public static final FakeJsonNull FAKE_NULL = new FakeJsonNull();

    public static FakeJsonNull ofFake() {
        return FAKE_NULL;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.NULL;
    }

    @Override
    public int presumeReferenceSizeInBytes() {
        return 1;
    }

    @Override
    public String toJson() {
        return "null";
    }

    @Override
    public String toString() {
        return "null";
    }

    @Override
    public boolean equals(final Object otherObject) {
        // Fake!
        if (otherObject instanceof JsonNull) {
            return true;
        }

        // Only the singleton instance is accepted. No arbitrary instantiation.
        return this == FAKE_NULL && otherObject == FAKE_NULL;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
