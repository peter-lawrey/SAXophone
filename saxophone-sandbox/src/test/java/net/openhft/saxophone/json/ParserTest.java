/*
 * Copyright 2014 Higher Frequency Trading
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.saxophone.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.openhft.lang.io.ByteBufferBytes;
import org.junit.Assert;
import org.junit.Test;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;

public final class ParserTest {

    @Test
    public void testInts() throws UnsupportedEncodingException, ParseException {
        test("{\"k1\": 1, \"k2\": 2}");
        test("[-1, 1, 0, -0]");
        test("[9223372036854775807, -9223372036854775808]");
    }

    @Test(expected = ParseException.class)
    public void testMaxLongOverflow() throws UnsupportedEncodingException, ParseException {
        test("9223372036854775808");
    }

    @Test(expected = ParseException.class)
    public void testMinLongOverflow() throws UnsupportedEncodingException, ParseException {
        test("-9223372036854775809");
    }

    @Test
    public void testDoubles() throws UnsupportedEncodingException, ParseException {
        test("{\"k1\": -1.0, \"k2\": 1.0}");
        test("[1.0, 2.0, 3.0]");
        test("[9.223372e+18, 9.223372e-18, 9.223372E+18, 9.223372E-18]");
    }

    @Test
    public void testBooleans() throws UnsupportedEncodingException, ParseException {
        test("{\"k1\": true, \"k2\": false}");
        test("[true, false]");
        test("true");
        test("false");
    }

    @Test
    public void testNullValue() throws UnsupportedEncodingException, ParseException {
        test("{\"k1\": null}");
        test("[null]");
        test("null");
    }

    @Test
    public void testStrings() throws UnsupportedEncodingException, ParseException {
        test("{\"k1\": \"v1\", \"\": \"v2\"}"); // empty key
        test("[\"v1\", \"v2\"]");
        test("\"v1\"");
        test("\"\""); // empty
    }

    @Test
    public void testEscape() throws UnsupportedEncodingException, ParseException {
        test("\" \\n \\t \\\" \\f \\r \\/ \\\\ \\b \"");
    }

    @Test
    public void testSurrogates() throws UnsupportedEncodingException, ParseException {
        test("{\"k1\":\"\\uD83D\\uDE03\"}");
    }

    @Test
    public void testNestedObjects() throws UnsupportedEncodingException, ParseException {
        test("{\"k1\": {\"k2\": {}}}");
    }

    @Test
    public void testNestedArrays() throws UnsupportedEncodingException, ParseException {
        test("[[], [[]]]");
    }

    @Test(expected = ParseException.class)
    public void testWrongNestedArrays() throws UnsupportedEncodingException, ParseException {
        test("[[], [[[]]");
    }

    private void test(String json) throws UnsupportedEncodingException, ParseException {
        StringWriter stringWriter = new StringWriter();
        Parser p = new ParserBuilder().applyAdapter(new WriterAdapter(stringWriter)).build();
        p.parse(new ByteBufferBytes(ByteBuffer.wrap(json.getBytes("UTF-8"))));
        p.close();
        JsonParser parser = new JsonParser();
        JsonElement o1 = parser.parse(json);
        JsonElement o2 = parser.parse(stringWriter.toString());
        assertEquals(o1, o2);
    }
}
