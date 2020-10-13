/*
 * Copyright 2014 The Embulk project
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

package org.embulk.spi;

import java.util.Objects;
import org.embulk.spi.type.BooleanType;
import org.embulk.spi.type.DoubleType;
import org.embulk.spi.type.JsonType;
import org.embulk.spi.type.LongType;
import org.embulk.spi.type.StringType;
import org.embulk.spi.type.TimestampType;
import org.embulk.spi.type.Type;

/**
 * Represents a column metadata of Embulk's data record.
 *
 * @since 0.4.0
 */
public class Column {
    /**
     * @since 0.4.0
     */
    public Column(final int index, final String name, final Type type) {
        this.index = index;
        this.name = name;
        this.type = type;
    }

    /**
     * @since 0.4.0
     */
    public int getIndex() {
        return this.index;
    }

    /**
     * @since 0.4.0
     */
    public String getName() {
        return this.name;
    }

    /**
     * @since 0.4.0
     */
    public Type getType() {
        return this.type;
    }

    /**
     * @since 0.4.0
     */
    public void visit(final ColumnVisitor visitor) {
        if (this.type instanceof BooleanType) {
            visitor.booleanColumn(this);
        } else if (this.type instanceof LongType) {
            visitor.longColumn(this);
        } else if (this.type instanceof DoubleType) {
            visitor.doubleColumn(this);
        } else if (this.type instanceof StringType) {
            visitor.stringColumn(this);
        } else if (this.type instanceof TimestampType) {
            visitor.timestampColumn(this);
        } else if (this.type instanceof JsonType) {
            visitor.jsonColumn(this);
        } else {
            throw new IllegalArgumentException("Column has an unexpected type: " + this.type);
        }
    }

    /**
     * @since 0.4.0
     */
    @Override
    public boolean equals(final Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (!(otherObject instanceof Column)) {
            return false;
        }

        final Column other = (Column) otherObject;
        return Objects.equals(this.index, other.index)
                && Objects.equals(this.name, other.name)
                && Objects.equals(this.type, other.type);
    }

    /**
     * @since 0.4.0
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.index, this.name, this.type);
    }

    /**
     * @since 0.4.0
     */
    @Override
    public String toString() {
        return String.format("Column{index:%d, name:%s, type:%s}",
                this.getIndex(), this.getName(), this.getType().getName());
    }

    private final int index;
    private final String name;
    private final Type type;
}
