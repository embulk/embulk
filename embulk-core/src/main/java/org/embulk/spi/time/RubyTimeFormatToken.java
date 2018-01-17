package org.embulk.spi.time;

/**
 * RubyTimeFormatToken represents a token in Ruby-compatible time format strings.
 *
 * Embulk's timestamp formats are based on Ruby's formats for historical reasons, and kept for compatibility.
 * Embulk maintains its own implementation of Ruby-compatible time parser to be independent from JRuby.
 *
 * This class is intentionally package-private so that plugins do not directly depend.
 */
abstract class RubyTimeFormatToken {
    abstract boolean isDirective();

    static class Directive extends RubyTimeFormatToken {
        Directive(final RubyTimeFormatDirective formatDirective) {
            this.formatDirective = formatDirective;
        }

        @Override
        public boolean equals(final Object otherObject) {
            if (!(otherObject instanceof Directive)) {
                return false;
            }
            final Directive other = (Directive) otherObject;
            return this.formatDirective.equals(other.formatDirective);
        }

        @Override
        public String toString() {
            return "<%" + this.formatDirective.toString() + ">";
        }

        @Override
        boolean isDirective() {
            return true;
        }

        RubyTimeFormatDirective getFormatDirective() {
            return this.formatDirective;
        }

        private final RubyTimeFormatDirective formatDirective;
    }

    static class Immediate extends RubyTimeFormatToken {
        Immediate(final char character) {
            this.string = "" + character;
        }

        Immediate(final String string) {
            this.string = string;
        }

        @Override
        public boolean equals(final Object otherObject) {
            if (!(otherObject instanceof Immediate)) {
                return false;
            }
            final Immediate other = (Immediate) otherObject;
            return this.string.equals(other.string);
        }

        @Override
        public String toString() {
            return "<\"" + this.string + "\">";
        }

        @Override
        boolean isDirective() {
            return false;
        }

        String getContent() {
            return this.string;
        }

        private final String string;
    }
}
