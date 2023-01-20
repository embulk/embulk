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

public final class FakeJsonDouble implements JsonValue {
    private FakeJsonDouble(final double value) {
        this.value = value;
    }

    public static FakeJsonDouble of(final double value) {
        return new FakeJsonDouble(value);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DOUBLE;
    }

    public boolean isIntegral() {
        return !Double.isNaN(this.value) && !Double.isInfinite(this.value) && this.value == Math.rint(this.value);
    }

    public boolean isByteValue() {
        return this.isIntegral() && ((double) Byte.MIN_VALUE) <= this.value && this.value <= ((double) Byte.MAX_VALUE);
    }

    public boolean isShortValue() {
        return this.isIntegral() && ((double) Short.MIN_VALUE) <= this.value && this.value <= ((double) Short.MAX_VALUE);
    }

    public boolean isIntValue() {
        return this.isIntegral() && ((double) Integer.MIN_VALUE) <= this.value && this.value <= ((double) Integer.MAX_VALUE);
    }

    public boolean isLongValue() {
        return this.isIntegral() && ((double) Long.MIN_VALUE) <= this.value && this.value <= ((double) Long.MAX_VALUE);
    }

    public byte byteValue() {
        return (byte) this.value;
    }

    public byte byteValueExact() {
        if (!this.isIntegral()) {
            throw new ArithmeticException("Not an integer: " + this.value);
        }
        if (((double) Byte.MIN_VALUE) <= this.value && this.value <= ((double) Byte.MAX_VALUE)) {
            throw new ArithmeticException("Out of the range of byte: " + this.value);
        }
        return (byte) this.value;
    }

    public short shortValue() {
        return (short) this.value;
    }

    public short shortValueExact() {
        if (!this.isIntegral()) {
            throw new ArithmeticException("Not an integer: " + this.value);
        }
        if (((double) Short.MIN_VALUE) <= this.value && this.value <= ((double) Short.MAX_VALUE)) {
            throw new ArithmeticException("Out of the range of short: " + this.value);
        }
        return (short) this.value;
    }

    public int intValue() {
        return (int) this.value;
    }

    public int intValueExact() {
        if (!this.isIntegral()) {
            throw new ArithmeticException("Not an integer: " + this.value);
        }
        if (((double) Integer.MIN_VALUE) <= this.value && this.value <= ((double) Integer.MAX_VALUE)) {
            throw new ArithmeticException("Out of the range of int: " + this.value);
        }
        return (int) this.value;
    }

    public long longValue() {
        return (long) this.value;
    }

    public long longValueExact() {
        if (!this.isIntegral()) {
            throw new ArithmeticException("Not an integer: " + this.value);
        }
        if (((double) Long.MIN_VALUE) <= this.value && this.value <= ((double) Long.MAX_VALUE)) {
            throw new ArithmeticException("Out of the range of long: " + this.value);
        }
        return (long) this.value;
    }

    public BigInteger bigIntegerValue() {
        return BigDecimal.valueOf(this.value).toBigInteger();
    }

    public BigInteger bigIntegerValueExact() {
        return BigDecimal.valueOf(this.value).toBigIntegerExact();
    }

    public float floatValue() {
        return (float) this.value;
    }

    public double doubleValue() {
        return this.value;
    }

    public BigDecimal bigDecimalValue() {
        return BigDecimal.valueOf(this.value);
    }

    @Override
    public String toJson() {
        if (Double.isNaN(this.value) || Double.isInfinite(this.value)) {
            return "null";
        }
        return Double.toString(this.value);
    }

    @Override
    public String toString() {
        return Double.toString(this.value);
    }

    @Override
    public boolean equals(final Object otherObject) {
        if (otherObject == this) {
            return true;
        }

        // Fake!
        if (otherObject instanceof JsonDouble) {
            final JsonDouble other = (JsonDouble) otherObject;
            return this.value == other.doubleValue();
        }

        // Check by `instanceof` in case against unexpected arbitrary extension of JsonValue.
        if (!(otherObject instanceof FakeJsonDouble)) {
            return false;
        }

        final FakeJsonDouble other = (FakeJsonDouble) otherObject;

        return this.value == other.value;
    }

    @Override
    public int hashCode() {
        final long bits = Double.doubleToLongBits(this.value);
        return (int) (bits ^ (bits >>> 32));
    }

    private final double value;
}
