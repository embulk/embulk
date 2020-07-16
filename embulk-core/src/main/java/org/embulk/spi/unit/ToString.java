package org.embulk.spi.unit;

@Deprecated
public final class ToString {
    private final String string;

    public ToString(String string) {
        this.string = string;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ToString)) {
            return false;
        }
        ToString o = (ToString) obj;
        return string.equals(o.string);
    }

    @Override
    public int hashCode() {
        return string.hashCode();
    }

    @Override
    public String toString() {
        return string;
    }
}
