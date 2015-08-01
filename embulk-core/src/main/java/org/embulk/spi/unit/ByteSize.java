package org.embulk.spi.unit;

import java.util.Objects;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.common.base.Preconditions;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class ByteSize
        implements Comparable<ByteSize>
{
    private static final Pattern PATTERN = Pattern.compile("\\A(\\d+(?:\\.\\d+)?)\\s?([a-zA-Z]*)\\z");

    private final long bytes;
    private final Unit displayUnit;

    public ByteSize(double size, Unit unit)
    {
        Preconditions.checkArgument(!Double.isInfinite(size), "size is infinite");
        Preconditions.checkArgument(!Double.isNaN(size), "size is not a number");
        Preconditions.checkArgument(size >= 0, "size is negative");
        Preconditions.checkNotNull(unit, "unit is null");
        Preconditions.checkArgument(size * unit.getFactor() <= (double) Long.MAX_VALUE, "size is large than (2^63)-1 in bytes");
        this.bytes = (long) (size * unit.getFactor());
        this.displayUnit = unit;
    }

    @JsonCreator
    public ByteSize(long bytes)
    {
        Preconditions.checkArgument(bytes >= 0, "size is negative");
        this.bytes = bytes;
        this.displayUnit = Unit.BYTES;
    }

    public long getBytes()
    {
        return bytes;
    }

    public int getBytesInt()
    {
        if (bytes > Integer.MAX_VALUE) {
            throw new RuntimeException("Byte size is too large (must be smaller than 2GB)");
        }
        return (int) bytes;
    }

    public long roundTo(Unit unit)
    {
        return (long) Math.floor(getValue(unit) + 0.5);
    }

    public double getValue(Unit unit)
    {
        return bytes / (double) unit.getFactor();
    }

    @JsonCreator
    public static ByteSize parseByteSize(String size)
    {
        Preconditions.checkNotNull(size, "size is null");
        Preconditions.checkArgument(!size.isEmpty(), "size is empty");

        Matcher matcher = PATTERN.matcher(size);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid byte size string '" + size + "'");
        }

        double value = Double.parseDouble(matcher.group(1));  // NumberFormatException extends IllegalArgumentException.

        String unitString = matcher.group(2);
        if (unitString.isEmpty()) {
            return new ByteSize(value, Unit.BYTES);
        } else {
            String upperUnitString = unitString.toUpperCase(Locale.ENGLISH);
            for (Unit unit : Unit.values()) {
                if (unit.getUnitString().toUpperCase(Locale.ENGLISH).equals(upperUnitString)) {
                    return new ByteSize(value, unit);
                }
            }
        }

        throw new IllegalArgumentException("Unknown unit '" + unitString + "'");
    }

    @JsonValue
    @Override
    public String toString()
    {
        double value = getValue(displayUnit);
        String integer = String.format(Locale.ENGLISH, "%d", (long) value);
        String decimal = String.format(Locale.ENGLISH, "%.2f", value);
        if (decimal.equals(integer + ".00")) {
            return integer + displayUnit.getUnitString();
        } else {
            return decimal + displayUnit.getUnitString();
        }
    }

    @Override
    public int compareTo(ByteSize o)
    {
        return Long.compare(bytes, o.bytes);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ByteSize)) {
            return false;
        }
        ByteSize o = (ByteSize) obj;
        return this.bytes == o.bytes;
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(bytes);
    }

    public enum Unit
    {
        BYTES(1L, "B"),
        KB(1L << 10, "KB"),
        MB(1L << 20, "MB"),
        GB(1L << 30, "GB"),
        TB(1L << 40, "TB"),
        PT(1L << 50, "PB");

        private final long factor;
        private final String unitString;

        Unit(long factor, String unitString)
        {
            this.factor = factor;
            this.unitString = unitString;
        }

        long getFactor()
        {
            return factor;
        }

        String getUnitString()
        {
            return unitString;
        }
    }
}
