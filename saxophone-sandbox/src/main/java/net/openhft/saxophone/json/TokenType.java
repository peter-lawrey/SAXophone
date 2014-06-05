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

/**
 * Porting note: this enum corresponds to yajl_tok enum from src/yajl_lex.h in YAJL.
 */
enum TokenType {
    BOOL,
    COLON,
    COMMA,
    EOF,
    ERROR,
    LEFT_BRACE,
    LEFT_BRACKET,
    NULL,
    RIGHT_BRACE,
    RIGHT_BRACKET,

    // we differentiate between integers and doubles to allow the
    // parser to interpret the number without re-scanning
    INTEGER,
    DOUBLE,

    // we differentiate between strings which require further processing
    // and strings that do not
    STRING,
    STRING_WITH_ESCAPES,

    // comment tokens are not currently returned to the parser, ever
    COMMENT
}
