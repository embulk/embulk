/* IMPORTANT: must compile with JFlex 1.4, JFlex 1.4.3 seems buggy with look-ahead */

package org.embulk.spi.time.lexer;

import org.embulk.spi.time.StrftimeToken;
import org.jruby.util.RubyTimeOutputFormatter;

%%
%public
%class StrptimeLexer
//%debug
%unicode
%type org.embulk.spi.time.StrftimeToken
%{
    StringBuilder stringBuf = new StringBuilder();

    public StrftimeToken rawString() {
        String str = stringBuf.toString();
        stringBuf.setLength(0);
        return StrftimeToken.str(str);
    }

    public StrftimeToken directive(char c) {
        StrftimeToken token;
        if (c == 'z') {
            int colons = yylength()-1; // can only be colons except the 'z'
            return StrftimeToken.zoneOffsetColons(colons);
        } else if ((token = StrftimeToken.format(c)) != null) {
            return token;
        } else {
            return StrftimeToken.special(c);
        }
    }

    public StrftimeToken formatter(String str) {
        int len = str.length();
        int i = 1; // first char is '%'
        char c;
        // look for flags
        while (i < len && ((c = str.charAt(i)) < '1' || c > '9')) i++;
        String flags = str.substring(1, i);
        int width = 0;
        while (i < len) {
            width = 10 * width + (str.charAt(i) - '0');
            i++;
        }
        return StrftimeToken.formatter(new RubyTimeOutputFormatter(flags, width));
    }
%}

Flags = [-_0#\^]+
Width = [1-9][0-9]*

// See RubyDateFormatter.main to generate this
// Chars are sorted by | ruby -e 'p STDIN.each_char.sort{|a,b|a.casecmp(b).tap{|c|break a<=>b if c==0}}.join'
Conversion = [\+AaBbCcDdeFGgHhIjkLlMmNnPpQRrSsTtUuVvWwXxYyZz] | {IgnoredModifier} | {Zone}
// From MRI strftime.c
IgnoredModifier = E[CcXxYy] | O[deHkIlMmSUuVWwy]
Zone = :{1,3} z

SimpleDirective = "%"
ComplexDirective = "%" ( {Flags} {Width}? | {Width} )
LiteralPercent = "%%"
Unknown = .|\n

%xstate CONVERSION

%%

<YYINITIAL> {
  {LiteralPercent}                  { return StrftimeToken.str("%"); }
  {SimpleDirective}  / {Conversion} { yybegin(CONVERSION); }
  {ComplexDirective} / {Conversion} { yybegin(CONVERSION); return formatter(yytext()); }
}

<CONVERSION> {Conversion}           { yybegin(YYINITIAL); return directive(yycharat(yylength()-1)); }

/* fallback */
{Unknown} / [^%]                    { stringBuf.append(yycharat(0)); }
{Unknown}                           { stringBuf.append(yycharat(0)); return rawString(); }
