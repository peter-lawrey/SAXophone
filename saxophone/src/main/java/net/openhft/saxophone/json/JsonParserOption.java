/*
 * Copyright 2014 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.saxophone.json;

/**
 * Options passed to {@code JsonParserBuilder} on {@link JsonParser} construction specify what
 * input is considered as "valid" JSON, i. e. don't cause
 * {@link net.openhft.saxophone.ParseException}.
 *
 * @see JsonParserBuilder#options(JsonParserOption, JsonParserOption...)
 */
public enum JsonParserOption {

    /**
     * Porting note: this enum corresponds to yajl_option enum from src/api/yajl_parse.h in YAJL.
     */

    /**
     * Ignore JavaScript-style comments present in JSON input. Non-standard, but rather fun.
     *
     * <p>Examples: <pre>{@code
     * {
     *     "foo": 1, // comment till the end of line
     *     "bar": /&#42; inline comment &#42;/ 2
     * }
     * }</pre>
     */
    ALLOW_COMMENTS,
    /**
     * When this option is set, the {@code JsonParser} will verify that all strings in JSON input
     * are valid UTF8 and will emit a {@code ParseException} if this is not so. When set,
     * this option makes parsing slightly more expensive (~7% depending on processor and compiler
     * in use).
     */
    DONT_VALIDATE_STRINGS,
    /**
     * When {@link JsonParser#parse(net.openhft.lang.io.Bytes)} is called the parser will
     * check that the top level value was completely consumed. I. e., if called whilst in the middle
     * of parsing a value yajl will throw a {@code ParseException}. Setting this
     * option suppresses that check and the corresponding exception.
     */
    ALLOW_PARTIAL_VALUES
}
