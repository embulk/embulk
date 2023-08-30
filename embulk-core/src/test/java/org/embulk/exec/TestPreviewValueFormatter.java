/*
 * Copyright 2023 The Embulk project
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

package org.embulk.exec;

import static org.junit.Assert.assertEquals;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.embulk.spi.json.JsonArray;
import org.embulk.spi.json.JsonBoolean;
import org.embulk.spi.json.JsonDouble;
import org.embulk.spi.json.JsonLong;
import org.embulk.spi.json.JsonNull;
import org.embulk.spi.json.JsonObject;
import org.junit.Test;

public class TestPreviewValueFormatter {
    @Test
    public void test() {
        assertFormatter("", null);
        assertFormatter("true", Boolean.valueOf(true));
        assertFormatter("false", Boolean.valueOf(false));
        assertFormatter("0", Long.valueOf(0L));
        assertFormatter("9,223,372,036,854,775,807", Long.valueOf(Long.MAX_VALUE));
        assertFormatter("0.0", Double.valueOf(0.0));
        assertFormatter("", "");
        assertFormatter("foo", "foo");
        assertFormatter("テスト", "テスト");
        assertFormatter("2023-03-12 09:59:59.999999999 UTC",
                        ZonedDateTime.of(2023, 3, 12, 1, 59, 59, 999999999, ZoneId.of("America/Los_Angeles")).toInstant());
        assertFormatter("2023-03-12 11:00:00 UTC",
                        ZonedDateTime.of(2023, 3, 12, 4, 0, 0, 0, ZoneId.of("America/Los_Angeles")).toInstant());
        assertFormatter("[]", JsonArray.of());
        assertFormatter("{}", JsonObject.of());
        assertFormatter("[1.0]", JsonArray.of(JsonDouble.of(1.0)));
        assertFormatter("{\"foo\":12}", JsonObject.of("foo", JsonLong.of(12)));
        assertFormatter("null", JsonNull.NULL);
        assertFormatter("true", JsonBoolean.TRUE);
        assertFormatter("false", JsonBoolean.FALSE);
    }

    private static void assertFormatter(final String expected, final Object value) {
        final PreviewValueFormatter formatter = new PreviewValueFormatter();
        assertEquals(expected, formatter.valueToString(value));
    }
}
