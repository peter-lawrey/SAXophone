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

package net.openhft.saxophone.json.handler;

/**
 * Triggered on JSON array, object or standalone (top-level) integer number value: for example,
 * {@code `-1`},{@code `0`} or {@code `9223372036854775807`}.
 *
 * <p>Use this handler, only if 1) you are sure integer values in the JSON are within
 * Java's {@code long} range, otherwise if you use {@code IntegerHandler}
 * {@link net.openhft.saxophone.ParseException} would be thrown,
 * and 2) you don't need to convert the values back to string form. Otherwise, use
 * {@link NumberHandler} and parse integer and floating values on your side, if needed.
 * {@link net.openhft.saxophone.json.JsonParser} can't have a {@code NumberHandler}
 * and {@code IntegerHandler} simultaneously.
 *
 * @see net.openhft.saxophone.json.JsonParserBuilder#integerHandler(IntegerHandler)
 */
public interface IntegerHandler extends JsonHandlerBase {
    /**
     * Handles a JSON array, object or standalone (top-level) integer number value: for example,
     * {@code `-1`},{@code `0`} or {@code `9223372036854775807`}.
     *
     * @param value the integer value as {@code long}
     * @return {@code true} if the parsing should be continued, {@code false} if it should be
     *         stopped immediately
     * @throws Exception if an error occurred during handling
     */
    boolean onInteger(long value) throws Exception;
}
