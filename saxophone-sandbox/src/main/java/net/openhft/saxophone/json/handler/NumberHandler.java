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

package net.openhft.saxophone.json.handler;

/**
 * Triggered on JSON array, object or standalone (top-level) number value: for example,
 * {@code `3.14`}, {@code `9.223372E-18`}, {@code `-1`}, {@code `0`}
 * or {@code `9223372036854775807`}.
 *
 * <p>Use this handler, if the JSON input might contain integer values lesser
 * than {@code Long.MIN_VALUE} or greater than {@code Long.MAX_VALUE}, or very precise floating
 * values and your need this precision, or you don't need to parse a number to the native form,
 * for example for pretty printing JSON or sending it in text format.
 * {@link net.openhft.saxophone.json.JsonParser} can't have a {@code NumberHandler}
 * and {@code IntegerHandler} or {@code NumberHandler} and {@code FloatingHandler} simultaneously.
 *
 * @see net.openhft.saxophone.json.JsonParserBuilder#numberHandler(NumberHandler)
 */
public interface NumberHandler extends JsonHandlerBase {
    /**
     * Handles a JSON array, object or standalone (top-level) number value: for example,
     * {@code `3.14`}, {@code `9.223372E-18`}, {@code `-1`}, {@code `0`}
     * or {@code `9223372036854775807`}.
     *
     * @param number the number value as a {@code CharSequence}
     * @return {@code true} if the parsing should be continued, {@code false} if it should be
     *         stopped immediately
     * @throws Exception if an error occurred during handling
     */
    boolean onNumber(CharSequence number) throws Exception;
}
