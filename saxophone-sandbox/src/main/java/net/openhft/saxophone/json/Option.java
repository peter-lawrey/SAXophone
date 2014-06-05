/*
 * Copyright 2007-2014, Lloyd Hilaiel <me@lloyd.io> and YAJL contributors
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

public enum Option {

    /**
     * Porting note: this enum corresponds to yajl_option enum from src/api/yajl_parse.h in YAJL.
     */

    /**
     * Ignore javascript style comments present in
     * JSON input.  Non-standard, but rather fun
     * arguments: toggled off with integer zero, on otherwise.
     */
    ALLOW_COMMENTS,
    /**
     * When set the parser will verify that all strings in JSON input are
     * valid UTF8 and will emit a parse error if this is not so.  When set,
     * this option makes parsing slightly more expensive (~7% depending
     * on processor and compiler in use)
     */
    DONT_VALIDATE_STRINGS,
    /**
     * By default, upon calls to yajl_complete_parse(), yajl will
     * ensure the entire input text was consumed and will raise an error
     * otherwise.  Enabling this flag will cause yajl to disable this
     * check.  This can be useful when parsing json out of a that contains more
     * than a single JSON document.
     */
    ALLOW_TRAILING_GARBAGE,
    /**
     * Allow multiple values to be parsed by a single handle.  The
     * entire text must be valid JSON, and values can be separated
     * by any kind of whitespace.  This flag will change the
     * behavior of the parser, and cause it continue parsing after
     * a value is parsed, rather than transitioning into a
     * complete state.  This option can be useful when parsing multiple
     * values from an input stream.
     */
    ALLOW_MULTIPLE_VALUES,
    /**
     * When yajl_complete_parse() is called the parser will
     * check that the top level value was completely consumed.  I.E.,
     * if called whilst in the middle of parsing a value
     * yajl will enter an error state (premature EOF).  Setting this
     * flag suppresses that check and the corresponding error.
     */
    ALLOW_PARTIAL_VALUES
}
