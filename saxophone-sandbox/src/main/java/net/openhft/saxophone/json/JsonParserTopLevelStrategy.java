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

/**
 * {@link JsonParser} strategies of treating of top-level JSON objects in the input and throwing
 * {@link net.openhft.saxophone.ParseException ParseExceptions}.
 *
 * @see JsonParserBuilder#topLevelStrategy(JsonParserTopLevelStrategy)
 */
public enum JsonParserTopLevelStrategy {
    /**
     * {@code Bytes} input must end exactly on the single JSON object end,
     * otherwise {@code ParseException} is thrown. Even trailing whitespaces are disallowed
     * (the contests of the remaining input are not checked indeed).
     *
     * <p>This strategy is default.
     *
     * @see JsonParserBuilder#topLevelStrategy()
     */
    ALLOW_JUST_A_SINGLE_OBJECT,
    /**
     * After parsing a single top-level JSON object
     * {@link JsonParser#parse(net.openhft.lang.io.Bytes)} returns {@code true} and the (last)
     * input {@code Bytes} might have some {@link net.openhft.lang.io.Bytes#remaining() remaining}
     * bytes.
     *
     * <p>This option is useful when you are parsing multiple JSON objects from the stream (see
     * {@link JsonParserTopLevelStrategy#ALLOW_MULTIPLE_VALUES}), but need to do some
     * per-top-level-object work.
     */
    ALLOW_TRAILING_GARBAGE,
    /**
     * Allow multiple values to be parsed by a single {@code JsonParser} between
     * {@link JsonParser#reset()} calls and since {@code JsonParser} construction. The entire text
     * must be valid JSON, and values can be separated by any kind of whitespace.
     *
     * <p>Example of the valid input for a parser with this strategy: <pre>{@code
     * {"foo": 1} // first top-level JSON object
     *
     * "bar" 42 3.14
     * []
     * }</pre>
     */
    ALLOW_MULTIPLE_VALUES,
}
