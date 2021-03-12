/*
 * Copyright 2020 The Embulk project
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

package org.embulk.guess.csv.timeformat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Matches a string with prepared regular expressions for time zones, and captures some pieces of strings from the given string.
 *
 * <p>It is a clone from {@code embulk-util-guess}. Its Java package has just changed.
 * It is tentatively here until {@code embulk-guess-csv} starts using {@code embulk-util-*} librarires.
 */
final class GuessZonePicker {
    private GuessZonePicker(final String zoneSpace, final String zoneOff) {
        this.zoneSpace = zoneSpace;
        this.zoneOff = zoneOff;
    }

    static GuessZonePicker from(final String text) {
        return ZONE_MATCHER.match(text);
    }

    String getZoneSpace() {
        return this.zoneSpace;
    }

    String getZoneOff() {
        return this.zoneOff;
    }

    private static class ZoneMatcher {
        ZoneMatcher(final Pattern pattern) {
            this.pattern = pattern;
        }

        GuessZonePicker match(final String text) {
            final Matcher matcher = this.pattern.matcher(text);
            if (matcher.matches()) {
                final String zoneSpace = matcher.group("zoneSpace");
                final String zoneOff = matcher.group("zoneOff");
                return new GuessZonePicker(
                        zoneSpace != null ? zoneSpace : "",
                        zoneOff != null ? zoneOff : "");
            }
            return null;
        }

        @Override
        public String toString() {
            return this.pattern.toString();
        }

        private final Pattern pattern;
    }

    private static final Pattern ZONE = Pattern.compile(String.format(
            "(?<zoneSpace> )?(?<zone>(?<zoneOff>%s)|(?<zoneAbb>%s))",
            Parts.ZONE_OFF, Parts.ZONE_ABB));

    private static final ZoneMatcher ZONE_MATCHER = new ZoneMatcher(ZONE);

    private final String zoneSpace;
    private final String zoneOff;
}
