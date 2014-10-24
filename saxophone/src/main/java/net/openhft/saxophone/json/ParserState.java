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

import java.util.Arrays;

/**
 * Porting note: this "enum" corresponds to yajl_state enum from src/yajl_parser.h in YAJL.
 */
final class ParserState {
    static final byte
    START = 0,
    PARSE_COMPLETE = 1,
    PARSE_ERROR = 2,
    LEXICAL_ERROR = 3,
    MAP_START = 4,
    MAP_SEP = 5,
    MAP_NEED_VAL = 6,
    MAP_GOT_VAL = 7,
    MAP_NEED_KEY = 8,
    ARRAY_START = 9,
    ARRAY_GOT_VAL = 10,
    ARRAY_NEED_VAL = 11,
    GOT_VALUE = 12,
    HANDLER_CANCEL = 13,
    HANDLER_EXCEPTION = 14;

    /**
     * Porting note: this class approximately corresponds to src/yajl_bytestack.h in YAJL.
     */
    static final class Stack {
        private static final int INC = 128;
        private byte[] stack = new byte[INC];
        private int size = 0;

        byte current() {
            return stack[size - 1];
        }

        void push(byte state) {
            assert START <= state && state <= HANDLER_EXCEPTION;
            if (size == stack.length)
                stack = Arrays.copyOf(stack, size + INC);
            stack[size++] = state;
        }

        void pop() {
            size--;
        }

        void set(byte state) {
            assert START <= state && state <= HANDLER_EXCEPTION;
            stack[size - 1] = state;
        }

        void clear() {
            size = 0;
        }
    }

    private ParserState() {}
}