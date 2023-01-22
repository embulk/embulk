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

import java.math.BigDecimal;
import java.math.BigInteger;

public final class FakeJsonLong implements JsonValue {
    private FakeJsonLong(final long value) {
        this.value = value;
    }

    public static FakeJsonLong of(final long value) {
        return new FakeJsonLong(value);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.LONG;
    }

    @Override
    public int presumeReferenceSizeInBytes() {
        return 8;
    }

    public boolean isIntegral() {
        return true;
    }

    public boolean isByteValue() {
        return ((long) Byte.MIN_VALUE) <= this.value && this.value <= ((long) Byte.MAX_VALUE);
    }

    public boolean isShortValue() {
        return ((long) Short.MIN_VALUE) <= this.value && this.value <= ((long) Short.MAX_VALUE);
    }

    public boolean isIntValue() {
        return ((long) Integer.MIN_VALUE) <= this.value && this.value <= ((long) Integer.MAX_VALUE);
    }

    public boolean isLongValue() {
        return true;
    }

    public byte byteValue() {
        return (byte) this.value;
    }

    public byte byteValueExact() {
        if (!this.isByteValue()) {
            throw new ArithmeticException("Out of the range of byte: " + this.value);
        }
        return (byte) this.value;
    }

    public short shortValue() {
        return (short) this.value;
    }

    public short shortValueExact() {
        if (!this.isShortValue()) {
            throw new ArithmeticException("Out of the range of short: " + this.value);
        }
        return (short) this.value;
    }

    public int intValue() {
        return (int) this.value;
    }

    public int intValueExact() {
        if (!this.isIntValue()) {
            throw new ArithmeticException("Out of the range of int: " + this.value);
        }
        return (int) this.value;
    }

    public long longValue() {
        return (long) this.value;
    }

    public long longValueExact() {
        return (long) this.value;
    }

    public BigInteger bigIntegerValue() {
        return BigInteger.valueOf(this.value);
    }

    public BigInteger bigIntegerValueExact() {
        return BigInteger.valueOf(this.value);
    }

    public float floatValue() {
        return (float) this.value;
    }

    public double doubleValue() {
        return (double) this.value;
    }

    public BigDecimal bigDecimalValue() {
        return BigDecimal.valueOf(this.value);
    }

    @Override
    public String toJson() {
        return Long.toString(this.value);
    }

    @Override
    public String toString() {
        return Long.toString(this.value);
    }

    @Override
    public boolean equals(final Object otherObject) {
        if (otherObject == this) {
            return true;
        }

        // Fake!
        if (otherObject instanceof JsonLong) {
            final JsonLong other = (JsonLong) otherObject;
            return this.value == other.longValue();
        }

        // Check by `instanceof` in case against unexpected arbitrary extension of JsonValue.
        if (!(otherObject instanceof FakeJsonLong)) {
            return false;
        }

        final FakeJsonLong other = (FakeJsonLong) otherObject;

        return this.value == other.value;
    }

    @Override
    public int hashCode() {
        if (((long) Integer.MIN_VALUE) <= this.value && this.value <= ((long) Integer.MAX_VALUE)) {
            return (int) value;
        }
        return (int) (value ^ (value >>> 32));
    }

    private final long value;
}
