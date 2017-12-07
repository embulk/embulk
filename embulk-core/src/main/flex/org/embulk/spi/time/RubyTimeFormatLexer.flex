/*
 * The following license block applies to this source code with some modification as described in the Javadoc.
 */
/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2002, 2009 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.embulk.spi.time;

/**
 * RubyTimeFormatLexer is a Ruby-compatible time format lexer.
 *
 * Embulk's timestamp formats are based on Ruby's formats for historical reasons, and kept for compatibility.
 * Embulk maintains its own implementation of Ruby-compatible time parser to be independent from JRuby.
 *
 * This class is intentionally package-private so that plugins do not directly depend.
 *
 * IMPORTANT: It must be compiled with JFlex 1.4. JFlex 1.4.3 seems buggy with look-ahead.
 *
 * This class is contributed to the JRuby project before it is refactored on the Embulk side.
 *
 * @see <a href="https://github.com/jruby/jruby/pull/4635">Implement RubyDateParser in Java by muga - Pull Request #4635 - jruby/jruby</a>
 *
 * This class is imported from JRuby 9.1.5.0's core/src/main/java/org/jruby/lexer/StrftimeLexer.flex with modification.
 * Eclipse Public License version 1.0 is applied for the import. See its COPYING for license.
 *
 * @see <a href="https://github.com/jruby/jruby/blob/9.1.5.0/core/src/main/java/org/jruby/lexer/StrftimeLexer.flex">core/src/main/java/org/jruby/lexer/StrftimeLexer.flex</a>
 * @see <a href="https://github.com/jruby/jruby/blob/9.1.5.0/COPYING">COPYING</a>
 */
%%
%class RubyTimeFormatLexer
//%debug
%unicode
%type org.embulk.spi.time.RubyTimeFormat
%{
  private StringBuilder buffer = new StringBuilder();

  private RubyTimeFormat emitRawStringToken() {
    final String string = buffer.toString();
    buffer.setLength(0);
    return RubyTimeFormat.create(new RubyTimeFormatToken.Immediate(string));
  }

  private RubyTimeFormat emitFormatDirective(final char specifier) {
    return RubyTimeFormat.create(RubyTimeFormatDirective.of(specifier).toTokens());
  }
%}

Flags = [-_0#\^]+
Width = [1-9][0-9]*

// See RubyDateFormatter.main to generate this
// Chars are sorted by | ruby -e 'p STDIN.each_char.sort{|a,b|a.casecmp(b).tap{|c|break a<=>b if c==0}}.join'
Conversion = [\+AaBbCcDdeFGgHhIjkLlMmNnPpQRrSsTtUuVvWwXxYyZz] | {IgnoredModifier} | {Zone}
// From MRI strftime.c
IgnoredModifier = E[CcXxYy] | O[deHIMmSUuVWwy]
Zone = :{1,3} z

SimpleDirective = "%"
LiteralPercent = "%%"
Unknown = .|\n

%xstate CONVERSION

%%

<YYINITIAL> {
  {LiteralPercent}                  { return RubyTimeFormat.create(new RubyTimeFormatToken.Immediate("%")); }
  {SimpleDirective}  / {Conversion} { yybegin(CONVERSION); }
}

<CONVERSION> {Conversion}           { yybegin(YYINITIAL); return emitFormatDirective(yycharat(yylength() - 1)); }

/* fallback */
{Unknown} / [^%]                    { buffer.append(yycharat(0)); }
{Unknown}                           { buffer.append(yycharat(0)); return emitRawStringToken(); }
