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

package net.openhft.saxophone;

/**
 * Thrown when a malformed data is given to the parser, or parser configuration disallow certain
 * constructs in the given data, or if one of the event handlers throw a checked exception.
 */
public class ParseException extends RuntimeException {

    /**
     * Constructs a {@code ParseException} with the specified detail message.
     *
     * @param message the detail message (which is saved for later retrieval
     *                 by the {@link #getMessage()} method)
     */
    public ParseException(String message) {
        super(message);
    }

    /**
     * Constructs a {@code ParseException} with the specified detail message and cause.
     *
     * <p>Note that the detail message associated with {@code cause} is <i>not</i> automatically
     * incorporated in this exception's detail message.
     *
     * @param  message the detail message (which is saved for later retrieval
     *                 by the {@link #getMessage()} method)
     * @param  cause the cause (which is saved for later retrieval by the {@link #getCause()}
     *               method). (A {@code null} value is permitted, and indicates that the cause
     *               is nonexistent or unknown.)
     */
    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
