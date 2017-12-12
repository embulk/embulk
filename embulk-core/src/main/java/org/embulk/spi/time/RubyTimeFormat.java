package org.embulk.spi.time;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * RubyTimeFormat represents a Ruby-compatible time format.
 *
 * Embulk's timestamp formats are based on Ruby's formats for historical reasons, and kept for compatibility.
 * Embulk maintains its own implementation of Ruby-compatible time parser to be independent from JRuby.
 *
 * This class is intentionally package-private so that plugins do not directly depend.
 */
class RubyTimeFormat implements Iterable<RubyTimeFormat.TokenWithNext>
{
    private RubyTimeFormat(final List<RubyTimeFormatToken> compiledPattern)
    {
        this.compiledPattern = Collections.unmodifiableList(compiledPattern);
    }

    public static RubyTimeFormat create(final List<RubyTimeFormatToken> compiledPattern)
    {
        return new RubyTimeFormat(compiledPattern);
    }

    public static RubyTimeFormat create(RubyTimeFormatToken compiledPattern)
    {
        ArrayList<RubyTimeFormatToken> array = new ArrayList<>();
        array.add(compiledPattern);
        return new RubyTimeFormat(array);
    }

    public static RubyTimeFormat compile(final String formatString)
    {
        final Reader reader = new StringReader(formatString); // TODO Use try-with-resource statement
        final RubyTimeFormatLexer lexer = new RubyTimeFormatLexer(reader);

        final List<RubyTimeFormatToken> compiledPattern = new LinkedList<>();

        try {
            RubyTimeFormat format;
            while ((format = lexer.yylex()) != null) {
                compiledPattern.addAll(format.compiledPattern);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return new RubyTimeFormat(compiledPattern);
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
