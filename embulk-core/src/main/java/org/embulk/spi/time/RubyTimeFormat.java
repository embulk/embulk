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

    public static RubyTimeFormat compile(final String formatString) {
        return new RubyTimeFormat(CompilerForParser.compile(formatString));
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

    private static class CompilerForParser {
        private CompilerForParser(final String formatString) {
            this.formatString = formatString;
        }

        public static List<RubyTimeFormatToken> compile(final String formatString) {
            return new CompilerForParser(formatString).compileInitial();
        }

        private List<RubyTimeFormatToken> compileInitial() {
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

        private boolean compileDirective(final int beginningIndex) {
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

        private final String formatString;

        private int index;
        private List<RubyTimeFormatToken> resultTokens;
        private StringBuilder rawStringBuffer;
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
