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
import org.msgpack.value.impl.ImmutableLongValueImpl;

public final class FakeJsonLong implements JsonValue {
    private FakeJsonLong(final long value) {
        this.value = new ImmutableLongValueImpl(value);
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
        return this.value.isInByteRange();
    }

    public boolean isShortValue() {
        return this.value.isInShortRange();
    }

    public boolean isIntValue() {
        return this.value.isInIntRange();
    }

    public boolean isLongValue() {
        return true;
    }

    public byte byteValue() {
        return this.value.toByte();
    }

    public byte byteValueExact() {
        if (!this.isByteValue()) {
            throw new ArithmeticException("Out of the range of byte: " + this.value);
        }
        return this.value.toByte();
    }

    public short shortValue() {
        return this.value.toShort();
    }

    public short shortValueExact() {
        if (!this.isShortValue()) {
            throw new ArithmeticException("Out of the range of short: " + this.value);
        }
        return this.value.toShort();
    }

    public int intValue() {
        return this.value.toInt();
    }

    public int intValueExact() {
        if (!this.isIntValue()) {
            throw new ArithmeticException("Out of the range of int: " + this.value);
        }
        return this.value.toInt();
    }

    public long longValue() {
        return this.value.toLong();
    }

    public long longValueExact() {
        return this.value.toLong();
    }

    public BigInteger bigIntegerValue() {
        return this.value.toBigInteger();
    }

    public BigInteger bigIntegerValueExact() {
        return this.value.toBigInteger();
    }

    public float floatValue() {
        return this.value.toFloat();
    }

    public double doubleValue() {
        return this.value.toDouble();
    }

    public BigDecimal bigDecimalValue() {
        return BigDecimal.valueOf(this.value.toLong());
    }

    @Override
    public String toJson() {
        return Long.toString(this.value.toLong());
    }

    @Deprecated
    public Value toMsgpack() {
        return this.value;
    }

    @Override
    public String toString() {
        return Long.toString(this.value.toLong());
    }

    @Override
    public boolean equals(final Object otherObject) {
        if (otherObject == this) {
            return true;
        }

        // Check by `instanceof` in case against unexpected arbitrary extension of JsonValue.
        if (otherObject instanceof FakeJsonLong) {
            final FakeJsonLong other = (FakeJsonLong) otherObject;
            return this.value.equals(other.value);
        }

        // Fake!
        if (otherObject instanceof JsonLong) {
            final JsonLong other = (JsonLong) otherObject;
            return this.longValue() == other.longValue();
        }

        if (otherObject instanceof JsonDouble) {
            final JsonDouble other = (JsonDouble) otherObject;
            return other.isLongValue() && this.value.toLong() == other.longValue();
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

    private final ImmutableLongValueImpl value;
}
