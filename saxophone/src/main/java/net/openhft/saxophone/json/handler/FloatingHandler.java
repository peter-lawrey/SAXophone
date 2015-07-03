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

import java.io.IOException;

/**
 * Triggered on JSON array, object or standalone (top-level) floating number value: for example,
 * {@code `3.14`} or {@code `9.223372E-18`}.
 *
 * <p>Use this handler, only if 1) exact precision doesn't matter and the JSON input doesn't contain
 * values out of Java's {@code double} representable range, and 2) you don't need to convert
 * the values back to string form. Otherwise, use {@link NumberHandler} and parse integer and
 * floating values on your side, if needed. {@link net.openhft.saxophone.json.JsonParser} can't
 * have a {@code NumberHandler} and {@code FloatingHandler} simultaneously.
 *
 * @see net.openhft.saxophone.json.JsonParserBuilder#floatingHandler(FloatingHandler)
 */
public interface FloatingHandler extends JsonHandlerBase {
    /**
     * Handles a JSON array, object or standalone (top-level) floating number value: for example,
     * {@code `3.14`} or {@code `9.223372E-18`}.
     *
     * @param value the floating value as {@code double},
     *              or the nearest value representable as {@code double}
     * @return {@code true} if the parsing should be continued, {@code false} if it should be
     *         stopped immediately
     * @throws IOException  if an error occurred during handling
     */
    boolean onFloating(double value) throws IOException;
}
