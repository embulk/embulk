package org.embulk.spi.time;

import org.jruby.runtime.ThreadContext;

import java.util.HashMap;
import java.util.List;

import static org.embulk.spi.time.StrptimeParser.FormatBag.has;
import static org.jruby.RubyRational.newRationalCanonicalize;

/**
 * This class extends {@code StrptimeParser} and provides methods that are calls from JRuby.
 */
public class RubyDateParser
        extends StrptimeParser
{
    private final ThreadContext context;

    public RubyDateParser(ThreadContext context)
    {
        super();
        this.context = context;
    }

    // Ported from Date._strptime method in lib/ruby/stdlib/date/format.rb in JRuby
    // This is Java implementation of date__strptime method in ext/date/date_strptime.c in Ruby
    public HashMap<String, Object> parse(final String format, final String text)
    {
        final List<StrptimeToken> compiledPattern = compilePattern(format);
        final FormatBag bag = parse(compiledPattern, text);
        if (bag != null) {
            return convertFormatBagToHash(bag);
        }
        else {
            return null;
        }
    }

    private HashMap<String, Object> convertFormatBagToHash(FormatBag bag)
    {
        final HashMap<String, Object> map = new HashMap<>();

        if (has(bag.mday)) {
            map.put("mday", bag.mday);
        }
        if (has(bag.wday)) {
            map.put("wday", bag.wday);
        }
        if (has(bag.cwday)) {
            map.put("cwday", bag.cwday);
        }
        if (has(bag.yday)) {
            map.put("yday", bag.yday);
        }
        if (has(bag.cweek)) {
            map.put("cweek", bag.cweek);
        }
        if (has(bag.cwyear)) {
            map.put("cwyear", bag.cwyear);
        }
        if (has(bag.min)) {
            map.put("min", bag.min);
        }
        if (has(bag.mon)) {
            map.put("mon", bag.mon);
        }
        if (has(bag.hour)) {
            map.put("hour", bag.hour);
        }
        if (has(bag.year)) {
            map.put("year", bag.year);
        }
        if (has(bag.sec)) {
            map.put("sec", bag.sec);
        }
        if (has(bag.wnum0)) {
            map.put("wnum0", bag.wnum0);
        }
        if (has(bag.wnum1)) {
            map.put("wnum1", bag.wnum1);
        }
        if (bag.zone != null) {
            map.put("zone", bag.zone);
            int offset = RubyDateParse.dateZoneToDiff(bag.zone);
            if (offset != Integer.MIN_VALUE) {
                map.put("offset", offset);
            }
        }
        if (has(bag.sec_fraction)) {
            map.put("sec_fraction", newRationalCanonicalize(context, bag.sec_fraction, (long)Math.pow(10, bag.sec_fraction_size)));
        }
        if (bag.hasSeconds()) {
            if (has(bag.seconds_size)) {
                map.put("seconds", newRationalCanonicalize(context, bag.seconds, (long) Math.pow(10, bag.seconds_size)));
            }
            else {
                map.put("seconds", bag.seconds);
            }
        }
        if (has(bag._merid)) {
            map.put("_merid", bag._merid);
        }
        if (has(bag._cent)) {
            map.put("_cent", bag._cent);
        }
        if (bag.leftover != null) {
            map.put("leftover", bag.leftover);
        }

        return map;
    }
}
