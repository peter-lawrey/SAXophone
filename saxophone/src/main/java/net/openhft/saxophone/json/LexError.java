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
 * Porting note: this enum corresponds to yajl_lex_error enum from src/yajl_lex.h in YAJL.
 */
enum LexError {
    STRING_INVALID_UTF8,
    STRING_INVALID_ESCAPED_CHAR,
    STRING_INVALID_JSON_CHAR,
    STRING_INVALID_HEX_CHAR,
    INVALID_CHAR,
    INVALID_STRING,
    MISSING_INTEGER_AFTER_DECIMAL,
    MISSING_INTEGER_AFTER_EXPONENT,
    MISSING_INTEGER_AFTER_MINUS,
    UNALLOWED_COMMENT
}
