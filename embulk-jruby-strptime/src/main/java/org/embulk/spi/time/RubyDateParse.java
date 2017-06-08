package org.embulk.spi.time;

import java.util.HashMap;
import java.util.Map;

public class RubyDateParse
{
    // Ported zones_source in ext/date/date_parse.c
    static final Map<String, Integer> ZONES_SOURCE;

    static {
        Map<String, Integer> map = new HashMap<>();
        map.put("ut",                     0*3600);
        map.put("gmt",                    0*3600);
        map.put("est",                   -5*3600);
        map.put("edt",                   -4*3600);
        map.put("cst",                   -6*3600);
        map.put("cdt",                   -5*3600);
        map.put("mst",                   -7*3600);
        map.put("mdt",                   -6*3600);
        map.put("pst",                   -8*3600);
        map.put("pdt",                   -7*3600);
        map.put("a",                      1*3600);
        map.put("b",                      2*3600);
        map.put("c",                      3*3600);
        map.put("d",                      4*3600);
        map.put("e",                      5*3600);
        map.put("f",                      6*3600);
        map.put("g",                      7*3600);
        map.put("h",                      8*3600);
        map.put("i",                      9*3600);
        map.put("k",                     10*3600);
        map.put("l",                     11*3600);
        map.put("m",                     12*3600);
        map.put("n",                     -1*3600);
        map.put("o",                     -2*3600);
        map.put("p",                     -3*3600);
        map.put("q",                     -4*3600);
        map.put("r",                     -5*3600);
        map.put("s",                     -6*3600);
        map.put("t",                     -7*3600);
        map.put("u",                     -8*3600);
        map.put("v",                     -9*3600);
        map.put("w",                    -10*3600);
        map.put("x",                    -11*3600);
        map.put("y",                    -12*3600);
        map.put("z",                      0*3600);
        map.put("utc",                    0*3600);
        map.put("wet",                    0*3600);
        map.put("at",                    -2*3600);
        map.put("brst",                  -2*3600);
        map.put("ndt",            -(2*3600+1800));
        map.put("art",                   -3*3600);
        map.put("adt",                   -3*3600);
        map.put("brt",                   -3*3600);
        map.put("clst",                  -3*3600);
        map.put("nst",            -(3*3600+1800));
        map.put("ast",                   -4*3600);
        map.put("clt",                   -4*3600);
        map.put("akdt",                  -8*3600);
        map.put("ydt",                   -8*3600);
        map.put("akst",                  -9*3600);
        map.put("hadt",                  -9*3600);
        map.put("hdt",                   -9*3600);
        map.put("yst",                   -9*3600);
        map.put("ahst",                 -10*3600);
        map.put("cat",                  -10*3600);
        map.put("hast",                 -10*3600);
        map.put("hst",                  -10*3600);
        map.put("nt",                   -11*3600);
        map.put("idlw",                 -12*3600);
        map.put("bst",                    1*3600);
        map.put("cet",                    1*3600);
        map.put("fwt",                    1*3600);
        map.put("met",                    1*3600);
        map.put("mewt",                   1*3600);
        map.put("mez",                    1*3600);
        map.put("swt",                    1*3600);
        map.put("wat",                    1*3600);
        map.put("west",                   1*3600);
        map.put("cest",                   2*3600);
        map.put("eet",                    2*3600);
        map.put("fst",                    2*3600);
        map.put("mest",                   2*3600);
        map.put("mesz",                   2*3600);
        map.put("sast",                   2*3600);
        map.put("sst",                    2*3600);
        map.put("bt",                     3*3600);
        map.put("eat",                    3*3600);
        map.put("eest",                   3*3600);
        map.put("msk",                    3*3600);
        map.put("msd",                    4*3600);
        map.put("zp4",                    4*3600);
        map.put("zp5",                    5*3600);
        map.put("ist",             (5*3600+1800));
        map.put("zp6",                    6*3600);
        map.put("wast",                   7*3600);
        map.put("cct",                    8*3600);
        map.put("sgt",                    8*3600);
        map.put("wadt",                   8*3600);
        map.put("jst",                    9*3600);
        map.put("kst",                    9*3600);
        map.put("east",                  10*3600);
        map.put("gst",                   10*3600);
        map.put("eadt",                  11*3600);
        map.put("idle",                  12*3600);
        map.put("nzst",                  12*3600);
        map.put("nzt",                   12*3600);
        map.put("nzdt",                  13*3600);
        map.put("afghanistan",             16200);
        map.put("alaskan",                -32400);
        map.put("arab",                    10800);
        map.put("arabian",                 14400);
        map.put("arabic",                  10800);
        map.put("atlantic",               -14400);
        map.put("aus central",             34200);
        map.put("aus eastern",             36000);
        map.put("azores",                  -3600);
        map.put("canada central",         -21600);
        map.put("cape verde",              -3600);
        map.put("caucasus",                14400);
        map.put("cen. australia",          34200);
        map.put("central america",        -21600);
        map.put("central asia",            21600);
        map.put("central europe",           3600);
        map.put("central european",         3600);
        map.put("central pacific",         39600);
        map.put("central",                -21600);
        map.put("china",                   28800);
        map.put("dateline",               -43200);
        map.put("e. africa",               10800);
        map.put("e. australia",            36000);
        map.put("e. europe",                7200);
        map.put("e. south america",       -10800);
        map.put("eastern",                -18000);
        map.put("egypt",                    7200);
        map.put("ekaterinburg",            18000);
        map.put("fiji",                    43200);
        map.put("fle",                      7200);
        map.put("greenland",              -10800);
        map.put("greenwich",                   0);
        map.put("gtb",                      7200);
        map.put("hawaiian",               -36000);
        map.put("india",                   19800);
        map.put("iran",                    12600);
        map.put("jerusalem",                7200);
        map.put("korea",                   32400);
        map.put("mexico",                 -21600);
        map.put("mid-atlantic",            -7200);
        map.put("mountain",               -25200);
        map.put("myanmar",                 23400);
        map.put("n. central asia",         21600);
        map.put("nepal",                   20700);
        map.put("new zealand",             43200);
        map.put("newfoundland",           -12600);
        map.put("north asia east",         28800);
        map.put("north asia",              25200);
        map.put("pacific sa",             -14400);
        map.put("pacific",                -28800);
        map.put("romance",                  3600);
        map.put("russian",                 10800);
        map.put("sa eastern",             -10800);
        map.put("sa pacific",             -18000);
        map.put("sa western",             -14400);
        map.put("samoa",                  -39600);
        map.put("se asia",                 25200);
        map.put("malay peninsula",         28800);
        map.put("south africa",             7200);
        map.put("sri lanka",               21600);
        map.put("taipei",                  28800);
        map.put("tasmania",                36000);
        map.put("tokyo",                   32400);
        map.put("tonga",                   46800);
        map.put("us eastern",             -18000);
        map.put("us mountain",            -25200);
        map.put("vladivostok",             36000);
        map.put("w. australia",            28800);
        map.put("w. central africa",        3600);
        map.put("w. europe",                3600);
        map.put("west asia",               18000);
        map.put("west pacific",            36000);
        map.put("yakutsk",                 32400);
        ZONES_SOURCE = new HashMap<>(map);
    }

    // Ported date_zone_to_diff in ext/date/date_parse.c
    public static int dateZoneToDiff(String zone)
    {
        String z = zone.toLowerCase();

        boolean dst;
        if (z.endsWith(" daylight time")) {
            z = z.substring(0, z.length() - " daylight time".length());
            dst = true;
        }
        else if (z.endsWith(" standard time")) {
            z = z.substring(0, z.length() - " standard time".length());
            dst = false;
        }
        else if (z.endsWith(" dst")) {
            z = z.substring(0, z.length() - " dst".length());
            dst = true;
        }
        else {
            dst = false;
        }

        if (RubyDateParse.ZONES_SOURCE.containsKey(z)) {
            int offset = RubyDateParse.ZONES_SOURCE.get(z);
            if (dst) {
                offset += 3600;
            }
            return offset;
        }

        if (z.startsWith("gmt") || z.startsWith("utc")) {
            z = z.substring(3, z.length()); // remove "gmt" or "utc"
        }

        boolean sign;
        if (z.charAt(0) == '+') {
            sign = true;
        }
        else if (z.charAt(0) == '-') {
            sign = false;
        }
        else {
            // if z doesn't start with "+" or "-", invalid
            return Integer.MIN_VALUE;
        }
        z = z.substring(1);

        int hour = 0, min = 0, sec = 0;
        if (z.contains(":")) {
            String[] splited = z.split(":");
            if (splited.length == 2) {
                hour = Integer.parseInt(splited[0]);
                min = Integer.parseInt(splited[1]);
            }
            else {
                hour = Integer.parseInt(splited[0]);
                min = Integer.parseInt(splited[1]);
                sec = Integer.parseInt(splited[2]);
            }

        }
        else if (z.contains(",") || z.contains(".")) {
            // TODO min = Rational(fr.to_i, 10**fr.size) * 60
            String[] splited = z.split("[\\.,]");
            hour = Integer.parseInt(splited[0]);
            min = (int)(Integer.parseInt(splited[1]) * 60 / Math.pow(10, splited[1].length()));

        }
        else {
            int len = z.length();
            if (len % 2 != 0) {
                if (len >= 1) {
                    hour = Integer.parseInt(z.substring(0, 1));
                }
                if (len >= 3) {
                    min = Integer.parseInt(z.substring(1, 3));
                }
                if (len >= 5) {
                    sec = Integer.parseInt(z.substring(3, 5));
                }
            }
            else {
                if (len >= 2) {
                    hour = Integer.parseInt(z.substring(0, 2));
                }
                if (len >= 4) {
                    min = Integer.parseInt(z.substring(2, 4));
                }
                if (len >= 6) {
                    sec = Integer.parseInt(z.substring(4, 6));
                }
            }
        }

        int offset = hour * 3600 + min * 60 + sec;
        return sign ? offset : -offset;
    }
}
