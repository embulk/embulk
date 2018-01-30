package org.embulk.spi.time;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * RubyTimeFormat represents a Ruby-compatible time format.
 *
 * Embulk's timestamp formats are based on Ruby's formats for historical reasons, and kept for compatibility.
 * Embulk maintains its own implementation of Ruby-compatible time parser to be independent from JRuby.
 *
 * This class is intentionally package-private so that plugins do not directly depend.
 */
class RubyTimeFormat implements Iterable<RubyTimeFormat.TokenWithNext> {
    private RubyTimeFormat(final List<RubyTimeFormatToken> compiledPattern) {
        this.compiledPattern = Collections.unmodifiableList(compiledPattern);
    }

    public static RubyTimeFormat compileForFormatter(final String formatString) {
        return new RubyTimeFormat(ContextualCompilerForFormatter.compile(formatString));
    }

    public static RubyTimeFormat compile(final String formatString) {
        return new RubyTimeFormat(ContextualCompilerForParser.compile(formatString));
    }

    static RubyTimeFormat createForTesting(final List<RubyTimeFormatToken> compiledPattern) {
        return new RubyTimeFormat(compiledPattern);
    }

    @Override
    public boolean equals(final Object otherObject) {
        if (!(otherObject instanceof RubyTimeFormat)) {
            return false;
        }
        final RubyTimeFormat other = (RubyTimeFormat) otherObject;
        return this.compiledPattern.equals(other.compiledPattern);
    }

    @Override
    public Iterator<TokenWithNext> iterator() {
        return new TokenIterator(this.compiledPattern.iterator());
    }

    static class TokenWithNext {
        private TokenWithNext(final RubyTimeFormatToken token, final RubyTimeFormatToken nextToken) {
            this.token = token;
            this.nextToken = nextToken;
        }

        RubyTimeFormatToken getToken() {
            return this.token;
        }

        RubyTimeFormatToken getNextToken() {
            return this.nextToken;
        }

        private final RubyTimeFormatToken token;
        private final RubyTimeFormatToken nextToken;
    }

    private abstract static class AbstractContextualCompiler {
        AbstractContextualCompiler(final String formatString) {
            this.formatString = formatString;
        }

        final List<RubyTimeFormatToken> compileInitial() {
            this.index = 0;
            this.resultTokens = new ArrayList<>();
            this.rawStringBuffer = new StringBuilder();

            while (this.index < this.formatString.length()) {
                final char cur = this.formatString.charAt(this.index);
                switch (cur) {
                    case '%':
                        if (this.rawStringBuffer.length() > 0) {
                            this.resultTokens.add(new RubyTimeFormatToken.Immediate(this.rawStringBuffer.toString()));
                        }
                        this.rawStringBuffer = new StringBuilder();
                        this.index++;
                        if (!this.compileDirective(this.index)) {
                            this.rawStringBuffer.append(cur);  // Add '%', and go next ordinarily.
                        }
                        break;
                    default:
                        this.rawStringBuffer.append(cur);
                        this.index++;
                }
            }
            if (this.rawStringBuffer.length() > 0) {
                this.resultTokens.add(new RubyTimeFormatToken.Immediate(this.rawStringBuffer.toString()));
            }

            return Collections.unmodifiableList(this.resultTokens);
        }

        abstract boolean compileDirective(final int beginningIndex);

        final String formatString;

        int index;
        ArrayList<RubyTimeFormatToken> resultTokens;
        StringBuilder rawStringBuffer;
    }

    private static class ContextualCompilerForFormatter extends AbstractContextualCompiler {
        private ContextualCompilerForFormatter(final String formatString) {
            super(formatString);
        }

        public static List<RubyTimeFormatToken> compile(final String formatString) {
            return new ContextualCompilerForFormatter(formatString).compileInitial();
        }

        @Override
        boolean compileDirective(final int beginningIndex) {

            if (beginningIndex >= this.formatString.length()) {
                return false;
            }
            final char cur = this.formatString.charAt(beginningIndex);
            switch (cur) {
                case 'E':
                    if (beginningIndex + 1 < this.formatString.length()
                            && "cCxXyY".indexOf(this.formatString.charAt(beginningIndex + 1)) >= 0) {
                        return this.compileDirective(beginningIndex + 1);
                    } else {
                        return false;
                    }
                case 'O':
                    if (beginningIndex + 1 < this.formatString.length()
                            && "deHkIlmMSuUVwWy".indexOf(this.formatString.charAt(beginningIndex + 1)) >= 0) {
                        return this.compileDirective(beginningIndex + 1);
                    } else {
                        return false;
                    }
                case ':':
                    for (int i = 1; i <= 3; ++i) {
                        if (beginningIndex + i >= this.formatString.length()) {
                            return false;
                        }
                        if (this.formatString.charAt(beginningIndex + i) == 'z') {
                            return this.compileDirective(beginningIndex + i);
                        }
                        if (this.formatString.charAt(beginningIndex + i) != ':') {
                            return false;
                        }
                    }
                    return false;
                case '%':
                    this.resultTokens.add(new RubyTimeFormatToken.Immediate("%"));
                    this.index = beginningIndex + 1;
                    return true;
                default:
                    this.directiveIndex = beginningIndex + 1;
                    final RubyTimeFormatToken.Flags flags = compileFlags();
                    final int precision = compilePrecision();
                    final char directiveChar = this.formatString.charAt(this.directiveIndex);

                    if (RubyTimeFormatDirective.isSpecifier(directiveChar)) {
                        if (this.directiveIndex == beginningIndex + 1) {
                            // Simple Directive.
                            this.resultTokens.addAll(RubyTimeFormatDirective.of(directiveChar).toTokens());
                        } else {
                            // Complex Directive.
                        }
                        this.index = this.directiveIndex;
                        return true;
                    } else {
                        return false;
                    }
            }
        }

        private RubyTimeFormatToken.Flags compileFlags() {
            final RubyTimeFormatToken.Flags.Builder builder = RubyTimeFormatToken.Flags.builder();

            for (; this.directiveIndex < this.formatString.length(); ++this.directiveIndex) {
                final char c = this.formatString.charAt(this.directiveIndex);
                if (c == '_') {
                    builder.setPadding(' ');
                } else if (c == '-') {
                    builder.setLeft(true);
                } else if (c == '^') {
                    builder.setUpper(true);
                } else if (c == '#') {
                    builder.setChCase(true);
                } else if (c == '0') {
                    builder.setPadding('0');
                } else {
                    break;
                }
            }

            return builder.build();
        }

        private int compilePrecision() {
            int precision = 0;
            for (; this.directiveIndex < this.formatString.length(); ++this.directiveIndex) {
                final char c = this.formatString.charAt(this.directiveIndex);
                // TODO: Check with Integer.MAX_VALUE.
                if ('0' <= c && c <= '9') {
                    precision = precision * 10 + Character.digit(c, 10);
                } else {
                    break;
                }
            }
            return precision;
        }

        private int directiveIndex;
    }

    private static class ContextualCompilerForParser extends AbstractContextualCompiler {
        private ContextualCompilerForParser(final String formatString) {
            super(formatString);
        }

        public static List<RubyTimeFormatToken> compile(final String formatString) {
            return new ContextualCompilerForParser(formatString).compileInitial();
        }

        @Override
        boolean compileDirective(final int beginningIndex) {
            if (beginningIndex >= this.formatString.length()) {
                return false;
            }
            final char cur = this.formatString.charAt(beginningIndex);
            switch (cur) {
                case 'E':
                    if (beginningIndex + 1 < this.formatString.length()
                            && "cCxXyY".indexOf(this.formatString.charAt(beginningIndex + 1)) >= 0) {
                        return this.compileDirective(beginningIndex + 1);
                    } else {
                        return false;
                    }
                case 'O':
                    if (beginningIndex + 1 < this.formatString.length()
                            && "deHImMSuUVwWy".indexOf(this.formatString.charAt(beginningIndex + 1)) >= 0) {
                        return this.compileDirective(beginningIndex + 1);
                    } else {
                        return false;
                    }
                case ':':
                    for (int i = 1; i <= 3; ++i) {
                        if (beginningIndex + i >= this.formatString.length()) {
                            return false;
                        }
                        if (this.formatString.charAt(beginningIndex + i) == 'z') {
                            return this.compileDirective(beginningIndex + i);
                        }
                        if (this.formatString.charAt(beginningIndex + i) != ':') {
                            return false;
                        }
                    }
                    return false;
                case '%':
                    this.resultTokens.add(new RubyTimeFormatToken.Immediate("%"));
                    this.index = beginningIndex + 1;
                    return true;
                default:
                    if (RubyTimeFormatDirective.isSpecifier(cur)) {
                        this.resultTokens.addAll(RubyTimeFormatDirective.of(cur).toTokens());
                        this.index = beginningIndex + 1;
                        return true;
                    } else {
                        return false;
                    }
            }
        }
    }

    private static class TokenIterator implements Iterator<TokenWithNext> {
        private TokenIterator(final Iterator<RubyTimeFormatToken> initialIterator) {
            this.internalIterator = initialIterator;
            if (initialIterator.hasNext()) {
                this.next = initialIterator.next();
            } else {
                this.next = null;
            }
        }

        @Override
        public boolean hasNext() {
            return this.next != null;
        }

        @Override
        public TokenWithNext next() {
            final TokenWithNext tokenWithNext;
            if (this.internalIterator.hasNext()) {
                tokenWithNext = new TokenWithNext(this.next, this.internalIterator.next());
            } else {
                tokenWithNext = new TokenWithNext(this.next, null);
            }
            this.next = tokenWithNext.getNextToken();
            return tokenWithNext;
        }

        private final Iterator<RubyTimeFormatToken> internalIterator;
        private RubyTimeFormatToken next;
    }

    private final List<RubyTimeFormatToken> compiledPattern;
}
