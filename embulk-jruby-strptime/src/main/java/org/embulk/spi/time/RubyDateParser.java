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

        if (has(bag.getMDay())) {
            map.put("mday", bag.getMDay());
        }
        if (has(bag.getWDay())) {
            map.put("wday", bag.getWDay());
        }
        if (has(bag.getCWDay())) {
            map.put("cwday", bag.getCWDay());
        }
        if (has(bag.getYDay())) {
            map.put("yday", bag.getYDay());
        }
        if (has(bag.getCWeek())) {
            map.put("cweek", bag.getCWeek());
        }
        if (has(bag.getCWYear())) {
            map.put("cwyear", bag.getCWYear());
        }
        if (has(bag.getMin())) {
            map.put("min", bag.getMin());
        }
        if (has(bag.getMon())) {
            map.put("mon", bag.getMon());
        }
        if (has(bag.getHour())) {
            map.put("hour", bag.getHour());
        }
        if (has(bag.getYear())) {
            map.put("year", bag.getYear());
        }
        if (has(bag.getSec())) {
            map.put("sec", bag.getSec());
        }
        if (has(bag.getWNum0())) {
            map.put("wnum0", bag.getWNum0());
        }
        if (has(bag.getWNum1())) {
            map.put("wnum1", bag.getWNum1());
        }
        if (bag.getZone() != null) {
            map.put("zone", bag.getZone());
            int offset = RubyDateParse.dateZoneToDiff(bag.getZone());
            if (offset != Integer.MIN_VALUE) {
                map.put("offset", offset);
            }
        }
        if (has(bag.getSecFraction())) {
            map.put("sec_fraction", newRationalCanonicalize(context, bag.getSecFraction(), (long)Math.pow(10, bag.getSecFractionSize())));
        }
        if (bag.hasSeconds()) {
            if (has(bag.getSecondsSize())) {
                map.put("seconds", newRationalCanonicalize(context, bag.getSeconds(), (long) Math.pow(10, bag.getSecondsSize())));
            }
            else {
                map.put("seconds", bag.getSeconds());
            }
        }
        if (has(bag.getMerid())) {
            map.put("_merid", bag.getMerid());
        }
        if (has(bag.getCent())) {
            map.put("_cent", bag.getCent());
        }
        if (bag.getLeftover() != null) {
            map.put("leftover", bag.getLeftover());
        }

        return map;
    }
}
