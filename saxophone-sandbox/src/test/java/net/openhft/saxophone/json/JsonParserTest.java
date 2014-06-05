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
import net.openhft.lang.io.ByteBufferBytes;
import net.openhft.saxophone.ParseException;
import org.junit.Test;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import static java.nio.ByteBuffer.wrap;
import static org.junit.Assert.assertEquals;

public final class JsonParserTest {

    public static final String WRONG_NESTED_ARRAYS = "[[], [[[]]";
    public static final String BEYOND_MAX_LONG = "9223372036854775808";
    public static final String BEYOND_MIN_LONG = "-9223372036854775809";

    @Test
    public void testInts() {
        test("{\"k1\": 1, \"k2\": 2}");
        test("[-1, 1, 0, -0]");
        test("[9223372036854775807, -9223372036854775808]");
    }

    @Test(expected = ParseException.class)
    public void testMaxLongOverflowSimple() {
        testSimple(BEYOND_MAX_LONG);
    }

    @Test(expected = ParseException.class)
    public void testMaxLongOverflowPull() {
        testPull(BEYOND_MAX_LONG);
    }

    @Test(expected = ParseException.class)
    public void testMinLongOverflowSimple() {
        testSimple(BEYOND_MIN_LONG);
    }

    @Test(expected = ParseException.class)
    public void testMinLongOverflowPull() {
        testPull(BEYOND_MIN_LONG);
    }

    @Test
    public void testDoubles() {
        test("{\"k1\": -1.0, \"k2\": 1.0}");
        test("[1.0, 2.0, 3.0]");
        test("[9.223372e+18, 9.223372e-18, 9.223372E+18, 9.223372E-18]");
    }

    @Test
    public void testBooleans() {
        test("{\"k1\": true, \"k2\": false}");
        test("[true, false]");
        test("true");
        test("false");
    }

    @Test
    public void testNullValue() {
        test("{\"k1\": null}");
        test("[null]");
        test("null");
    }

    @Test
    public void testStrings() {
        test("{\"k1\": \"v1\", \"\": \"v2\"}"); // empty key
        test("[\"v1\", \"v2\"]");
        test("\"v1\"");
        test("\"\""); // empty
    }

    @Test
    public void testEscape() {
        test("\" \\n \\t \\\" \\f \\r \\/ \\\\ \\b \"");
    }

    @Test
    public void testSurrogates() {
        test("{\"k1\":\"\\uD83D\\uDE03\"}");
    }

    @Test
    public void testNestedObjects() {
        test("{\"k1\": {\"k2\": {}}}");
    }

    @Test
    public void testNestedArrays() {
        test("[[], [[]]]");
    }

    @Test(expected = ParseException.class)
    public void testWrongNestedArraysSimple() {
        testSimple(WRONG_NESTED_ARRAYS);
    }

    @Test(expected = ParseException.class)
    public void testWrongNestedArraysPull() {
        testPull(WRONG_NESTED_ARRAYS);
    }

    private void test(String json) {
        testSimple(json);
        testPull(json);
    }
    
    private void testSimple(String json) {
        StringWriter stringWriter = new StringWriter();
        JsonParser p = JsonParser.builder().applyAdapter(new WriterAdapter(stringWriter)).build();
        try {
            p.parse(new ByteBufferBytes(wrap(json.getBytes("UTF-8"))));
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
        p.close();
        com.google.gson.JsonParser referenceParser = new com.google.gson.JsonParser();
        JsonElement o1 = referenceParser.parse(json);
        JsonElement o2 = referenceParser.parse(stringWriter.toString());
        assertEquals(o1, o2);
    }

    private void testPull(String json) {
        StringWriter stringWriter = new StringWriter();
        JsonParser p = JsonParser.builder().applyAdapter(new WriterAdapter(stringWriter)).build();
        try {
            ByteBufferBytes jsonBytes = new ByteBufferBytes(wrap(json.getBytes("UTF-8")));
            for (long i = 0; i < jsonBytes.capacity(); i++) {
                p.parse(jsonBytes.bytes(i, 1));
            }
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
        p.close();
        com.google.gson.JsonParser referenceParser = new com.google.gson.JsonParser();
        JsonElement o1 = referenceParser.parse(json);
        JsonElement o2 = referenceParser.parse(stringWriter.toString());
        assertEquals(o1, o2);
    }
}
