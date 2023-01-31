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
import org.msgpack.value.Value;
import org.msgpack.value.impl.ImmutableDoubleValueImpl;

public final class FakeJsonDouble implements JsonValue {
    private FakeJsonDouble(final double value) {
        this.value = new ImmutableDoubleValueImpl(value);
    }

    public static FakeJsonDouble of(final double value) {
        return new FakeJsonDouble(value);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DOUBLE;
    }

    @Override
    public int presumeReferenceSizeInBytes() {
        return 8;
    }

    public boolean isIntegral() {
        final double inner = this.value.toDouble();
        return inner == Math.rint(inner);
    }

    public boolean isByteValue() {
        return this.isIntegral() && ((double) Byte.MIN_VALUE) <= this.value.toDouble() && this.value.toDouble() <= ((double) Byte.MAX_VALUE);
    }

    public boolean isShortValue() {
        return this.isIntegral() && ((double) Short.MIN_VALUE) <= this.value.toDouble() && this.value.toDouble() <= ((double) Short.MAX_VALUE);
    }

    public boolean isIntValue() {
        return this.isIntegral() && ((double) Integer.MIN_VALUE) <= this.value.toDouble() && this.value.toDouble() <= ((double) Integer.MAX_VALUE);
    }

    public boolean isLongValue() {
        return this.isIntegral() && ((double) Long.MIN_VALUE) <= this.value.toDouble() && this.value.toDouble() <= ((double) Long.MAX_VALUE);
    }

    public byte byteValue() {
        return this.value.toByte();
    }

    public byte byteValueExact() {
        if (!this.isByteValue()) {
            throw new ArithmeticException("Out of the range of byte, or not integral: " + this.value);
        }
        return this.value.toByte();
    }

    public short shortValue() {
        return this.value.toShort();
    }

    public short shortValueExact() {
        if (!this.isShortValue()) {
            throw new ArithmeticException("Out of the range of short, or not integral: " + this.value);
        }
        return this.value.toShort();
    }

    public int intValue() {
        return this.value.toInt();
    }

    public int intValueExact() {
        if (!this.isIntValue()) {
            throw new ArithmeticException("Out of the range of int, or not integral: " + this.value);
        }
        return this.value.toInt();
    }

    public long longValue() {
        return this.value.toLong();
    }

    public long longValueExact() {
        if (!this.isLongValue()) {
            throw new ArithmeticException("Out of the range of long, or not integral: " + this.value);
        }
        return this.value.toLong();
    }

    public BigInteger bigIntegerValue() {
        return BigDecimal.valueOf(this.value.toDouble()).toBigInteger();
    }

    public BigInteger bigIntegerValueExact() {
        return BigDecimal.valueOf(this.value.toDouble()).toBigIntegerExact();
    }

    public float floatValue() {
        return this.value.toFloat();
    }

    public double doubleValue() {
        return this.value.toDouble();
    }

    public BigDecimal bigDecimalValue() {
        return BigDecimal.valueOf(this.value.toDouble());
    }

    @Override
    public String toJson() {
        return Double.toString(this.value.toDouble());
    }

    @Deprecated
    public Value toMsgpack() {
        return this.value;
    }

    @Override
    public String toString() {
        return Double.toString(this.value.toDouble());
    }

    @Override
    public boolean equals(final Object otherObject) {
        if (otherObject == this) {
            return true;
        }

        // Check by `instanceof` in case against unexpected arbitrary extension of JsonValue.
        if (otherObject instanceof FakeJsonDouble) {
            final FakeJsonDouble other = (FakeJsonDouble) otherObject;
            return this.value.equals(other.value);
        }

        // Fake!
        if (otherObject instanceof JsonDouble) {
            final JsonDouble other = (JsonDouble) otherObject;
            return this.doubleValue() == other.doubleValue();
        }

        if (otherObject instanceof JsonLong) {
            final JsonLong other = (JsonLong) otherObject;
            return this.isLongValue() && this.value.toLong() == other.longValue();
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

    private final ImmutableDoubleValueImpl value;
}
